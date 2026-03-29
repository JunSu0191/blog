package com.study.blog.recommendation;

import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.Post;
import com.study.blog.post.PostErrorCode;
import com.study.blog.post.PostRepository;
import com.study.blog.post.ScheduledPostPublicationService;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.recommendation.dto.RecommendationDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RecommendationService {

    private final AdminRecommendationRepository adminRecommendationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ScheduledPostPublicationService scheduledPostPublicationService;

    public RecommendationService(AdminRecommendationRepository adminRecommendationRepository,
                                 PostRepository postRepository,
                                 UserRepository userRepository,
                                 ScheduledPostPublicationService scheduledPostPublicationService) {
        this.adminRecommendationRepository = adminRecommendationRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.scheduledPostPublicationService = scheduledPostPublicationService;
    }

    public List<RecommendationDto.Response> listRecommendations() {
        publishDueScheduledPosts();
        return adminRecommendationRepository.findAllByOrderBySlotAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public RecommendationDto.Response upsertRecommendation(String actorUsername, RecommendationDto.UpsertRequest request) {
        publishDueScheduledPosts();
        Post post = getRecommendablePost(request.postId());
        User actor = userRepository.findByUsernameAndDeletedYn(actorUsername, "N").orElse(null);

        adminRecommendationRepository.findByPost_Id(post.getId())
                .filter(existing -> !existing.getSlot().equals(request.slot()))
                .ifPresent(adminRecommendationRepository::delete);

        AdminRecommendation recommendation = adminRecommendationRepository.findBySlot(request.slot())
                .orElseGet(AdminRecommendation::new);
        recommendation.setSlot(request.slot());
        recommendation.setPost(post);
        recommendation.setCreatedBy(actor);
        recommendation.setUpdatedAt(LocalDateTime.now());
        if (recommendation.getCreatedAt() == null) {
            recommendation.setCreatedAt(LocalDateTime.now());
        }
        return toResponse(adminRecommendationRepository.save(recommendation));
    }

    public void deleteRecommendation(Long recommendationId) {
        AdminRecommendation recommendation = adminRecommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("추천 슬롯을 찾을 수 없습니다."));
        adminRecommendationRepository.delete(recommendation);
    }

    private void publishDueScheduledPosts() {
        scheduledPostPublicationService.publishDueScheduledPosts();
    }

    private Post getRecommendablePost(Long postId) {
        Post post = postRepository.findWithAssociationsById(postId)
                .orElseThrow(() -> new CodedApiException(
                        PostErrorCode.POST_NOT_FOUND.code(),
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."));

        if (!post.isPubliclyListed()) {
            throw new IllegalArgumentException("추천 슬롯에는 공개 발행 글만 지정할 수 있습니다.");
        }
        return post;
    }

    private RecommendationDto.Response toResponse(AdminRecommendation recommendation) {
        Post post = recommendation.getPost();
        return new RecommendationDto.Response(
                recommendation.getId(),
                recommendation.getSlot(),
                new RecommendationDto.PostSummary(
                        post.getId(),
                        post.getTitle(),
                        post.getSubtitle(),
                        post.getThumbnailUrl(),
                        post.getSlug()));
    }
}
