package com.study.blog.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.study.blog.category.Category;
import com.study.blog.category.CategoryRepository;
import com.study.blog.content.dto.ContentDto;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.PostContentProcessor.ProcessedContent;
import com.study.blog.post.dto.PostManagementDto;
import com.study.blog.series.PostSeries;
import com.study.blog.series.PostSeriesRepository;
import com.study.blog.tag.PostTag;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class PostManagementService {

    private static final String FLAG_NO = "N";

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final PostTagRepository postTagRepository;
    private final UserRepository userRepository;
    private final PostContentProcessor postContentProcessor;
    private final PostSlugService postSlugService;
    private final PostTagAssignmentService postTagAssignmentService;
    private final PostSeriesRepository postSeriesRepository;
    private final ScheduledPostPublicationService scheduledPostPublicationService;

    public PostManagementService(PostRepository postRepository,
                                 CategoryRepository categoryRepository,
                                 PostTagRepository postTagRepository,
                                 UserRepository userRepository,
                                 PostContentProcessor postContentProcessor,
                                 PostSlugService postSlugService,
                                 PostTagAssignmentService postTagAssignmentService,
                                 PostSeriesRepository postSeriesRepository,
                                 ScheduledPostPublicationService scheduledPostPublicationService) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.postTagRepository = postTagRepository;
        this.userRepository = userRepository;
        this.postContentProcessor = postContentProcessor;
        this.postSlugService = postSlugService;
        this.postTagAssignmentService = postTagAssignmentService;
        this.postSeriesRepository = postSeriesRepository;
        this.scheduledPostPublicationService = scheduledPostPublicationService;
    }

    public PostManagementDto.PostResponse createPost(Long authorUserId, PostManagementDto.UpsertRequest request) {
        User author = getUserOrThrow(authorUserId);
        ProcessedContent processedContent = postContentProcessor.process(request.contentJson());

        Post post = Post.builder()
                .user(author)
                .deletedYn(FLAG_NO)
                .viewCount(0L)
                .likeCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        applyUpsertRequest(post, request, processedContent, authorUserId, true);
        Post saved = postRepository.save(post);
        postTagAssignmentService.replaceTags(saved, request.tagIds(), request.tags());
        return toPostResponse(saved);
    }

    public PostManagementDto.PostResponse updatePost(Long actorUserId, Long postId, PostManagementDto.UpsertRequest request) {
        Post post = getOwnedPostOrThrow(postId, actorUserId);
        ProcessedContent processedContent = postContentProcessor.process(request.contentJson());
        applyUpsertRequest(post, request, processedContent, actorUserId, false);
        Post saved = postRepository.save(post);
        postTagAssignmentService.replaceTags(saved, request.tagIds(), request.tags());
        return toPostResponse(saved);
    }

    public PostManagementDto.PostResponse publishPost(Long actorUserId, Long postId) {
        Post post = getOwnedPostOrThrow(postId, actorUserId);
        LocalDateTime now = LocalDateTime.now();
        post.setStatus(PostStatus.PUBLISHED);
        post.setPublishedAt(now);
        post.setScheduledAt(null);
        post.setUpdatedAt(now);
        return toPostResponse(postRepository.save(post));
    }

    public PostManagementDto.PostResponse schedulePost(Long actorUserId, Long postId, PostManagementDto.ScheduleRequest request) {
        Post post = getOwnedPostOrThrow(postId, actorUserId);
        LocalDateTime scheduledAt = request.scheduledAt();
        validateScheduledAt(scheduledAt);
        post.setStatus(PostStatus.SCHEDULED);
        post.setScheduledAt(scheduledAt);
        post.setPublishedAt(null);
        post.setUpdatedAt(LocalDateTime.now());
        return toPostResponse(postRepository.save(post));
    }

    public Page<PostManagementDto.PostSummaryResponse> listMyPosts(Long actorUserId, String rawStatus, Pageable pageable) {
        publishDueScheduledPosts();
        List<PostStatus> statuses = parseStatuses(rawStatus);
        Page<Post> page = postRepository.findByUser_IdAndDeletedYnAndDeletedAtIsNullAndStatusIn(
                actorUserId,
                FLAG_NO,
                statuses,
                pageable);

        Map<Long, List<ContentDto.TagRef>> tagsByPostId = getTagsByPostId(page.getContent());
        List<PostManagementDto.PostSummaryResponse> content = page.getContent().stream()
                .map(post -> toPostSummaryResponse(post, tagsByPostId.getOrDefault(post.getId(), List.of())))
                .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PostManagementDto.SlugCheckResponse checkSlug(String slug) {
        String normalized = postSlugService.normalizeSlug(slug);
        boolean available = postSlugService.isAvailable(slug, null);
        return new PostManagementDto.SlugCheckResponse(normalized, available);
    }

    private void applyUpsertRequest(Post post,
                                    PostManagementDto.UpsertRequest request,
                                    ProcessedContent processedContent,
                                    Long actorUserId,
                                    boolean create) {
        post.setTitle(request.title().trim());
        post.setSubtitle(normalizeNullable(request.subtitle()));
        post.setThumbnailUrl(normalizeNullable(request.thumbnailUrl()));
        post.setMetaTitle(normalizeNullable(request.metaTitle()));
        post.setMetaDescription(normalizeNullable(request.metaDescription()));
        post.setCategory(resolveCategory(request.categoryId()));
        post.setVisibility(request.visibility() == null ? PostVisibility.PUBLIC : request.visibility());
        post.setSlug(resolveSlug(request.slug(), request.title(), post.getId()));

        applyContent(post, processedContent);
        applyLifecycle(post, request.status(), request.publishedAt(), request.scheduledAt(), create);
        applySeries(post, actorUserId, request.seriesId(), request.seriesOrder());
        post.setUpdatedAt(LocalDateTime.now());
    }

    private void applyContent(Post post, ProcessedContent processedContent) {
        post.setContentJson(processedContent.contentJson());
        post.setContentHtml(processedContent.contentHtml());
        post.setContent(processedContent.plainText());
        post.setExcerpt(processedContent.excerpt());
        post.setReadTimeMinutes(processedContent.readTimeMinutes());
        post.setTocJson(processedContent.tocJson());
    }

    private void applyLifecycle(Post post,
                                PostStatus requestedStatus,
                                LocalDateTime publishedAt,
                                LocalDateTime scheduledAt,
                                boolean create) {
        PostStatus targetStatus = requestedStatus;
        if (targetStatus == null) {
            targetStatus = create ? PostStatus.DRAFT : post.getStatus();
        }

        if (targetStatus == PostStatus.PUBLISHED) {
            post.setStatus(PostStatus.PUBLISHED);
            post.setPublishedAt(publishedAt == null ? LocalDateTime.now() : publishedAt);
            post.setScheduledAt(null);
            return;
        }

        if (targetStatus == PostStatus.SCHEDULED) {
            validateScheduledAt(scheduledAt);
            post.setStatus(PostStatus.SCHEDULED);
            post.setScheduledAt(scheduledAt);
            post.setPublishedAt(null);
            return;
        }

        post.setStatus(PostStatus.DRAFT);
        post.setScheduledAt(null);
        post.setPublishedAt(null);
    }

    private void applySeries(Post post, Long actorUserId, Long seriesId, Integer seriesOrder) {
        if (seriesId == null) {
            post.setSeries(null);
            post.setSeriesOrder(null);
            return;
        }

        Long previousSeriesId = post.getSeries() == null ? null : post.getSeries().getId();
        Integer previousSeriesOrder = post.getSeriesOrder();
        PostSeries series = postSeriesRepository.findByIdAndOwner_Id(seriesId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("시리즈를 찾을 수 없습니다."));
        post.setSeries(series);
        if (seriesOrder == null || seriesOrder <= 0) {
            Integer currentMax = postRepository.findMaxSeriesOrder(series.getId());
            if (Objects.equals(previousSeriesId, series.getId()) && previousSeriesOrder != null) {
                post.setSeriesOrder(previousSeriesOrder);
            } else {
                post.setSeriesOrder((currentMax == null ? 0 : currentMax) + 1);
            }
        } else {
            post.setSeriesOrder(seriesOrder);
        }
    }

    private Map<Long, List<ContentDto.TagRef>> getTagsByPostId(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return Map.of();
        }
        List<PostTag> postTags = postTagRepository.findActiveByPostIdsWithTag(posts.stream().map(Post::getId).toList(), FLAG_NO);
        Map<Long, List<ContentDto.TagRef>> result = new LinkedHashMap<>();
        for (PostTag postTag : postTags) {
            result.computeIfAbsent(postTag.getPost().getId(), key -> new ArrayList<>())
                    .add(new ContentDto.TagRef(
                            postTag.getTag().getId(),
                            postTag.getTag().getName(),
                            postTag.getTag().getSlug()));
        }
        return result;
    }

    private PostManagementDto.PostResponse toPostResponse(Post post) {
        Map<Long, List<ContentDto.TagRef>> tagsByPostId = getTagsByPostId(List.of(post));
        JsonNode contentJson = postContentProcessor.parseJson(post.getContentJson());
        return new PostManagementDto.PostResponse(
                post.getId(),
                post.getTitle(),
                post.getSubtitle(),
                post.getExcerpt(),
                post.getSlug(),
                post.getMetaTitle(),
                post.getMetaDescription(),
                post.getThumbnailUrl(),
                post.getStatus(),
                post.getVisibility(),
                post.getPublishedAt(),
                post.getScheduledAt(),
                post.getCategory() == null ? null : new ContentDto.CategoryRef(
                        post.getCategory().getId(),
                        post.getCategory().getName(),
                        post.getCategory().getSlug()),
                tagsByPostId.getOrDefault(post.getId(), List.of()),
                new ContentDto.AuthorRef(
                        post.getUser().getId(),
                        post.getUser().getUsername(),
                        post.getUser().getName()),
                post.getSeries() == null ? null : new PostManagementDto.SeriesRef(
                        post.getSeries().getId(),
                        post.getSeries().getTitle(),
                        post.getSeries().getSlug(),
                        post.getSeriesOrder()),
                contentJson,
                post.getContentHtml(),
                post.getReadTimeMinutes() == null ? 0 : post.getReadTimeMinutes(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private PostManagementDto.PostSummaryResponse toPostSummaryResponse(Post post, List<ContentDto.TagRef> tags) {
        return new PostManagementDto.PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getSubtitle(),
                post.getSlug(),
                post.getThumbnailUrl(),
                post.getStatus(),
                post.getVisibility(),
                post.getPublishedAt(),
                post.getScheduledAt(),
                post.getCategory() == null ? null : new ContentDto.CategoryRef(
                        post.getCategory().getId(),
                        post.getCategory().getName(),
                        post.getCategory().getSlug()),
                tags,
                post.getSeries() == null ? null : new PostManagementDto.SeriesRef(
                        post.getSeries().getId(),
                        post.getSeries().getTitle(),
                        post.getSeries().getSlug(),
                        post.getSeriesOrder()),
                post.getUpdatedAt());
    }

    private String resolveSlug(String requestedSlug, String title, Long excludePostId) {
        String source = normalizeNullable(requestedSlug);
        if (source == null) {
            return postSlugService.generateUniqueSlug(title, excludePostId);
        }
        return postSlugService.generateUniqueSlugFromSource(source, excludePostId);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdAndDeletedYn(categoryId, FLAG_NO)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
    }

    private Post getOwnedPostOrThrow(Long postId, Long actorUserId) {
        publishDueScheduledPosts();
        Post post = postRepository.findWithAssociationsById(postId)
                .orElseThrow(() -> new CodedApiException(
                        PostErrorCode.POST_NOT_FOUND.code(),
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."));
        if (post.getUser() == null || !Objects.equals(post.getUser().getId(), actorUserId)) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "작성자만 게시글을 관리할 수 있습니다.");
        }
        return post;
    }

    private void publishDueScheduledPosts() {
        scheduledPostPublicationService.publishDueScheduledPosts();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> FLAG_NO.equalsIgnoreCase(user.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private List<PostStatus> parseStatuses(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return List.of(PostStatus.DRAFT, PostStatus.PUBLISHED, PostStatus.SCHEDULED);
        }
        String normalized = rawStatus.trim().toLowerCase();
        return switch (normalized) {
            case "draft" -> List.of(PostStatus.DRAFT);
            case "published" -> List.of(PostStatus.PUBLISHED);
            case "scheduled" -> List.of(PostStatus.SCHEDULED);
            default -> throw new IllegalArgumentException("status는 draft, published, scheduled 중 하나여야 합니다.");
        };
    }

    private void validateScheduledAt(LocalDateTime scheduledAt) {
        if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("예약 발행 시간은 현재 시각 이후여야 합니다.");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
