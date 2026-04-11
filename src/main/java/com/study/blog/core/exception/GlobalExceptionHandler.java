package com.study.blog.core.exception;

import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.post.PostErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseTemplate<HashMap<String, List<String>>>> handleValidation(
            MethodArgumentNotValidException ex) {
        HashMap<String, List<String>> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(f -> f.getField(),
                        Collectors.mapping(f -> f.getDefaultMessage(), Collectors.toList())))
                .entrySet().stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseTemplate<>(details, HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다.", false));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseTemplate<Object>> handleBadRequest(IllegalArgumentException ex) {
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "잘못된 요청입니다."
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseTemplate<>(null, HttpStatus.BAD_REQUEST, message, false));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseTemplate<Object>> handleConflict(IllegalStateException ex) {
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "요청을 처리할 수 없습니다."
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponseTemplate<>(null, HttpStatus.CONFLICT, message, false));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseTemplate<Object>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseTemplate<>(null, HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.", false));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponseTemplate<Object>> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponseTemplate<>(null, HttpStatus.FORBIDDEN, "접근이 거부되었습니다.", false));
    }

    @ExceptionHandler(CodedApiException.class)
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> handleCodedApiException(CodedApiException ex) {
        HttpStatus status = ex.getStatus();
        Map<String, Object> error = Map.of(
                "code", ex.getCode(),
                "message", ex.getMessage(),
                "status", status.value());
        return ResponseEntity.status(status)
                .body(new ApiResponseTemplate<>(error, status, ex.getMessage(), false));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "업로드 가능한 최대 파일 크기를 초과했습니다.";
        Map<String, Object> error = Map.of(
                "code", PostErrorCode.UPLOAD_FAILED.code(),
                "message", message,
                "status", status.value());
        return ResponseEntity.status(status)
                .body(new ApiResponseTemplate<>(error, status, message, false));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> handleNoResourceFound(NoResourceFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = "요청한 경로를 찾을 수 없습니다.";
        Map<String, Object> error = Map.of(
                "code", "resource_not_found",
                "message", message,
                "status", status.value());
        return ResponseEntity.status(status)
                .body(new ApiResponseTemplate<>(error, status, message, false));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseTemplate<Map<String, Object>>> handleAll(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "서버에서 오류가 발생했습니다.";
        Map<String, Object> error = Map.of(
                "code", PostErrorCode.GENERIC_SERVER_ERROR.code(),
                "message", message,
                "status", status.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseTemplate<>(error, status, message, false));
    }
}
