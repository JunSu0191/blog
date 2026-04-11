package com.study.blog.series;

import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.Post;
import com.study.blog.post.PostErrorCode;
import com.study.blog.post.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class SeriesMembershipService {

    private final PostRepository postRepository;
    private final PostSeriesRepository postSeriesRepository;

    public SeriesMembershipService(PostRepository postRepository, PostSeriesRepository postSeriesRepository) {
        this.postRepository = postRepository;
        this.postSeriesRepository = postSeriesRepository;
    }

    public MembershipAssignment syncPostSeries(Long actorUserId, Post post, Long seriesId, Integer requestedOrder) {
        if (post.getUser() == null || !Objects.equals(post.getUser().getId(), actorUserId)) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "작성자만 시리즈를 변경할 수 있습니다.");
        }

        Long currentSeriesId = post.getSeries() == null ? null : post.getSeries().getId();
        if (seriesId == null) {
            if (currentSeriesId != null) {
                detachInternal(post, LocalDateTime.now());
            }
            return MembershipAssignment.from(post);
        }

        PostSeries targetSeries = postSeriesRepository.findByIdAndOwner_Id(seriesId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("시리즈를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        if (!Objects.equals(currentSeriesId, targetSeries.getId())) {
            if (currentSeriesId != null) {
                detachInternal(post, now);
            }
            attachInternal(post, targetSeries, requestedOrder, now);
            return MembershipAssignment.from(post);
        }

        if ((requestedOrder == null || requestedOrder <= 0) && post.getSeriesOrder() != null) {
            return MembershipAssignment.from(post);
        }
        reorderInternal(post, targetSeries, requestedOrder, now);
        return MembershipAssignment.from(post);
    }

    public MembershipAssignment assignPostToSeries(Long actorUserId, Long seriesId, Long postId, Integer requestedOrder) {
        Post post = getOwnedPostOrThrow(postId, actorUserId);
        return syncPostSeries(actorUserId, post, seriesId, requestedOrder);
    }

    public MembershipAssignment updatePostOrder(Long actorUserId, Long seriesId, Long postId, Integer requestedOrder) {
        if (requestedOrder == null || requestedOrder <= 0) {
            throw new IllegalArgumentException("order는 1 이상의 값이어야 합니다.");
        }
        Post post = getOwnedPostOrThrow(postId, actorUserId);
        if (post.getSeries() == null || !Objects.equals(post.getSeries().getId(), seriesId)) {
            throw new IllegalArgumentException("해당 게시글은 지정한 시리즈에 속해 있지 않습니다.");
        }
        return syncPostSeries(actorUserId, post, seriesId, requestedOrder);
    }

    public void removePostFromSeries(Long actorUserId, Long seriesId, Long postId) {
        Post post = getOwnedPostOrThrow(postId, actorUserId);
        if (post.getSeries() == null || !Objects.equals(post.getSeries().getId(), seriesId)) {
            throw new IllegalArgumentException("해당 게시글은 지정한 시리즈에 속해 있지 않습니다.");
        }
        detachInternal(post, LocalDateTime.now());
    }

    private Post getOwnedPostOrThrow(Long postId, Long actorUserId) {
        Post post = postRepository.findWithAssociationsById(postId)
                .orElseThrow(() -> new CodedApiException(
                        PostErrorCode.POST_NOT_FOUND.code(),
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."));
        if (post.getUser() == null || !Objects.equals(post.getUser().getId(), actorUserId)) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "작성자만 시리즈를 변경할 수 있습니다.");
        }
        return post;
    }

    private void attachInternal(Post post, PostSeries targetSeries, Integer requestedOrder, LocalDateTime now) {
        List<Post> members = new ArrayList<>(postRepository.findActiveBySeriesIdOrderBySeriesOrderAscIdAsc(targetSeries.getId()));
        members.removeIf(candidate -> Objects.equals(candidate.getId(), post.getId()));

        int insertIndex = normalizeInsertIndex(requestedOrder, members.size());
        members.add(insertIndex, post);

        post.setSeries(targetSeries);
        if (post.getSeriesAssignedAt() == null) {
            post.setSeriesAssignedAt(now);
        }
        renumberMembers(members, now);
    }

    private void reorderInternal(Post post, PostSeries series, Integer requestedOrder, LocalDateTime now) {
        List<Post> members = new ArrayList<>(postRepository.findActiveBySeriesIdOrderBySeriesOrderAscIdAsc(series.getId()));
        int currentIndex = findPostIndex(members, post.getId());
        if (currentIndex < 0) {
            attachInternal(post, series, requestedOrder, now);
            return;
        }

        Post moving = members.remove(currentIndex);
        int insertIndex = normalizeInsertIndex(requestedOrder, members.size(), currentIndex);
        members.add(insertIndex, moving);
        renumberMembers(members, now);
    }

    private void detachInternal(Post post, LocalDateTime now) {
        Long seriesId = post.getSeries() == null ? null : post.getSeries().getId();
        if (seriesId == null) {
            return;
        }

        List<Post> remaining = new ArrayList<>(postRepository.findActiveBySeriesIdOrderBySeriesOrderAscIdAsc(seriesId));
        remaining.removeIf(candidate -> Objects.equals(candidate.getId(), post.getId()));

        post.setSeries(null);
        post.setSeriesOrder(null);
        post.setSeriesAssignedAt(null);
        post.setSeriesUpdatedAt(now);

        renumberMembers(remaining, now);
    }

    private void renumberMembers(List<Post> members, LocalDateTime now) {
        for (int index = 0; index < members.size(); index++) {
            Post member = members.get(index);
            member.setSeriesOrder(index + 1);
            if (member.getSeriesAssignedAt() == null) {
                member.setSeriesAssignedAt(now);
            }
            member.setSeriesUpdatedAt(now);
        }
    }

    private int normalizeInsertIndex(Integer requestedOrder, int size) {
        return normalizeInsertIndex(requestedOrder, size, size);
    }

    private int normalizeInsertIndex(Integer requestedOrder, int size, int fallbackIndex) {
        if (requestedOrder == null || requestedOrder <= 0) {
            return Math.min(Math.max(fallbackIndex, 0), size);
        }
        return Math.min(requestedOrder - 1, size);
    }

    private int findPostIndex(List<Post> members, Long postId) {
        for (int index = 0; index < members.size(); index++) {
            if (Objects.equals(members.get(index).getId(), postId)) {
                return index;
            }
        }
        return -1;
    }

    public record MembershipAssignment(
            Long seriesId,
            Long postId,
            Integer order,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static MembershipAssignment from(Post post) {
            return new MembershipAssignment(
                    post.getSeries() == null ? null : post.getSeries().getId(),
                    post.getId(),
                    post.getSeriesOrder(),
                    post.getSeriesAssignedAt(),
                    post.getSeriesUpdatedAt());
        }
    }
}
