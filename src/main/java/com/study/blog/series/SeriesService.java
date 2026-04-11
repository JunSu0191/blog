package com.study.blog.series;

import com.study.blog.content.ContentMapperService;
import com.study.blog.content.dto.ContentDto;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.Post;
import com.study.blog.post.PostErrorCode;
import com.study.blog.post.PostRepository;
import com.study.blog.post.ScheduledPostPublicationService;
import com.study.blog.series.dto.SeriesDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SeriesService {

    private static final String SERIES_NOT_FOUND_CODE = "series_not_found";

    private final PostSeriesRepository postSeriesRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ContentMapperService contentMapperService;
    private final SeriesMembershipService seriesMembershipService;
    private final ScheduledPostPublicationService scheduledPostPublicationService;

    public SeriesService(PostSeriesRepository postSeriesRepository,
                         PostRepository postRepository,
                         UserRepository userRepository,
                         ContentMapperService contentMapperService,
                         SeriesMembershipService seriesMembershipService,
                         ScheduledPostPublicationService scheduledPostPublicationService) {
        this.postSeriesRepository = postSeriesRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.contentMapperService = contentMapperService;
        this.seriesMembershipService = seriesMembershipService;
        this.scheduledPostPublicationService = scheduledPostPublicationService;
    }

    public Page<SeriesDto.SummaryResponse> listSeries(Pageable pageable) {
        publishDueScheduledPosts();
        Page<SeriesWithPostCountProjection> page = postSeriesRepository.findPublicSeries(pageable);
        List<SeriesDto.SummaryResponse> content = page.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public SeriesDto.DetailResponse getSeries(Long seriesId) {
        publishDueScheduledPosts();
        PostSeries series = getSeriesOrThrow(seriesId);
        List<Post> posts = postRepository.findPublicPostsBySeriesId(seriesId);
        List<ContentDto.PostCard> postCards = contentMapperService.toPostCards(posts);

        return new SeriesDto.DetailResponse(
                series.getId(),
                series.getTitle(),
                series.getSlug(),
                series.getDescription(),
                series.getCoverImageUrl(),
                series.getOwner().getId(),
                (long) postCards.size(),
                toAuthor(series.getOwner()),
                postCards,
                series.getCreatedAt(),
                series.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public Page<ContentDto.PostCard> listSeriesPosts(Long seriesId, Pageable pageable) {
        publishDueScheduledPosts();
        getSeriesOrThrow(seriesId);
        Pageable normalized = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(
                                Sort.Order.asc("seriesOrder"),
                                Sort.Order.asc("publishedAt"),
                                Sort.Order.asc("id")));
        return contentMapperService.toPostCards(postRepository.findPublicPostsPageBySeriesId(seriesId, normalized));
    }

    public SeriesDto.DetailResponse createSeries(Long ownerUserId, SeriesDto.UpsertRequest request) {
        User owner = getUserOrThrow(ownerUserId);
        String slug = generateUniqueSlug(request.slug(), request.title(), null);

        PostSeries series = PostSeries.builder()
                .owner(owner)
                .title(request.title().trim())
                .slug(slug)
                .description(normalizeNullable(request.description()))
                .coverImageUrl(normalizeNullable(request.coverImageUrl()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        PostSeries saved = postSeriesRepository.save(series);
        return getSeries(saved.getId());
    }

    public SeriesDto.DetailResponse updateSeries(Long seriesId, Long ownerUserId, SeriesDto.UpsertRequest request) {
        PostSeries series = postSeriesRepository.findByIdAndOwner_Id(seriesId, ownerUserId)
                .orElseThrow(() -> new CodedApiException(
                        PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                        HttpStatus.FORBIDDEN,
                        "시리즈를 수정할 권한이 없습니다."));

        series.setTitle(request.title().trim());
        series.setSlug(generateUniqueSlug(request.slug(), request.title(), seriesId));
        series.setDescription(normalizeNullable(request.description()));
        series.setCoverImageUrl(normalizeNullable(request.coverImageUrl()));
        series.setUpdatedAt(LocalDateTime.now());
        postSeriesRepository.save(series);
        return getSeries(seriesId);
    }

    public SeriesDto.AssignmentResponse assignPost(Long actorUserId, Long postId, SeriesDto.AssignPostRequest request) {
        SeriesMembershipService.MembershipAssignment assignment =
                seriesMembershipService.assignPostToSeries(actorUserId, request.seriesId(), postId, request.order());
        return toAssignmentResponse(assignment);
    }

    public PostSeries resolveOwnedSeries(Long ownerUserId, Long seriesId, String seriesTitle) {
        if (seriesId != null) {
            return postSeriesRepository.findByIdAndOwner_Id(seriesId, ownerUserId)
                    .orElseThrow(this::seriesNotFound);
        }

        String normalizedSeriesTitle = normalizeNullable(seriesTitle);
        if (normalizedSeriesTitle == null) {
            return null;
        }

        User owner = getUserOrThrow(ownerUserId);
        PostSeries series = PostSeries.builder()
                .owner(owner)
                .title(normalizedSeriesTitle)
                .slug(generateUniqueSlug(null, normalizedSeriesTitle, null))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return postSeriesRepository.save(series);
    }

    private void publishDueScheduledPosts() {
        scheduledPostPublicationService.publishDueScheduledPosts();
    }

    private PostSeries getSeriesOrThrow(Long seriesId) {
        return postSeriesRepository.findById(seriesId)
                .orElseThrow(this::seriesNotFound);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> "N".equalsIgnoreCase(user.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private SeriesDto.SummaryResponse toSummaryResponse(SeriesWithPostCountProjection projection) {
        PostSeries series = projection.getSeries();
        return new SeriesDto.SummaryResponse(
                series.getId(),
                series.getTitle(),
                series.getSlug(),
                series.getDescription(),
                series.getCoverImageUrl(),
                series.getOwner().getId(),
                projection.getPostCount(),
                toAuthor(series.getOwner()),
                series.getCreatedAt(),
                series.getUpdatedAt());
    }

    private ContentDto.AuthorRef toAuthor(User user) {
        return new ContentDto.AuthorRef(user.getId(), user.getUsername(), user.getName());
    }

    private String generateUniqueSlug(String requestedSlug, String fallbackTitle, Long excludeSeriesId) {
        String base = slugify(normalizeNullable(requestedSlug) == null ? fallbackTitle : requestedSlug);
        if (base.isBlank()) {
            throw new IllegalArgumentException("시리즈 slug를 생성할 수 없습니다.");
        }

        String candidate = base;
        int suffix = 1;
        while (existsSlug(candidate, excludeSeriesId)) {
            suffix++;
            String append = "-" + suffix;
            String prefix = base;
            if (prefix.length() + append.length() > 220) {
                prefix = prefix.substring(0, Math.max(1, 220 - append.length()));
            }
            candidate = prefix + append;
        }
        return candidate;
    }

    private boolean existsSlug(String slug, Long excludeSeriesId) {
        if (excludeSeriesId == null) {
            return postSeriesRepository.existsBySlug(slug);
        }
        return postSeriesRepository.existsBySlugAndIdNot(slug, excludeSeriesId);
    }

    private String slugify(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣\\s-]", " ")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public SeriesDto.AssignmentResponse addPost(Long actorUserId, Long seriesId, SeriesDto.AddPostRequest request) {
        SeriesMembershipService.MembershipAssignment assignment =
                seriesMembershipService.assignPostToSeries(actorUserId, seriesId, request.postId(), request.order());
        return toAssignmentResponse(assignment);
    }

    public SeriesDto.AssignmentResponse updatePostOrder(Long actorUserId,
                                                        Long seriesId,
                                                        Long postId,
                                                        SeriesDto.UpdateSeriesPostRequest request) {
        SeriesMembershipService.MembershipAssignment assignment =
                seriesMembershipService.updatePostOrder(actorUserId, seriesId, postId, request.order());
        return toAssignmentResponse(assignment);
    }

    public void removePost(Long actorUserId, Long seriesId, Long postId) {
        seriesMembershipService.removePostFromSeries(actorUserId, seriesId, postId);
    }

    private SeriesDto.AssignmentResponse toAssignmentResponse(SeriesMembershipService.MembershipAssignment assignment) {
        return new SeriesDto.AssignmentResponse(
                assignment.postId(),
                assignment.seriesId(),
                assignment.order(),
                assignment.createdAt(),
                assignment.updatedAt());
    }

    private CodedApiException seriesNotFound() {
        return new CodedApiException(
                SERIES_NOT_FOUND_CODE,
                HttpStatus.NOT_FOUND,
                "시리즈를 찾을 수 없습니다.");
    }
}
