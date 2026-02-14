package com.study.blog.core.exception;

import com.study.blog.core.response.ApiResponseTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseTemplate<>(null, HttpStatus.BAD_REQUEST, "잘못된 요청입니다.", false));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseTemplate<Object>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponseTemplate<>(null, HttpStatus.CONFLICT, "요청을 처리할 수 없습니다.", false));
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseTemplate<Object>> handleAll(Exception ex) {
        System.out.println("GlobalExceptionHandler.handleAll called: " + ex.getClass().getName() + " - " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseTemplate<>(null, HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다.", false));
    }
}
