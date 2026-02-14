package com.study.blog.core.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.study.blog.core.constant.ErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

/**
 * Api 에러 응답
 * {
 * "status": 400,
 * "success": false,
 * "error": {
 * "codeDTO": "validation_exception",
 * "message": "입력값이 유효하지 않습니다.",
 * "errors": {
 * "search": ["search 필드는 필수입니다."]
 * }
 * }
 * }
 */
@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiErrorResponse {
  @Schema(description = "에러 발생 시간", format = "yyyy-MM-dd HH:mm:ss", example = "2023-06-21 17:21:53")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private final LocalDateTime timestamp = LocalDateTime.now();
  @Schema(description = "성공여부", example = "false")
  private final boolean success;
  @Schema(description = "에러 코드", example = "40000")
  private final Integer errorCode;
  @Schema(description = "에러 메시지", example = "잘못된 요청 입니다.")
  private final String message;
  @Schema(description = "에러 상세 메시지", example = "{ 'errorName': ['errorMessage1, errorMessage2']}")
  private final HashMap<String, List<String>> details;


    public static ApiErrorResponse of(Boolean success, ErrorCode errorCode) {
        return new ApiErrorResponse(success, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiErrorResponse of(Boolean success, ErrorCode errorCode, Exception e) {
        return new ApiErrorResponse(success, errorCode.getCode(), errorCode.getMessage(e), null);
    }

    public static ApiErrorResponse of(Boolean success, ErrorCode errorCode, String message) {
        return new ApiErrorResponse(success, errorCode.getCode(), errorCode.getMessage(message), null);
    }

    public static ApiErrorResponse of(Boolean success, ErrorCode errorCode, HashMap<String, List<String>> details) {
        return new ApiErrorResponse(success, errorCode.getCode(), errorCode.getMessage(), details);
    }

    public static ApiErrorResponse of(Boolean success, ErrorCode errorCode, String message, HashMap<String, List<String>> details) {
        return new ApiErrorResponse(success, errorCode.getCode(), errorCode.getMessage(message), details);
    }
}
