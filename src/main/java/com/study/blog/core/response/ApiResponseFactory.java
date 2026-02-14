package com.study.blog.core.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

public final class ApiResponseFactory {

    private ApiResponseFactory() {
    }

    public static <T> ResponseEntity<ApiResponseTemplate<T>> ok(T data) {
        return ResponseEntity.ok(new ApiResponseTemplate<>(data));
    }

    public static <T> ResponseEntity<ApiResponseTemplate<PageResponse<T>>> ok(
            org.springframework.data.domain.Page<T> page) {
        PageResponse<T> pageResponse = PageResponse.from(page);
        return ResponseEntity.ok(new ApiResponseTemplate<>(pageResponse));
    }

    public static <T> ResponseEntity<ApiResponseTemplate<T>> badRequest(String message) {
        ApiResponseTemplate<T> body = new ApiResponseTemplate<>(null, org.springframework.http.HttpStatus.BAD_REQUEST,
                message, false);
        return ResponseEntity.badRequest().body(body);
    }

    public static <T> ResponseEntity<ApiResponseTemplate<T>> unauthorized(String message) {
        ApiResponseTemplate<T> body = new ApiResponseTemplate<>(null, org.springframework.http.HttpStatus.UNAUTHORIZED,
                message, false);
        return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(body);
    }

    public static <T> ResponseEntity<ApiResponseTemplate<T>> forbidden(String message) {
        ApiResponseTemplate<T> body = new ApiResponseTemplate<>(null, org.springframework.http.HttpStatus.FORBIDDEN,
                message, false);
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(body);
    }

    public static <T> ResponseEntity<ApiResponseTemplate<T>> internalError(String message) {
        ApiResponseTemplate<T> body = new ApiResponseTemplate<>(null,
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, message, false);
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    public static <T> ResponseEntity<ApiResponseTemplate<T>> created(URI uri, T data) {
        ApiResponseTemplate<T> body = new ApiResponseTemplate<>(data, HttpStatus.CREATED);
        return ResponseEntity.created(uri).body(body);
    }

    public static ResponseEntity<ApiResponseTemplate<Void>> noContent() {
        ApiResponseTemplate<Void> body = new ApiResponseTemplate<>(null, HttpStatus.NO_CONTENT);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(body);
    }
}
