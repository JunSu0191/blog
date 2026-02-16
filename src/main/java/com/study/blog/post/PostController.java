package com.study.blog.post;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.response.CursorResponse;
import com.study.blog.post.dto.PostDto;
import com.study.blog.user.UserRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 게시글 관련 간단한 REST 컨트롤러입니다.
 * - 비즈니스 로직은 `PostService`에 위임합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;

    public PostController(PostService postService, UserRepository userRepository) {
        this.postService = postService;
        this.userRepository = userRepository;
    }

    /**
     * 모든 게시물 조회
     *
     * mode=cursor(기본): 무한 스크롤용 커서 응답
     * mode=page: 기존 페이지네이션 응답
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "cursor") String mode,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") int size,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword) {

        if ("page".equalsIgnoreCase(mode)) {
            Page<PostDto.Response> resp = postService.listPosts(pageable, keyword);
            return ApiResponseFactory.ok(resp);
        }

        if (!"cursor".equalsIgnoreCase(mode)) {
            return ApiResponseFactory.badRequest("mode는 cursor 또는 page만 사용할 수 있습니다.");
        }

        CursorResponse<PostDto.Response> resp = postService.listPostsByCursor(cursorId, size, keyword);
        return ApiResponseFactory.ok(resp);
    }

    /**
     * 새 게시글을 생성합니다.
     */
    @PostMapping
    public ResponseEntity<ApiResponseTemplate<PostDto.Response>> create(@Validated @RequestBody PostDto.Request req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Long userId = userRepository.findByUsernameAndDeletedYn(username, "N")
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")).getId();

        PostDto.Response resp = postService.createPost(req, userId);
        return ApiResponseFactory.created(URI.create("/api/posts/" + resp.id), resp);
    }

    /**
     * id로 게시글을 조회합니다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseTemplate<PostDto.Response>> get(@PathVariable Long id) {
        return postService.getPost(id)
                .map(ApiResponseFactory::ok)
                .orElseGet(() -> ApiResponseFactory.badRequest("요청하신 게시글을 찾을 수 없습니다."));
    }

    /**
     * 게시글을 수정합니다. 컨트롤러는 요청 수신 및 전달만 수행합니다.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseTemplate<PostDto.Response>> update(@PathVariable Long id,
            @Validated @RequestBody PostDto.Request req) {
        PostDto.Response resp = postService.updatePost(id, req);
        return ApiResponseFactory.ok(resp);
    }

    /**
     * 게시글을 소프트 삭제합니다.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseTemplate<Void>> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return ApiResponseFactory.noContent();
    }

    /**
     * 사용자별 게시글 목록을 반환합니다.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponseTemplate<List<PostDto.Response>>> listByUser(@PathVariable Long userId) {
        return ApiResponseFactory.ok(postService.listByUser(userId));
    }

    /**
     * 테스트용 - 예외 발생
     */
    @GetMapping("/test-error")
    public ResponseEntity<ApiResponseTemplate<Object>> testError() {
        throw new RuntimeException("테스트 예외");
    }
}
