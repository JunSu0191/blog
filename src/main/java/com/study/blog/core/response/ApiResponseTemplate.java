package com.study.blog.core.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 단일 객체 Api 응답
 * 
 * @param <T> 리소스 객체
 *
 *            {
 *            "status": 200,
 *            "success": true,
 *            "data": {
 *            "id": 1,
 *            "user_id": "admin",
 *            "name": "관리자"
 *            }
 *            }
 */
@Getter
@RequiredArgsConstructor
public class ApiResponseTemplate<T> {
    @Schema(description = "상태 코드 값", example = "OK")
    private final HttpStatus status;
    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "메시지")
    private final String message;
    private final T data;

    public ApiResponseTemplate(T data) {
        this.data = data;
        this.status = HttpStatus.OK;
        this.message = "";
        this.success = true;
    }

    public ApiResponseTemplate(T data, HttpStatus status) {
        this.data = data;
        this.status = status;
        this.message = "";
        this.success = true;
    }

    public ApiResponseTemplate(T data, HttpStatus status, String message) {
        this.data = data;
        this.status = status;
        this.message = message;
        this.success = true;
    }

    public static ApiResponseTemplate<Object> ok() {
        return new ApiResponseTemplate<>(null, HttpStatus.OK);
    }

    public static ApiResponseTemplate<Object> noContent() {
        return new ApiResponseTemplate<>(null, HttpStatus.NO_CONTENT);
    }

    public ApiResponseTemplate(T data, HttpStatus status, String message, boolean success) {
        this.data = data;
        this.status = status;
        this.message = message;
        this.success = success;
    }
}