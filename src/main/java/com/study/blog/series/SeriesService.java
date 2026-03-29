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
import org.springframework.data.domain.Pageable;
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

    private final PostSeriesRepository postSeriesRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ContentMapperService contentMapperService;
    private final ScheduledPostPublicationService scheduledPostPublicationService;

    public SeriesService(PostSeriesRepository postSeriesRepository,
                         PostRepository postRepository,
                         UserRepository userRepository,
                         ContentMapperService contentMapperService,
                         ScheduledPostPublicationService scheduledPostPublicationService) {
        this.postSeriesRepository = postSeriesRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.contentMapperService = contentMapperService;
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
                (long) postCards.size(),
                toAuthor(series.getOwner()),
                postCards,
                series.getCreatedAt(),
                series.getUpdatedAt());
    }

    public SeriesDto.DetailResponse createSeries(Long ownerUserId, SeriesDto.UpsertRequest request) {
        User owner = getUserOrThrow(ownerUserId);
        String slug = generateUniqueSlug(request.slug(), request.title(), null);

        PostSeries series = PostSeries.builder()
                .owner(owner)
                .title(request.title().trim())
                .slug(slug)
                .description(normalizeNullable(request.description()))
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
        series.setUpdatedAt(LocalDateTime.now());
        postSeriesRepository.save(series);
        return getSeries(seriesId);
    }

    public SeriesDto.AssignmentResponse assignPost(Long actorUserId, Long postId, SeriesDto.AssignPostRequest request) {
        Post post = postRepository.findWithAssociationsById(postId)
                .orElseThrow(() -> new CodedApiException(
                        PostErrorCode.POST_NOT_FOUND.code(),
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."));
        if (!post.getUser().getId().equals(actorUserId)) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "작성자만 시리즈를 변경할 수 있습니다.");
        }

        PostSeries series = postSeriesRepository.findByIdAndOwner_Id(request.seriesId(), actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("시리즈를 찾을 수 없습니다."));
        Integer order = request.order();
        if (order == null || order <= 0) {
            Integer maxOrder = postRepository.findMaxSeriesOrder(series.getId());
            order = (maxOrder == null ? 0 : maxOrder) + 1;
        }

        post.setSeries(series);
        post.setSeriesOrder(order);
        postRepository.save(post);
        return new SeriesDto.AssignmentResponse(post.getId(), series.getId(), order);
    }

    private void publishDueScheduledPosts() {
        scheduledPostPublicationService.publishDueScheduledPosts();
    }

    private PostSeries getSeriesOrThrow(Long seriesId) {
        return postSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new IllegalArgumentException("시리즈를 찾을 수 없습니다."));
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
                projection.getPostCount(),
                toAuthor(series.getOwner()),
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
}
