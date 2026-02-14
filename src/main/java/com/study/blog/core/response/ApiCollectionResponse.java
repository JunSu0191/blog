package com.study.blog.core.response;

import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

/**
 * 컬렉션 Api 응답
 * @param <T> 리소스 객체
 *
 * {
 *     "status": 200,
 *     "success": true,
 *     "data": [
 *         {
 *             "id": 1,
 *         "user_id": "admin",
 *             "name": "관리자"
 *         },{
 *             "id": "2",
 *             "user_id": "bread",
 *             "name": "브래드"
 *         },
 *         ...
 *     ],
 *     "pagination": {
 *         "count": 2,
 *         "total": 102,
 *         "perPage": 100,
 *         "currentPage": 2,
 *         "totalPages": 2,
 *         "links": {
 *             "previous": "http://proxima.test/admin/users?page=1"
 *         }
 *     }
 * }
 */
@Data
public class ApiCollectionResponse<T> {

    private final Page<T> page;
    private final org.springframework.http.HttpStatus status;
    private final boolean success;

    public ApiCollectionResponse(Page<T> page) {
        this.page = page;
        this.status = org.springframework.http.HttpStatus.OK;
        this.success = true;
    }
}
