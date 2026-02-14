package com.study.blog.comment;

import com.study.blog.comment.dto.CommentDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.user.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 댓글 관련 REST 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    public CommentController(CommentService commentService, UserRepository userRepository) {
        this.commentService = commentService;
        this.userRepository = userRepository;
    }

    /**
     * 특정 게시글의 댓글 목록 조회
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponseTemplate<List<CommentDto.Response>>> getComments(@PathVariable Long postId) {
        List<CommentDto.Response> comments = commentService.getCommentsByPostId(postId);
        return ApiResponseFactory.ok(comments);
    }

    /**
     * 댓글 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponseTemplate<CommentDto.Response>> create(@Validated @RequestBody CommentDto.CreateRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")).getId();

        CommentDto.Response resp = commentService.createComment(req, userId);
        return ApiResponseFactory.created(URI.create("/api/comments/" + resp.id), resp);
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseTemplate<CommentDto.Response>> update(@PathVariable Long id,
            @Validated @RequestBody CommentDto.UpdateRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")).getId();

        CommentDto.Response resp = commentService.updateComment(id, req, userId);
        return ApiResponseFactory.ok(resp);
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseTemplate<Void>> delete(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")).getId();

        commentService.deleteComment(id, userId);
        return ApiResponseFactory.noContent();
    }
}