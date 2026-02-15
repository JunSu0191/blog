package com.study.blog.post;

import com.study.blog.core.response.CursorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.study.blog.attach.AttachFile;
import com.study.blog.attach.dto.AttachFileDto;
import com.study.blog.post.dto.PostDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 게시글 관련 비즈니스 로직 서비스 레이어입니다.
 * - 컨트롤러는 얇게 유지하고 모든 핵심 처리 로직은 이 클래스에서 수행합니다.
 */
@Slf4j
@Service
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    /**
     * 모든 게시글 조회 (페이지네이션 및 검색 지원)
     * 
     * @param pageable 페이지네이션 정보
     * @param keyword  검색 키워드 (제목 또는 내용)
     * @return 페이지네이션된 게시글 목록
     */
    public Page<PostDto.Response> listPosts(Pageable pageable, String keyword) {
        String normalizedKeyword = keyword != null ? keyword.trim() : null;
        Page<Post> posts;
        if (normalizedKeyword == null || normalizedKeyword.isEmpty()) {
            posts = postRepository.findByDeletedYn("N", pageable);
        } else {
            posts = postRepository.findByDeletedYnAndTitleContainingIgnoreCaseOrDeletedYnAndContentContainingIgnoreCase(
                    "N", normalizedKeyword, "N", normalizedKeyword, pageable);
        }
        return posts.map(this::toResponse);
    }

    public CursorResponse<PostDto.Response> listPostsByCursor(Long cursorId, int size, String keyword) {
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        int fetchSize = normalizedSize + 1;
        String normalizedKeyword = keyword != null ? keyword.trim() : null;

        List<Post> posts = (normalizedKeyword == null || normalizedKeyword.isEmpty())
                ? postRepository.findCursorPageWithoutKeyword("N", cursorId, PageRequest.of(0, fetchSize))
                : postRepository.findCursorPageWithKeyword("N", normalizedKeyword, cursorId, PageRequest.of(0, fetchSize));

        boolean hasNext = posts.size() > normalizedSize;
        List<Post> pagePosts = hasNext ? posts.subList(0, normalizedSize) : posts;
        Long nextCursorId = hasNext && !pagePosts.isEmpty() ? pagePosts.get(pagePosts.size() - 1).getId() : null;

        List<PostDto.Response> content = pagePosts.stream()
                .map(this::toResponse)
                .toList();

        return new CursorResponse<>(content, nextCursorId, hasNext, normalizedSize);
    }

    /**
     * 모든 게시글 조회 (기존 메서드 - 호환성 유지)
     * 
     * @return 모든 게시글 목록
     */
    public List<PostDto.Response> listPosts() {
        List<Post> posts = postRepository.findAll()
                .stream()
                .filter(p -> "N".equals(p.getDeletedYn()))
                .collect(Collectors.toList());

        return posts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 새 게시글을 작성합니다. 작성자(`userId`)가 존재하는지 검증합니다.
     */
    public PostDto.Response createPost(PostDto.Request req, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Post post = Post.builder()
                .user(user)
                .title(req.title)
                .content(req.content)
                .deletedYn("N")
                .createdAt(LocalDateTime.now())
                .viewCount(0L)
                .likeCount(0L)
                .build();
        log.info("  Creating post: {}", post);

        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    /**
     * id로 게시글을 조회합니다. 삭제된 게시글은 제외합니다.
     */
    public Optional<PostDto.Response> getPost(Long id) {
        return postRepository.findById(id)
                .filter(p -> "N".equals(p.getDeletedYn()))
                .map(this::toResponse);
    }

    /**
     * 게시글의 제목과 내용을 수정합니다. 작성자 변경은 허용하지 않습니다.
     */
    public PostDto.Response updatePost(Long id, PostDto.Request req) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));

        if ("Y".equals(post.getDeletedYn())) {
            throw new IllegalStateException("Cannot update deleted post: " + id);
        }

        // only update mutable fields
        post.setTitle(req.title);
        post.setContent(req.content);

        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    /**
     * 게시글을 소프트 삭제합니다(`deletedYn`을 'Y'로 설정).
     */
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        post.setDeletedYn("Y");
        postRepository.save(post);
    }

    /**
     * 특정 사용자의 게시글 목록을 조회합니다 (삭제되지 않은 것만).
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<PostDto.Response> listByUser(Long userId) {
        return postRepository.findByUser_IdAndDeletedYn(userId, "N")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PostDto.Response toResponse(Post p) {
        PostDto.Response r = new PostDto.Response();
        r.id = p.getId();
        r.userId = p.getUser().getId();
        r.title = p.getTitle();
        r.content = p.getContent();
        r.deletedYn = p.getDeletedYn();
        r.createdAt = p.getCreatedAt();
        r.viewCount = p.getViewCount();
        r.likeCount = p.getLikeCount();
        // attach files mapping
        r.attachFiles = p.getAttachFiles() != null ? p.getAttachFiles()
                .stream()
                .filter(a -> "N".equals(a.getDeletedYn()))
                .map((AttachFile a) -> {
                    AttachFileDto.Response ar = new AttachFileDto.Response();
                    ar.id = a.getId();
                    ar.postId = a.getPost().getId();
                    ar.originalName = a.getOriginalName();
                    ar.storedName = a.getStoredName();
                    ar.path = a.getPath();
                    ar.deletedYn = a.getDeletedYn();
                    ar.createdAt = a.getCreatedAt();
                    return ar;
                })
                .collect(Collectors.toList()) : new ArrayList<>();
        return r;
    }
}
