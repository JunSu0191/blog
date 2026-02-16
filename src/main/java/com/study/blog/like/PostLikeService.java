package com.study.blog.like;

import com.study.blog.like.dto.LikeDto;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostLikeService(PostLikeRepository postLikeRepository,
                           PostRepository postRepository,
                           UserRepository userRepository) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public LikeDto.PostLikeResponse like(Long userId, Long postId) {
        return setLike(userId, postId, true);
    }

    public LikeDto.PostLikeResponse unlike(Long userId, Long postId) {
        return setLike(userId, postId, false);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public LikeDto.PostLikeResponse getLikeStatus(Long userId, Long postId) {
        Post post = getPostOrThrow(postId);
        boolean liked = postLikeRepository.findByPost_IdAndUser_Id(postId, userId)
                .map(it -> "N".equals(it.getDeletedYn()))
                .orElse(false);

        LikeDto.PostLikeResponse response = new LikeDto.PostLikeResponse();
        response.setPostId(postId);
        response.setLiked(liked);
        response.setLikeCount(normalize(post.getLikeCount()));
        return response;
    }

    private LikeDto.PostLikeResponse setLike(Long userId, Long postId, boolean liked) {
        Post post = getPostOrThrow(postId);
        PostLike postLike = postLikeRepository.findByPost_IdAndUser_Id(postId, userId).orElse(null);
        boolean currentlyLiked = postLike != null && "N".equals(postLike.getDeletedYn());
        boolean countChanged = false;

        if (liked && !currentlyLiked) {
            if (postLike == null) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
                postLike = PostLike.builder()
                        .post(post)
                        .user(user)
                        .deletedYn("N")
                        .build();
            } else {
                postLike.setDeletedYn("N");
            }
            postLikeRepository.save(postLike);
            post.setLikeCount(normalize(post.getLikeCount()) + 1);
            countChanged = true;
        } else if (!liked && currentlyLiked) {
            postLike.setDeletedYn("Y");
            postLikeRepository.save(postLike);
            post.setLikeCount(Math.max(0L, normalize(post.getLikeCount()) - 1));
            countChanged = true;
        }

        if (countChanged) {
            postRepository.save(post);
        }

        LikeDto.PostLikeResponse response = new LikeDto.PostLikeResponse();
        response.setPostId(postId);
        response.setLiked(liked && (!currentlyLiked || (postLike != null && "N".equals(postLike.getDeletedYn()))));
        response.setLikeCount(normalize(post.getLikeCount()));
        return response;
    }

    private Post getPostOrThrow(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));
        if (post.isDeleted()) {
            throw new IllegalArgumentException("삭제된 게시글에는 좋아요를 처리할 수 없습니다.");
        }
        return post;
    }

    private long normalize(Long value) {
        return value == null ? 0L : value;
    }
}
