package com.study.blog.mypage;

import com.study.blog.comment.dto.CommentDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.mypage.dto.MyPageDto;
import com.study.blog.post.dto.PostDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private final CurrentUserResolver currentUserResolver;

    public MyPageController(MyPageService myPageService, CurrentUserResolver currentUserResolver) {
        this.myPageService = myPageService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponseTemplate<MyPageDto.SummaryResponse>> getMySummary(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(myPageService.getSummary(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponseTemplate<MyPageDto.SummaryResponse>> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody MyPageDto.UpdateProfileRequest req) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(myPageService.upsertProfile(userId, req));
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponseTemplate<List<PostDto.Response>>> getMyPosts(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(myPageService.getMyPosts(userId));
    }

    @GetMapping("/comments")
    public ResponseEntity<ApiResponseTemplate<List<CommentDto.Response>>> getMyComments(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(myPageService.getMyComments(userId));
    }
}
