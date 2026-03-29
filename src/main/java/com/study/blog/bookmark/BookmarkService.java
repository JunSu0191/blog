package com.study.blog.bookmark;

import com.study.blog.bookmark.dto.BookmarkDto;
import com.study.blog.content.ContentMapperService;
import com.study.blog.content.dto.ContentDto;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.Post;
import com.study.blog.post.PostErrorCode;
import com.study.blog.post.PostRepository;
import com.study.blog.post.ScheduledPostPublicationService;
import com.study.blog.post.PostStatus;
import com.study.blog.post.PostVisibility;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookmarkService {

    private static final String FLAG_NO = "N";

    private final PostBookmarkRepository postBookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ContentMapperService contentMapperService;
    private final ScheduledPostPublicationService scheduledPostPublicationService;

    public BookmarkService(PostBookmarkRepository postBookmarkRepository,
                           PostRepository postRepository,
                           UserRepository userRepository,
                           ContentMapperService contentMapperService,
                           ScheduledPostPublicationService scheduledPostPublicationService) {
        this.postBookmarkRepository = postBookmarkRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.contentMapperService = contentMapperService;
        this.scheduledPostPublicationService = scheduledPostPublicationService;
    }

    public Page<ContentDto.PostCard> listBookmarks(Long userId, Pageable pageable) {
        publishDueScheduledPosts();
        Page<PostBookmark> page = postBookmarkRepository.findVisibleBookmarksByUserId(userId, pageable);
        List<Post> posts = page.getContent().stream().map(PostBookmark::getPost).toList();
        return contentMapperService.toPostCards(new PageImpl<>(posts, page.getPageable(), page.getTotalElements()));
    }

    public BookmarkDto.BookmarkStatusResponse bookmark(Long userId, Long postId) {
        Post post = getAccessiblePost(postId, userId);
        User user = getUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();

        PostBookmark bookmark = postBookmarkRepository.findByPost_IdAndUser_Id(postId, userId)
                .orElseGet(() -> PostBookmark.builder()
                        .post(post)
                        .user(user)
                        .createdAt(now)
                        .build());

        bookmark.setDeletedYn(FLAG_NO);
        bookmark.setBookmarkedAt(now);
        bookmark.setUpdatedAt(now);
        PostBookmark saved = postBookmarkRepository.save(bookmark);
        return new BookmarkDto.BookmarkStatusResponse(postId, true, saved.getBookmarkedAt());
    }

    public void unbookmark(Long userId, Long postId) {
        getAccessiblePost(postId, userId);
        postBookmarkRepository.findByPost_IdAndUser_Id(postId, userId)
                .ifPresent(bookmark -> {
                    bookmark.setDeletedYn("Y");
                    bookmark.setUpdatedAt(LocalDateTime.now());
                    postBookmarkRepository.save(bookmark);
                });
    }

    public BookmarkDto.BookmarkStatusResponse getStatus(Long userId, Long postId) {
        getAccessiblePost(postId, userId);
        return postBookmarkRepository.findByPost_IdAndUser_Id(postId, userId)
                .filter(bookmark -> FLAG_NO.equalsIgnoreCase(bookmark.getDeletedYn()))
                .map(bookmark -> new BookmarkDto.BookmarkStatusResponse(postId, true, bookmark.getBookmarkedAt()))
                .orElseGet(() -> new BookmarkDto.BookmarkStatusResponse(postId, false, null));
    }

    private void publishDueScheduledPosts() {
        scheduledPostPublicationService.publishDueScheduledPosts();
    }

    private Post getAccessiblePost(Long postId, Long actorUserId) {
        publishDueScheduledPosts();
        Post post = postRepository.findWithAssociationsById(postId)
                .orElseThrow(() -> new CodedApiException(
                        PostErrorCode.POST_NOT_FOUND.code(),
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."));

        if (post.isDeleted()) {
            throw new CodedApiException(
                    PostErrorCode.POST_NOT_FOUND.code(),
                    HttpStatus.NOT_FOUND,
                    "게시글을 찾을 수 없습니다.");
        }

        boolean owner = post.getUser() != null && post.getUser().getId() != null && post.getUser().getId().equals(actorUserId);
        if (owner) {
            return post;
        }

        if (post.getStatus() != PostStatus.PUBLISHED || post.getPublishedAt() == null) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "저장할 수 없는 게시글입니다.");
        }

        if (post.getVisibility() == PostVisibility.PRIVATE) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "저장할 수 없는 게시글입니다.");
        }
        return post;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> FLAG_NO.equalsIgnoreCase(user.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
