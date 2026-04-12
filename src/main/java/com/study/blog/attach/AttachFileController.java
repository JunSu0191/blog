package com.study.blog.attach;

import com.study.blog.attach.dto.AttachFileDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.storage.UploadStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.TusFileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attach-files")
public class AttachFileController {
    private static final Logger log = LoggerFactory.getLogger(AttachFileController.class);
    private static final String DEFAULT_ATTACH_DIR = "attachments";
    private static final String AVATAR_DIR = "avatars";
    private static final String PURPOSE_AVATAR = "avatar";
    private static final List<String> ALLOWED_AVATAR_MIME_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final AttachFileService attachFileService;
    private final TusFileUploadService tusFileUploadService;
    private final UploadStorageService uploadStorageService;
    private final long maxImageSizeBytes;

    public AttachFileController(
            AttachFileService attachFileService,
            TusFileUploadService tusFileUploadService,
            UploadStorageService uploadStorageService,
            @Value("${app.upload.image-max-size-bytes:5242880}") long maxImageSizeBytes) {
        this.attachFileService = attachFileService;
        this.tusFileUploadService = tusFileUploadService;
        this.uploadStorageService = uploadStorageService;
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    @PostMapping
    public ResponseEntity<ApiResponseTemplate<AttachFileDto.Response>> create(
            @Validated @RequestBody AttachFileDto.Request req) {
        AttachFileDto.Response resp = attachFileService.create(req);
        return ApiResponseFactory.created(URI.create("/api/attach-files/" + resp.id), resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseTemplate<AttachFileDto.Response>> get(@PathVariable Long id) {
        return attachFileService.get(id)
                .map(ApiResponseFactory::ok)
                .orElseGet(() -> ApiResponseFactory.badRequest("요청하신 첨부파일을 찾을 수 없습니다."));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponseTemplate<List<AttachFileDto.Response>>> listByPost(@PathVariable Long postId) {
        return ApiResponseFactory.ok(attachFileService.listByPost(postId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseTemplate<Void>> delete(@PathVariable Long id) {
        attachFileService.delete(id);
        return ApiResponseFactory.noContent();
    }

    // TUS 업로드 완료 확인 API
    @PostMapping("/complete")
    public ResponseEntity<ApiResponseTemplate<TusCompleteResponse>> completeUpload(
            @RequestBody TusCompleteRequest completeReq,
            HttpServletRequest request) throws Exception {
        String uploadId = normalizeRequired(completeReq.uploadId, "업로드 ID가 없습니다.");
        String uploadUri = "/api/attach-files/uploads/" + uploadId;
        log.info("tus complete requested: uploadId={}, postId={}", uploadId, completeReq.postId);

        // TUS 업로드 정보 확인
        me.desair.tus.server.upload.UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(uploadUri);
        if (uploadInfo == null) {
            log.warn("tus upload info not found: uploadId={}", uploadId);
            return ApiResponseFactory.badRequest("업로드 정보를 찾을 수 없습니다.");
        }

        // 업로드 완료 여부 확인
        boolean isComplete = uploadInfo.getOffset() != null && uploadInfo.getLength() != null
                && uploadInfo.getOffset().equals(uploadInfo.getLength());
        if (!isComplete) {
            log.warn("tus upload not finished: uploadId={}, offset={}, length={}",
                    uploadId, uploadInfo.getOffset(), uploadInfo.getLength());
            return ApiResponseFactory.badRequest("업로드가 아직 완료되지 않았습니다.");
        }

        // 파일명 처리
        String originalFileName = uploadInfo.getFileName() != null ? uploadInfo.getFileName() : "file";
        String storedName = normalizeStoredName(completeReq.storedName);
        String normalizedPurpose = normalizePurpose(completeReq.purpose);
        boolean avatarUpload = PURPOSE_AVATAR.equals(normalizedPurpose);
        if (avatarUpload) {
            ResponseEntity<ApiResponseTemplate<TusCompleteResponse>> validationError = validateAvatarUpload(uploadInfo, uploadUri, uploadId);
            if (validationError != null) {
                return validationError;
            }
        }
        if (storedName == null) {
            storedName = UUID.randomUUID().toString();
        }

        String extension = resolveExtension(originalFileName, uploadInfo.getFileMimeType(), avatarUpload);
        if (extension != null && !storedName.contains(".")) {
            storedName = storedName + extension;
        }

        String storedPath;
        try (InputStream inputStream = tusFileUploadService.getUploadedBytes(uploadUri)) {
            storedPath = uploadStorageService.store(
                    (avatarUpload ? AVATAR_DIR : DEFAULT_ATTACH_DIR) + "/" + storedName,
                    inputStream,
                    uploadInfo.getFileMimeType(),
                    uploadInfo.getLength());
        }
        log.info("tus file stored: uploadId={}, path={}", uploadId, storedPath);

        // DB에 저장
        AttachFileDto.Request req = new AttachFileDto.Request();
        req.postId = completeReq.postId;
        req.originalName = completeReq.originalName != null ? completeReq.originalName : originalFileName;
        req.storedName = storedName;
        req.path = storedPath;

        AttachFileDto.Response resp = attachFileService.create(req);
        log.info("attach file metadata saved: attachFileId={}, uploadId={}", resp.id, uploadId);

        String publicUrl = buildPublicUrl(request, storedPath);
        String markdownAlt = completeReq.originalName != null ? completeReq.originalName : storedName;

        TusCompleteResponse completeResponse = new TusCompleteResponse();
        completeResponse.id = resp.id;
        completeResponse.postId = resp.postId;
        completeResponse.originalName = resp.originalName;
        completeResponse.storedName = resp.storedName;
        completeResponse.path = resp.path;
        completeResponse.fileUrl = publicUrl;
        completeResponse.displayUrl = publicUrl;
        completeResponse.markdown = "![" + markdownAlt + "](" + publicUrl + ")";

        cleanupTusUpload(uploadUri, uploadId);
        return ApiResponseFactory.created(URI.create("/api/attach-files/" + resp.id), completeResponse);
    }

    // TUS 업로드 정보 조회 API (업로드 완료 후 실제 파일 URL 가져오기)
    @GetMapping("/uploads/{uploadId}/info")
    public ResponseEntity<ApiResponseTemplate<TusUploadInfo>> getUploadInfo(@PathVariable String uploadId)
            throws Exception {
        me.desair.tus.server.upload.UploadInfo uploadInfo = tusFileUploadService
                .getUploadInfo("/api/attach-files/uploads/" + uploadId);

        if (uploadInfo == null) {
            return ApiResponseFactory.badRequest("업로드 정보를 찾을 수 없습니다.");
        }

        String fileName = uploadInfo.getFileName() != null ? uploadInfo.getFileName() : "file";

        // 실제 파일 URL 생성
        String fileUrl = "/api/attach-files/uploads/" + uploadId;

        TusUploadInfo info = new TusUploadInfo();
        info.uploadId = uploadId;
        info.fileName = fileName;
        info.fileSize = uploadInfo.getLength();
        info.isComplete = (uploadInfo.getOffset() != null && uploadInfo.getLength() != null
                && uploadInfo.getOffset().equals(uploadInfo.getLength())); // 업로드 완료 여부
        info.fileUrl = fileUrl; // 실제 파일 다운로드 URL
        info.displayUrl = fileUrl; // 이미지 표시용 URL (동일)

        return ApiResponseFactory.ok(info);
    }

    // TUS 파일 다운로드 API
    @GetMapping("/uploads/{uploadId}/download")
    public void downloadUploadedFile(@PathVariable String uploadId, HttpServletResponse response) throws Exception {
        me.desair.tus.server.upload.UploadInfo uploadInfo = tusFileUploadService
                .getUploadInfo("/api/attach-files/uploads/" + uploadId);

        if (uploadInfo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "파일을 찾을 수 없습니다.");
            return;
        }

        // 업로드 완료 여부 확인
        boolean isComplete = uploadInfo.getOffset() != null && uploadInfo.getLength() != null
                && uploadInfo.getOffset().equals(uploadInfo.getLength());

        if (!isComplete) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "업로드가 아직 완료되지 않았습니다.");
            return;
        }

        // 파일 다운로드 처리
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "inline; filename=\"" + uploadInfo.getFileName() + "\"");

        try (var inputStream = tusFileUploadService.getUploadedBytes("/api/attach-files/uploads/" + uploadId)) {
            inputStream.transferTo(response.getOutputStream());
        }
    }

    @RequestMapping(value = {"/uploads", "/uploads/", "/uploads/**"}, method = { RequestMethod.OPTIONS, RequestMethod.POST,
            RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.DELETE, RequestMethod.GET })
    public void handleTusRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        tusFileUploadService.process(request, response);
        if ("POST".equals(request.getMethod()) || "PATCH".equals(request.getMethod()) || "HEAD".equals(request.getMethod())) {
            log.info("tus request processed: method={}, uri={}, status={}, uploadOffset={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    response.getHeader("Upload-Offset"));
        }
    }

    // TUS 완료 요청 DTO
    public static class TusCompleteRequest {
        public String uploadId; // TUS 업로드 ID
        public Long postId;
        public String originalName;
        public String storedName;
        public String path;
        public String purpose;
    }

    // TUS 업로드 정보 응답 DTO
    public static class TusUploadInfo {
        public String uploadId;
        public String fileName;
        public Long fileSize;
        public Boolean isComplete;
        public String fileUrl; // 파일 다운로드 URL
        public String displayUrl; // 이미지 표시용 URL
    }

    public static class TusCompleteResponse {
        public Long id;
        public Long postId;
        public String originalName;
        public String storedName;
        public String path;
        public String fileUrl;
        public String displayUrl;
        public String markdown;
    }

    private String buildPublicUrl(HttpServletRequest request, String path) {
        return uploadStorageService.toPublicUrl(request, path);
    }

    private void cleanupTusUpload(String uploadUri, String uploadId) {
        try {
            tusFileUploadService.deleteUpload(uploadUri);
            log.info("tus temp upload deleted: uploadId={}", uploadId);
        } catch (Exception ex) {
            log.warn("failed to delete tus temp upload: uploadId={}", uploadId, ex);
        }
    }

    private String normalizeStoredName(String storedName) {
        String normalized = normalizeNullable(storedName);
        if (normalized == null) {
            return null;
        }
        String sanitized = normalized.replace('\\', '_')
                .replace('/', '_')
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
        sanitized = sanitized.replaceAll("^[_\\-.]+", "").replaceAll("[_\\-.]+$", "");
        return sanitized.isBlank() ? null : sanitized.toLowerCase(Locale.ROOT);
    }

    private ResponseEntity<ApiResponseTemplate<TusCompleteResponse>> validateAvatarUpload(
            me.desair.tus.server.upload.UploadInfo uploadInfo,
            String uploadUri,
            String uploadId) {
        String mimeType = normalizeNullable(uploadInfo.getFileMimeType());
        String normalizedMimeType = mimeType == null ? null : mimeType.toLowerCase(Locale.ROOT);
        if (normalizedMimeType == null || !ALLOWED_AVATAR_MIME_TYPES.contains(normalizedMimeType)) {
            log.warn("avatar upload rejected by mime type: uploadId={}, mimeType={}", uploadId, mimeType);
            cleanupTusUpload(uploadUri, uploadId);
            return ApiResponseFactory.badRequest("아바타는 JPEG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }
        Long length = uploadInfo.getLength();
        if (length != null && length > maxImageSizeBytes) {
            log.warn("avatar upload rejected by size: uploadId={}, length={}", uploadId, length);
            cleanupTusUpload(uploadUri, uploadId);
            return ApiResponseFactory.badRequest("아바타 이미지 크기 제한을 초과했습니다.");
        }
        return null;
    }

    private String resolveExtension(String originalFileName, String mimeType, boolean avatarUpload) {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        if (!avatarUpload || mimeType == null) {
            return null;
        }
        return switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> null;
        };
    }

    private String normalizePurpose(String purpose) {
        String normalized = normalizeNullable(purpose);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
