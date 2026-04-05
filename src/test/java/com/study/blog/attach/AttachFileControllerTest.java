package com.study.blog.attach;

import com.study.blog.attach.dto.AttachFileDto;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.storage.UploadStorageService;
import jakarta.servlet.http.HttpServletRequest;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.upload.UploadInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachFileControllerTest {

    @Mock
    private AttachFileService attachFileService;
    @Mock
    private TusFileUploadService tusFileUploadService;
    @Mock
    private UploadStorageService uploadStorageService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private UploadInfo uploadInfo;

    @Test
    void completeUploadShouldStoreToS3AndDeleteTusTempFile() throws Exception {
        AttachFileController controller = new AttachFileController(
                attachFileService,
                tusFileUploadService,
                uploadStorageService);

        AttachFileController.TusCompleteRequest completeRequest = new AttachFileController.TusCompleteRequest();
        completeRequest.uploadId = "upload-123";
        completeRequest.postId = 44L;
        completeRequest.originalName = "photo.png";
        completeRequest.storedName = "photo_final";

        when(tusFileUploadService.getUploadInfo("/api/attach-files/uploads/upload-123")).thenReturn(uploadInfo);
        when(uploadInfo.getOffset()).thenReturn(5L);
        when(uploadInfo.getLength()).thenReturn(5L);
        when(uploadInfo.getFileName()).thenReturn("photo.png");
        when(uploadInfo.getFileMimeType()).thenReturn("image/png");
        when(tusFileUploadService.getUploadedBytes("/api/attach-files/uploads/upload-123"))
                .thenReturn(new ByteArrayInputStream("hello".getBytes()));
        when(uploadStorageService.store(any(), any(), any(), any())).thenReturn("https://cdn.example.com/attachments/photo_final.png");
        when(uploadStorageService.toPublicUrl(request, "https://cdn.example.com/attachments/photo_final.png"))
                .thenReturn("https://cdn.example.com/attachments/photo_final.png");

        AttachFileDto.Response attachResponse = new AttachFileDto.Response();
        attachResponse.id = 99L;
        attachResponse.postId = 44L;
        attachResponse.originalName = "photo.png";
        attachResponse.storedName = "photo_final.png";
        attachResponse.path = "https://cdn.example.com/attachments/photo_final.png";
        when(attachFileService.create(any())).thenReturn(attachResponse);

        ResponseEntity<ApiResponseTemplate<AttachFileController.TusCompleteResponse>> response =
                controller.completeUpload(completeRequest, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().fileUrl).isEqualTo("https://cdn.example.com/attachments/photo_final.png");

        verify(uploadStorageService).store(
                eq("attachments/photo_final.png"),
                any(),
                eq("image/png"),
                eq(5L));
        verify(tusFileUploadService).deleteUpload("/api/attach-files/uploads/upload-123");

        ArgumentCaptor<AttachFileDto.Request> captor = ArgumentCaptor.forClass(AttachFileDto.Request.class);
        verify(attachFileService).create(captor.capture());
        assertThat(captor.getValue().storedName).isEqualTo("photo_final.png");
        assertThat(captor.getValue().path).isEqualTo("https://cdn.example.com/attachments/photo_final.png");
    }
}
