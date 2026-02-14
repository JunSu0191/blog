package com.study.blog.core.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CursorResponse<T> {
    private final List<T> content;
    private final Long nextCursorId;
    private final boolean hasNext;
    private final int size;
}
