package com.study.blog.attach;

import com.study.blog.attach.dto.AttachFileDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.web.PublicUrlBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.TusFileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attach-files")
public class AttachFileController {
    private static final Logger log = LoggerFactory.getLogger(AttachFileController.class);

    private final AttachFileService attachFileService;
    private final TusFileUploadService tusFileUploadService;
    private final PublicUrlBuilder publicUrlBuilder;

    public AttachFileController(
            AttachFileService attachFileService,
            TusFileUploadService tusFileUploadService,
            PublicUrlBuilder publicUrlBuilder) {
        this.attachFileService = attachFileService;
        this.tusFileUploadService = tusFileUploadService;
        this.publicUrlBuilder = publicUrlBuilder;
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
        String uploadId = completeReq.uploadId;
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
        String storedName = completeReq.storedName != null ? completeReq.storedName : UUID.randomUUID().toString();

        // 파일 확장자 추가 (원본 파일명에서)
        if (originalFileName.contains(".") && !storedName.contains(".")) {
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            storedName = storedName + extension;
        }

        // 최종 저장 경로 설정
        Path uploadsDir = Paths.get(TusConfiguration.FINAL_UPLOAD_DIR);
        if (!Files.exists(uploadsDir)) {
            Files.createDirectories(uploadsDir);
        }
        Path finalFilePath = uploadsDir.resolve(storedName);

        // TUS 임시 파일을 최종 위치로 복사
        try (InputStream inputStream = tusFileUploadService.getUploadedBytes(uploadUri)) {
            Files.copy(inputStream, finalFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("tus file copied to final path: uploadId={}, path={}", uploadId, finalFilePath);

        // DB에 저장
        AttachFileDto.Request req = new AttachFileDto.Request();
        req.postId = completeReq.postId;
        req.originalName = completeReq.originalName != null ? completeReq.originalName : originalFileName;
        req.storedName = storedName;
        String publicPath = "/upload/" + storedName;
        req.path = publicPath;

        AttachFileDto.Response resp = attachFileService.create(req);
        log.info("attach file metadata saved: attachFileId={}, uploadId={}", resp.id, uploadId);

        String publicUrl = buildPublicUrl(request, publicPath);
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

    @RequestMapping(value = "/uploads/**", method = { RequestMethod.OPTIONS, RequestMethod.POST,
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
        return publicUrlBuilder.build(request, path);
    }
}
