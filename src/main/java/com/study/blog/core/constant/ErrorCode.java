package com.study.blog.core.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorCode {
    BAD_REQUEST_ERROR(10000, HttpStatus.BAD_REQUEST, "Bad request"),
    AUTH_UNAUTHORIZED_ERROR(20000, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    AUTH_FORBIDDEN_ERROR(20001, HttpStatus.FORBIDDEN, "Forbidden"),
    BUSINESS_ERROR(40000, HttpStatus.BAD_REQUEST, "Business error"),
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "Internal error"),
    DATA_VIOLATION_EXCEPTION(40009, HttpStatus.CONFLICT, "데이터 무결성 위반"),
    ;

    private final Integer code;
    private final HttpStatus httpStatus;
    private final String message;

    public String getMessage(Throwable e) {
        return this.getMessage(this.getMessage() + " - " + e.getMessage());
    }

    public String getMessage(String message) {
        return Optional.ofNullable(message)
                .filter(Predicate.not(String::isBlank))
                .orElse(this.getMessage());
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", this.name(), this.getCode());
    }

}
