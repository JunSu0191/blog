package com.study.blog.post;

import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.dto.PostContractDto;
import com.study.blog.storage.UploadStorageService;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PostImageUploadService {

    private static final String FLAG_NO = "N";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final UploadStorageService uploadStorageService;
    private final long maxImageSizeBytes;

    public PostImageUploadService(PostImageRepository postImageRepository,
                                  UserRepository userRepository,
                                  UploadStorageService uploadStorageService,
                                  @Value("${app.upload.image-max-size-bytes:5242880}") long maxImageSizeBytes) {
        this.postImageRepository = postImageRepository;
        this.userRepository = userRepository;
        this.uploadStorageService = uploadStorageService;
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public PostContractDto.ImageUploadResponse uploadImage(MultipartFile file,
                                                           Long uploaderUserId,
                                                           HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw uploadException(HttpStatus.BAD_REQUEST, "업로드할 이미지 파일이 비어 있습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw uploadException(HttpStatus.BAD_REQUEST, "허용되지 않은 이미지 확장자입니다.");
        }

        if (file.getSize() > maxImageSizeBytes) {
            throw uploadException(HttpStatus.BAD_REQUEST, "이미지 크기 제한을 초과했습니다.");
        }

        User uploader = userRepository.findById(uploaderUserId)
                .filter(user -> FLAG_NO.equalsIgnoreCase(user.getDeletedYn()))
                .orElseThrow(() -> uploadException(HttpStatus.UNAUTHORIZED, "업로더 사용자를 찾을 수 없습니다."));

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw uploadException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 파일을 읽는 중 실패했습니다.");
        }

        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }

            String storedName = UUID.randomUUID() + "." + extension;
            String storedPath = uploadStorageService.store(
                    "posts/" + storedName,
                    new ByteArrayInputStream(fileBytes),
                    file.getContentType(),
                    (long) fileBytes.length);

            PostImage metadata = PostImage.builder()
                    .uploader(uploader)
                    .url(storedPath)
                    .width(width)
                    .height(height)
                    .size(file.getSize())
                    .createdAt(LocalDateTime.now())
                    .build();

            postImageRepository.save(metadata);

            String publicUrl = uploadStorageService.toPublicUrl(request, storedPath);
            return new PostContractDto.ImageUploadResponse(publicUrl, width, height, file.getSize());
        } catch (IOException ex) {
            throw uploadException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 파일 저장에 실패했습니다.");
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT)
                .trim();
        return extension.replaceAll("[^a-z0-9]", "");
    }

    private CodedApiException uploadException(HttpStatus status, String message) {
        return new CodedApiException(PostErrorCode.UPLOAD_FAILED.code(), status, message);
    }
}
