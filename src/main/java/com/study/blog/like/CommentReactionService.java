package com.study.blog.like;

import com.study.blog.comment.Comment;
import com.study.blog.comment.CommentRepository;
import com.study.blog.like.dto.LikeDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CommentReactionService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    public CommentReactionService(CommentRepository commentRepository,
                                  CommentLikeRepository commentLikeRepository,
                                  UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
    }

    public LikeDto.CommentReactionResponse updateReaction(Long userId, Long commentId, CommentReactionType targetReaction) {
        Comment comment = getCommentOrThrow(commentId);
        CommentLike reaction = commentLikeRepository.findByComment_IdAndUser_Id(commentId, userId).orElse(null);
        CommentReactionType currentReaction = resolveReaction(reaction);

        if (currentReaction == targetReaction) {
            return toReactionResponse(comment, currentReaction);
        }

        if (currentReaction == CommentReactionType.LIKE) {
            comment.setLikeCount(Math.max(0L, normalize(comment.getLikeCount()) - 1));
        } else if (currentReaction == CommentReactionType.DISLIKE) {
            comment.setDislikeCount(Math.max(0L, normalize(comment.getDislikeCount()) - 1));
        }

        if (targetReaction == CommentReactionType.LIKE) {
            comment.setLikeCount(normalize(comment.getLikeCount()) + 1);
        } else if (targetReaction == CommentReactionType.DISLIKE) {
            comment.setDislikeCount(normalize(comment.getDislikeCount()) + 1);
        }

        if (targetReaction == CommentReactionType.NONE) {
            if (reaction != null) {
                reaction.setDeletedYn("Y");
                commentLikeRepository.save(reaction);
            }
        } else {
            if (reaction == null) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
                reaction = CommentLike.builder()
                        .comment(comment)
                        .user(user)
                        .deletedYn("N")
                        .reactionType(targetReaction)
                        .build();
            } else {
                reaction.setDeletedYn("N");
                reaction.setReactionType(targetReaction);
            }
            commentLikeRepository.save(reaction);
        }

        commentRepository.save(comment);
        return toReactionResponse(comment, targetReaction);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public LikeDto.CommentReactionResponse getMyReaction(Long userId, Long commentId) {
        Comment comment = getCommentOrThrow(commentId);
        CommentReactionType myReaction = commentLikeRepository.findByComment_IdAndUser_Id(commentId, userId)
                .map(this::resolveReaction)
                .orElse(CommentReactionType.NONE);
        return toReactionResponse(comment, myReaction);
    }

    private Comment getCommentOrThrow(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));
        if (comment.isDeleted()) {
            throw new IllegalArgumentException("삭제된 댓글에는 반응을 처리할 수 없습니다.");
        }
        return comment;
    }

    private CommentReactionType resolveReaction(CommentLike reaction) {
        if (reaction == null || !"N".equals(reaction.getDeletedYn())) {
            return CommentReactionType.NONE;
        }
        return reaction.getReactionType();
    }

    private LikeDto.CommentReactionResponse toReactionResponse(Comment comment, CommentReactionType myReaction) {
        LikeDto.CommentReactionResponse response = new LikeDto.CommentReactionResponse();
        response.setCommentId(comment.getId());
        response.setMyReaction(myReaction);
        response.setLikeCount(normalize(comment.getLikeCount()));
        response.setDislikeCount(normalize(comment.getDislikeCount()));
        return response;
    }

    private long normalize(Long value) {
        return value == null ? 0L : value;
    }
}
