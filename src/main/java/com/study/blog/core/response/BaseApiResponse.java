package com.study.blog.core.response;

import org.springframework.http.HttpStatus;

/**
 * Api Response 기본 속성
 * https://github.com/gemiso-dev/dev-docs/blob/main/standards/restful-api-standard.md#%EC%9A%94%EC%B2%AD%EA%B3%BC-%EC%9D%91%EB%8B%B5
 */
public abstract class BaseApiResponse {
    HttpStatus status;
    boolean success;
}
