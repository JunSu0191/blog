package com.study.blog.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.study.blog.category.Category;
import com.study.blog.category.CategoryErrorCode;
import com.study.blog.category.CategoryRepository;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.PostContentProcessor.ProcessedContent;
import com.study.blog.post.dto.PostContractDto;
import com.study.blog.tag.PostTag;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserProfile;
import com.study.blog.user.UserProfileRepository;
import com.study.blog.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostApplicationService {

    private static final String FLAG_NO = "N";

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PostContentProcessor postContentProcessor;
    private final PostSlugService postSlugService;
    private final PostTagAssignmentService postTagAssignmentService;
    private final ScheduledPostPublicationService scheduledPostPublicationService;

    public PostApplicationService(PostRepository postRepository,
                                  PostTagRepository postTagRepository,
                                  CategoryRepository categoryRepository,
                                  UserRepository userRepository,
                                  UserProfileRepository userProfileRepository,
                                  PostContentProcessor postContentProcessor,
                                  PostSlugService postSlugService,
                                  PostTagAssignmentService postTagAssignmentService,
                                  ScheduledPostPublicationService scheduledPostPublicationService) {
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.postContentProcessor = postContentProcessor;
        this.postSlugService = postSlugService;
        this.postTagAssignmentService = postTagAssignmentService;
        this.scheduledPostPublicationService = scheduledPostPublicationService;
    }

    public PostContractDto.PostDetailResponse createPost(PostContractDto.PostWriteRequest request, Long authorId) {
        User author = getActiveUserOrThrow(authorId);
        Category category = resolveCategory(request.categoryId());
        ProcessedContent processed = postContentProcessor.process(request.contentJson());

        Post post = Post.builder()
                .user(author)
                .title(request.title().trim())
                .subtitle(normalizeNullable(request.subtitle()))
                .category(category)
                .thumbnailUrl(normalizeNullable(request.thumbnailUrl()))
                .deletedYn(FLAG_NO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(0L)
                .likeCount(0L)
                .build();

        applyContent(post, request.publishNow(), processed);
        post.setSlug(postSlugService.generateUniqueSlug(post.getTitle(), null));

        Post saved = postRepository.save(post);
        postTagAssignmentService.replaceTags(saved, request.tagIds(), request.tags());

        Map<Long, List<PostContractDto.TagSummary>> tagsByPostId = getTagsByPostIds(List.of(saved.getId()));
        return toDetailResponse(saved, tagsByPostId);
    }

    public PostContractDto.PostDetailResponse updatePost(Long postId,
                                                         PostContractDto.PostWriteRequest request,
                                                         Long actorUserId) {
        Post post = getPostOrThrow(postId);
        assertAuthor(post, actorUserId);

        Category category = resolveCategory(request.categoryId());
        ProcessedContent processed = postContentProcessor.process(request.contentJson());

        String normalizedTitle = request.title().trim();
        if (!Objects.equals(post.getTitle(), normalizedTitle)) {
            post.setSlug(postSlugService.generateUniqueSlug(normalizedTitle, post.getId()));
        }

        post.setTitle(normalizedTitle);
        post.setSubtitle(normalizeNullable(request.subtitle()));
        post.setCategory(category);
        post.setThumbnailUrl(normalizeNullable(request.thumbnailUrl()));
        applyContent(post, request.publishNow(), processed);

        Post saved = postRepository.save(post);
        postTagAssignmentService.replaceTags(saved, request.tagIds(), request.tags());

        Map<Long, List<PostContractDto.TagSummary>> tagsByPostId = getTagsByPostIds(List.of(saved.getId()));
        return toDetailResponse(saved, tagsByPostId);
    }

    /**
     * 게시글을 완전히 삭제하는 대신, 삭제 여부를 나타내는 플래그와 삭제 시점을 기록하는 방식으로 구현합니다.
     * @param postId
     * @param actorUserId
     */
    public void deletePost(Long postId, Long actorUserId) {
        Post post = getPostOrThrow(postId);
        assertAuthor(post, actorUserId);

        post.setDeletedYn("Y");
        post.setDeletedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    public Page<PostContractDto.PostListItem> listPublishedPosts(String query,
                                                                 Long categoryId,
                                                                 String tag,
                                                                 String sort,
                                                                 Pageable pageable) {
        publishDueScheduledPosts();
        validateCategoryFilter(categoryId);
        PostSortType sortType = parseSortType(sort);
        Pageable normalizedPageable = applySort(pageable, sortType);

        Specification<Post> spec = Specification.where(publishedSpec())
                .and(keywordSpec(query))
                .and(categorySpec(categoryId))
                .and(tagSpec(tag));

        Page<Post> page = postRepository.findAll(spec, normalizedPageable);
        Map<Long, String> profileImageByUserId = getProfileImageByUserId(page.getContent());
        Map<Long, List<PostContractDto.TagSummary>> tagsByPostId = getTagsByPostIds(extractPostIds(page.getContent()));
        Map<Long, List<String>> imageUrlsByPostId = getImageUrlsByPostId(page.getContent());

        List<PostContractDto.PostListItem> content = page.getContent().stream()
                .map(post -> toListItem(
                        post,
                        tagsByPostId.getOrDefault(post.getId(), List.of()),
                        profileImageByUserId.get(post.getUser().getId()),
                        imageUrlsByPostId.getOrDefault(post.getId(), List.of())))
                .toList();

        return new PageImpl<>(content, normalizedPageable, page.getTotalElements());
    }

    public Page<PostContractDto.PostListItem> listPublishedPostsByAuthor(Long authorId,
                                                                         String query,
                                                                         String sort,
                                                                         Pageable pageable) {
        publishDueScheduledPosts();
        if (authorId == null || authorId <= 0L) {
            throw new IllegalArgumentException("유효하지 않은 작성자 ID입니다.");
        }

        PostSortType sortType = parseSortType(sort);
        Pageable normalizedPageable = applySort(pageable, sortType);

        Specification<Post> spec = Specification.where(publishedSpec())
                .and(authorSpec(authorId))
                .and(keywordSpec(query));

        Page<Post> page = postRepository.findAll(spec, normalizedPageable);
        Map<Long, String> profileImageByUserId = getProfileImageByUserId(page.getContent());
        Map<Long, List<PostContractDto.TagSummary>> tagsByPostId = getTagsByPostIds(extractPostIds(page.getContent()));
        Map<Long, List<String>> imageUrlsByPostId = getImageUrlsByPostId(page.getContent());

        List<PostContractDto.PostListItem> content = page.getContent().stream()
                .map(post -> toListItem(
                        post,
                        tagsByPostId.getOrDefault(post.getId(), List.of()),
                        profileImageByUserId.get(post.getUser().getId()),
                        imageUrlsByPostId.getOrDefault(post.getId(), List.of())))
                .toList();

        return new PageImpl<>(content, normalizedPageable, page.getTotalElements());
    }

    public PostContractDto.PostDetailResponse getPostById(Long postId, Long actorUserId) {
        publishDueScheduledPosts();
        Post post = getPostOrThrow(postId);
        boolean owner = actorUserId != null && Objects.equals(post.getUser().getId(), actorUserId);

        if (!owner && !canAccessPublishedPost(post)) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "해당 게시글에 접근할 권한이 없습니다.");
        }

        if (post.isPublished() && !owner) {
            postRepository.incrementViewCount(post.getId());
            post.setViewCount(post.getViewCount() == null ? 1L : post.getViewCount() + 1L);
        }

        Map<Long, List<PostContractDto.TagSummary>> tagsByPostId = getTagsByPostIds(List.of(post.getId()));
        return toDetailResponse(post, tagsByPostId);
    }

    public List<PostContractDto.RelatedPostResponse> getRelatedPosts(Long postId, int limit) {
        publishDueScheduledPosts();
        int normalizedLimit = Math.min(Math.max(limit, 1), 20);
        Post source = getPostOrThrow(postId);

        if (!canAccessPublishedPost(source)) {
            throw new CodedApiException(
                    PostErrorCode.POST_NOT_FOUND.code(),
                    HttpStatus.NOT_FOUND,
                    "게시글을 찾을 수 없습니다.");
        }

        Map<Long, List<PostContractDto.TagSummary>> sourceTagMap = getTagsByPostIds(List.of(source.getId()));
        Set<Long> sourceTagIds = sourceTagMap.getOrDefault(source.getId(), List.of())
                .stream()
                .map(PostContractDto.TagSummary::id)
                .collect(Collectors.toSet());

        List<Long> relatedTagFilter = sourceTagIds.isEmpty() ? List.of(-1L) : new ArrayList<>(sourceTagIds);
        boolean hasTags = !sourceTagIds.isEmpty();

        List<Post> candidates = new ArrayList<>(postRepository.findRelatedCandidates(
                source.getId(),
                source.getCategory() == null ? null : source.getCategory().getId(),
                hasTags,
                relatedTagFilter,
                PostStatus.PUBLISHED,
                PageRequest.of(0, 50)));

        if (candidates.size() < normalizedLimit) {
            List<Post> fallbacks = postRepository.findLatestPublishedExcluding(
                    source.getId(),
                    PostStatus.PUBLISHED,
                    PageRequest.of(0, normalizedLimit * 3));
            for (Post fallback : fallbacks) {
                if (candidates.stream().noneMatch(post -> Objects.equals(post.getId(), fallback.getId()))) {
                    candidates.add(fallback);
                }
                if (candidates.size() >= normalizedLimit * 3) {
                    break;
                }
            }
        }

        Map<Long, List<PostContractDto.TagSummary>> tagsByPostId = getTagsByPostIds(extractPostIds(candidates));

        List<Post> selected = candidates.stream()
                .map(post -> new ScoredPost(post, computeRelatedScore(source, post, sourceTagIds,
                        tagsByPostId.getOrDefault(post.getId(), List.of()))))
                .sorted(Comparator
                        .comparingDouble(ScoredPost::score).reversed()
                        .thenComparing(item -> item.post().getPublishedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(normalizedLimit)
                .map(ScoredPost::post)
                .toList();

        return selected.stream()
                .map(post -> toRelatedResponse(post, tagsByPostId.getOrDefault(post.getId(), List.of())))
                .toList();
    }

    private Specification<Post> publishedSpec() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("deletedYn"), FLAG_NO),
                criteriaBuilder.isNull(root.get("deletedAt")),
                criteriaBuilder.equal(root.get("status"), PostStatus.PUBLISHED),
                criteriaBuilder.equal(root.get("visibility"), PostVisibility.PUBLIC),
                criteriaBuilder.isNotNull(root.get("publishedAt")));
    }

    private Specification<Post> keywordSpec(String query) {
        String normalized = normalizeNullable(query);
        if (normalized == null) {
            return null;
        }

        String loweredKeyword = "%" + normalized.toLowerCase(Locale.ROOT) + "%";
        String rawKeyword = "%" + normalized + "%";
        return (root, q, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), loweredKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("subtitle")), loweredKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("excerpt")), loweredKeyword),
                criteriaBuilder.like(root.get("content"), rawKeyword));
    }

    private Specification<Post> categorySpec(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    private Specification<Post> authorSpec(Long authorId) {
        if (authorId == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user").get("id"), authorId);
    }

    private void validateCategoryFilter(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (categoryId <= 0L) {
            throw new CodedApiException(
                    CategoryErrorCode.INVALID_CATEGORY_ID.code(),
                    HttpStatus.BAD_REQUEST,
                    "유효하지 않은 카테고리 ID입니다.");
        }
        if (categoryRepository.findByIdAndDeletedYn(categoryId, FLAG_NO).isEmpty()) {
            throw new CodedApiException(
                    CategoryErrorCode.CATEGORY_NOT_FOUND.code(),
                    HttpStatus.NOT_FOUND,
                    "카테고리를 찾을 수 없습니다.");
        }
    }

    private Specification<Post> tagSpec(String tagName) {
        String normalizedTag = normalizeNullable(tagName);
        if (normalizedTag == null) {
            return null;
        }

        String lowered = normalizedTag.toLowerCase(Locale.ROOT);
        return (root, query, criteriaBuilder) -> {
            var subQuery = query.subquery(Long.class);
            var postTagRoot = subQuery.from(PostTag.class);
            var tagJoin = postTagRoot.join("tag");

            subQuery.select(postTagRoot.get("post").get("id"))
                    .where(
                            criteriaBuilder.equal(postTagRoot.get("deletedYn"), FLAG_NO),
                            criteriaBuilder.equal(tagJoin.get("deletedYn"), FLAG_NO),
                            criteriaBuilder.equal(criteriaBuilder.lower(tagJoin.get("name")), lowered));

            return root.get("id").in(subQuery);
        };
    }

    private Pageable applySort(Pageable pageable, PostSortType sortType) {
        Sort resolvedSort;
        switch (sortType) {
            case POPULAR -> resolvedSort = JpaSort.unsafe(
                            Sort.Direction.DESC,
                            "(coalesce(likeCount, 0) * 3 + coalesce(viewCount, 0))")
                    .and(Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
            case VIEWS -> resolvedSort = Sort.by(
                    Sort.Order.desc("viewCount"),
                    Sort.Order.desc("publishedAt"),
                    Sort.Order.desc("id"));
            case LATEST -> resolvedSort = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
            default -> resolvedSort = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolvedSort);
    }

    private PostSortType parseSortType(String rawSort) {
        try {
            return PostSortType.fromNullable(rawSort);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("sort는 latest, popular, views 중 하나여야 합니다.");
        }
    }

    private Map<Long, List<PostContractDto.TagSummary>> getTagsByPostIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        List<PostTag> postTags = postTagRepository.findActiveByPostIdsWithTag(postIds, FLAG_NO);
        Map<Long, List<PostContractDto.TagSummary>> result = new LinkedHashMap<>();

        for (PostTag postTag : postTags) {
            Long postId = postTag.getPost().getId();
            List<PostContractDto.TagSummary> tags = result.computeIfAbsent(postId, key -> new ArrayList<>());
            tags.add(new PostContractDto.TagSummary(
                    postTag.getTag().getId(),
                    postTag.getTag().getName(),
                    postTag.getTag().getSlug()));
        }

        for (Map.Entry<Long, List<PostContractDto.TagSummary>> entry : result.entrySet()) {
            List<PostContractDto.TagSummary> deduplicated = entry.getValue().stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(PostContractDto.TagSummary::id, tag -> tag, (left, right) -> left, LinkedHashMap::new),
                            map -> new ArrayList<>(map.values())));
            entry.setValue(deduplicated);
        }

        return result;
    }

    private List<Long> extractPostIds(Collection<Post> posts) {
        return posts.stream()
                .map(Post::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private void applyContent(Post post, Boolean publishNow, ProcessedContent processedContent) {
        post.setContentJson(processedContent.contentJson());
        post.setContentHtml(processedContent.contentHtml());
        post.setContent(processedContent.plainText());
        post.setExcerpt(processedContent.excerpt());
        post.setReadTimeMinutes(processedContent.readTimeMinutes());
        post.setTocJson(processedContent.tocJson());
        post.setUpdatedAt(LocalDateTime.now());

        boolean shouldPublish = Boolean.TRUE.equals(publishNow);
        post.setStatus(shouldPublish ? PostStatus.PUBLISHED : PostStatus.DRAFT);

        if (shouldPublish) {
            post.setPublishedAt(post.getPublishedAt() == null ? LocalDateTime.now() : post.getPublishedAt());
        } else {
            post.setPublishedAt(null);
        }
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findWithAssociationsById(postId)
                .or(() -> postRepository.findById(postId))
                .filter(post -> FLAG_NO.equalsIgnoreCase(post.getDeletedYn()))
                .orElseThrow(() -> new CodedApiException(
                        PostErrorCode.POST_NOT_FOUND.code(),
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."));
    }

    private boolean canAccessPublishedPost(Post post) {
        return post.getStatus() == PostStatus.PUBLISHED
                && post.getPublishedAt() != null
                && post.getVisibility() != PostVisibility.PRIVATE;
    }

    private void publishDueScheduledPosts() {
        scheduledPostPublicationService.publishDueScheduledPosts();
    }

    private void assertAuthor(Post post, Long actorUserId) {
        if (actorUserId == null || !Objects.equals(post.getUser().getId(), actorUserId)) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "작성자만 게시글을 수정/삭제할 수 있습니다.");
        }
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (categoryId <= 0L) {
            throw new CodedApiException(
                    CategoryErrorCode.INVALID_CATEGORY_ID.code(),
                    HttpStatus.BAD_REQUEST,
                    "유효하지 않은 카테고리 ID입니다.");
        }
        return categoryRepository.findByIdAndDeletedYn(categoryId, FLAG_NO)
                .orElseThrow(() -> new CodedApiException(
                        CategoryErrorCode.CATEGORY_NOT_FOUND.code(),
                        HttpStatus.NOT_FOUND,
                        "카테고리를 찾을 수 없습니다."));
    }

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> FLAG_NO.equalsIgnoreCase(user.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Map<Long, String> getProfileImageByUserId(Collection<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return Map.of();
        }

        List<Long> userIds = posts.stream()
                .map(Post::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> imageByUserId = new HashMap<>();
        List<UserProfile> profiles = userProfileRepository.findByUser_IdIn(userIds);
        if (profiles == null || profiles.isEmpty()) {
            return imageByUserId;
        }

        for (UserProfile userProfile : profiles) {
            if (userProfile.getUser() == null || userProfile.getUser().getId() == null) {
                continue;
            }
            imageByUserId.put(userProfile.getUser().getId(), normalizeNullable(userProfile.getAvatarUrl()));
        }
        return imageByUserId;
    }

    private Map<Long, List<String>> getImageUrlsByPostId(Collection<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> imageUrlsByPostId = new HashMap<>();
        for (Post post : posts) {
            if (post.getId() == null) {
                continue;
            }
            imageUrlsByPostId.put(post.getId(), extractImageUrls(post));
        }
        return imageUrlsByPostId;
    }

    private List<String> extractImageUrls(Post post) {
        LinkedHashSet<String> imageUrls = new LinkedHashSet<>();
        try {
            JsonNode contentJson = postContentProcessor.parseJson(post.getContentJson());
            collectImageUrls(contentJson, imageUrls);
        } catch (Exception ignored) {
            // malformed contentJson인 경우 목록 API는 이미지 없이 계속 반환한다.
        }

        if (imageUrls.isEmpty()) {
            String thumbnailUrl = normalizeNullable(post.getThumbnailUrl());
            if (thumbnailUrl != null) {
                imageUrls.add(thumbnailUrl);
            }
        }
        return List.copyOf(imageUrls);
    }

    private void collectImageUrls(JsonNode node, LinkedHashSet<String> imageUrls) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectImageUrls(child, imageUrls);
            }
            return;
        }

        if (!node.isObject()) {
            return;
        }

        if ("image".equals(node.path("type").asText(""))) {
            String src = normalizeNullable(node.path("attrs").path("src").asText(null));
            if (src != null) {
                imageUrls.add(src);
            }
        }

        node.elements().forEachRemaining(child -> collectImageUrls(child, imageUrls));
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String findProfileImageByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileRepository.findByUser_Id(userId)
                .map(UserProfile::getAvatarUrl)
                .map(this::normalizeNullable)
                .orElse(null);
    }

    private PostContractDto.PostListItem toListItem(Post post,
                                                    List<PostContractDto.TagSummary> tags,
                                                    String profileImageUrl,
                                                    List<String> imageUrls) {
        PostContractDto.AuthorSummary author = new PostContractDto.AuthorSummary(
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getName(),
                post.getUser().getNickname(),
                profileImageUrl);

        return new PostContractDto.PostListItem(
                post.getId(),
                post.getTitle(),
                post.getSubtitle(),
                post.getExcerpt(),
                post.getThumbnailUrl(),
                post.getCategory() == null ? null : post.getCategory().getId(),
                post.getCategory() == null ? null : post.getCategory().getName(),
                toCategorySummary(post.getCategory()),
                post.getUser().getId(),
                post.getUser().getName(),
                tags,
                defaultLong(post.getViewCount()),
                defaultLong(post.getLikeCount()),
                post.getReadTimeMinutes() == null ? 0 : post.getReadTimeMinutes(),
                post.getPublishedAt(),
                author,
                imageUrls == null ? List.of() : imageUrls);
    }

    private PostContractDto.PostDetailResponse toDetailResponse(Post post,
                                                                Map<Long, List<PostContractDto.TagSummary>> tagsByPostId) {
        JsonNode contentJson = postContentProcessor.parseJson(post.getContentJson());
        List<PostContractDto.TocItem> toc = postContentProcessor.parseTocJson(post.getTocJson());
        List<PostContractDto.TagSummary> tags = tagsByPostId.getOrDefault(post.getId(), List.of());
        PostContractDto.AuthorSummary author = new PostContractDto.AuthorSummary(
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getName(),
                post.getUser().getNickname(),
                findProfileImageByUserId(post.getUser().getId()));

        return new PostContractDto.PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSubtitle(),
                post.getExcerpt(),
                post.getThumbnailUrl(),
                post.getCategory() == null ? null : post.getCategory().getId(),
                post.getCategory() == null ? null : post.getCategory().getName(),
                toCategorySummary(post.getCategory()),
                post.getUser().getId(),
                post.getUser().getName(),
                author,
                post.getStatus(),
                contentJson,
                post.getContentHtml(),
                toc,
                tags,
                defaultLong(post.getViewCount()),
                defaultLong(post.getLikeCount()),
                post.getReadTimeMinutes() == null ? 0 : post.getReadTimeMinutes(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private PostContractDto.CategorySummary toCategorySummary(Category category) {
        if (category == null) {
            return null;
        }
        return new PostContractDto.CategorySummary(
                category.getId(),
                category.getName());
    }

    private PostContractDto.RelatedPostResponse toRelatedResponse(Post post, List<PostContractDto.TagSummary> tags) {
        return new PostContractDto.RelatedPostResponse(
                post.getId(),
                post.getTitle(),
                post.getThumbnailUrl(),
                post.getExcerpt(),
                post.getPublishedAt(),
                defaultLong(post.getViewCount()),
                defaultLong(post.getLikeCount()),
                post.getReadTimeMinutes() == null ? 0 : post.getReadTimeMinutes(),
                tags);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private double computeRelatedScore(Post source,
                                       Post candidate,
                                       Set<Long> sourceTagIds,
                                       List<PostContractDto.TagSummary> candidateTags) {
        double score = 0.0;

        Long sourceCategoryId = source.getCategory() == null ? null : source.getCategory().getId();
        Long candidateCategoryId = candidate.getCategory() == null ? null : candidate.getCategory().getId();
        if (sourceCategoryId != null && Objects.equals(sourceCategoryId, candidateCategoryId)) {
            score += 3.0;
        }

        Set<Long> candidateTagIds = candidateTags.stream()
                .map(PostContractDto.TagSummary::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        long overlap = sourceTagIds.stream().filter(candidateTagIds::contains).count();
        score += overlap * 2.0;

        LocalDateTime publishedAt = candidate.getPublishedAt();
        if (publishedAt != null) {
            long days = Math.max(0L, Duration.between(publishedAt, LocalDateTime.now()).toDays());
            double freshness = Math.max(0.0, 1.0 - (days / 30.0));
            score += freshness;
        }

        score += Math.min(2.0, defaultLong(candidate.getViewCount()) / 1000.0);
        return score;
    }

    private record ScoredPost(Post post, double score) {
    }
}
