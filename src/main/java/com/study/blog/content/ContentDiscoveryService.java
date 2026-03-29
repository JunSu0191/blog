package com.study.blog.content;

import com.study.blog.category.Category;
import com.study.blog.category.CategoryRepository;
import com.study.blog.category.CategoryWithPostCountProjection;
import com.study.blog.content.dto.ContentDto;
import com.study.blog.core.response.PageResponse;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.post.ScheduledPostPublicationService;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.tag.RelatedTagProjection;
import com.study.blog.tag.Tag;
import com.study.blog.tag.TagRepository;
import com.study.blog.tag.TagWithPostCountProjection;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserWithPostCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContentDiscoveryService {

    private static final String FLAG_NO = "N";
    private static final int DEFAULT_DISCOVERY_SIZE = 10;

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PostTagRepository postTagRepository;
    private final ContentMapperService contentMapperService;
    private final ScheduledPostPublicationService scheduledPostPublicationService;

    public ContentDiscoveryService(PostRepository postRepository,
                                   TagRepository tagRepository,
                                   CategoryRepository categoryRepository,
                                   UserRepository userRepository,
                                   PostTagRepository postTagRepository,
                                   ContentMapperService contentMapperService,
                                   ScheduledPostPublicationService scheduledPostPublicationService) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.postTagRepository = postTagRepository;
        this.contentMapperService = contentMapperService;
        this.scheduledPostPublicationService = scheduledPostPublicationService;
    }

    public List<ContentDto.TagListItem> listTags() {
        publishDueScheduledPosts();
        return tagRepository.searchHubTags(null, PageRequest.of(0, 100))
                .stream()
                .map(this::toTagListItem)
                .toList();
    }

    public ContentDto.TagHubResponse getTagHub(String slug) {
        publishDueScheduledPosts();
        Tag tag = tagRepository.findBySlugAndDeletedYn(slug, FLAG_NO)
                .orElseThrow(() -> new IllegalArgumentException("태그를 찾을 수 없습니다."));

        long postCount = postRepository.countPublicPostsByTagId(tag.getId());
        Post featuredPost = postRepository.findFeaturedPublicPostsByTagId(tag.getId(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .orElse(null);
        List<ContentDto.TagRef> relatedTags = postTagRepository.findRelatedPublicTags(tag.getId(), PageRequest.of(0, 8))
                .stream()
                .map(this::toTagRef)
                .toList();

        return new ContentDto.TagHubResponse(
                tag.getId(),
                tag.getName(),
                tag.getSlug(),
                tag.getDescription(),
                postCount,
                relatedTags,
                contentMapperService.toFeaturedPost(featuredPost));
    }

    public Page<ContentDto.PostCard> listTagPosts(String slug, Pageable pageable) {
        publishDueScheduledPosts();
        Tag tag = tagRepository.findBySlugAndDeletedYn(slug, FLAG_NO)
                .orElseThrow(() -> new IllegalArgumentException("태그를 찾을 수 없습니다."));
        return contentMapperService.toPostCards(postRepository.findPublicPostsByTagId(tag.getId(), normalizePostPageable(pageable)));
    }

    public ContentDto.CategoryHubResponse getCategoryHub(Long categoryId) {
        publishDueScheduledPosts();
        Category category = categoryRepository.findByIdAndDeletedYn(categoryId, FLAG_NO)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        long postCount = postRepository.countPublicPostsByCategoryId(categoryId);
        Post featuredPost = postRepository.findFeaturedPublicPostsByCategoryId(categoryId, PageRequest.of(0, 1)).stream()
                .findFirst()
                .orElse(null);
        List<ContentDto.TagRef> relatedTags = postTagRepository.findTopPublicTagsByCategoryId(categoryId, PageRequest.of(0, 8))
                .stream()
                .map(this::toTagRef)
                .toList();

        return new ContentDto.CategoryHubResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                postCount,
                relatedTags,
                contentMapperService.toFeaturedPost(featuredPost));
    }

    public Page<ContentDto.PostCard> listCategoryPosts(Long categoryId, Pageable pageable) {
        publishDueScheduledPosts();
        if (categoryRepository.findByIdAndDeletedYn(categoryId, FLAG_NO).isEmpty()) {
            throw new IllegalArgumentException("카테고리를 찾을 수 없습니다.");
        }
        return contentMapperService.toPostCards(postRepository.findPublicPostsByCategoryId(categoryId, normalizePostPageable(pageable)));
    }

    public ContentDto.SearchResponse search(String keyword, Pageable pageable) {
        publishDueScheduledPosts();
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<ContentDto.PostCard> posts = contentMapperService.toPostCards(
                postRepository.searchPublicPosts(normalizedKeyword, normalizePostPageable(pageable)));
        return new ContentDto.SearchResponse(
                PageResponse.from(posts),
                tagRepository.searchHubTags(normalizedKeyword, PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(this::toTagListItem)
                        .toList(),
                userRepository.searchPublicAuthors(normalizedKeyword, PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(projection -> toAuthorRef(projection.getUser()))
                        .toList(),
                categoryRepository.searchHubCategories(normalizedKeyword, PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(this::toCategoryListItem)
                        .toList());
    }

    public ContentDto.SearchResponse trending() {
        publishDueScheduledPosts();
        Page<ContentDto.PostCard> posts = contentMapperService.toPostCards(
                postRepository.searchPublicPosts(null, PageRequest.of(
                        0,
                        DEFAULT_DISCOVERY_SIZE,
                        Sort.by(
                                Sort.Order.desc("viewCount"),
                                Sort.Order.desc("likeCount"),
                                Sort.Order.desc("publishedAt"),
                                Sort.Order.desc("id")))));
        return new ContentDto.SearchResponse(
                PageResponse.from(posts),
                tagRepository.searchHubTags(null, PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(this::toTagListItem)
                        .toList(),
                userRepository.searchPublicAuthors(null, PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(projection -> toAuthorRef(projection.getUser()))
                        .toList(),
                categoryRepository.searchHubCategories(null, PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(this::toCategoryListItem)
                        .toList());
    }

    public ContentDto.SearchResponse recent() {
        publishDueScheduledPosts();
        Page<ContentDto.PostCard> posts = contentMapperService.toPostCards(
                postRepository.searchPublicPosts(null, PageRequest.of(
                        0,
                        DEFAULT_DISCOVERY_SIZE,
                        Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")))));
        return new ContentDto.SearchResponse(
                PageResponse.from(posts),
                tagRepository.findRecentHubTags(PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(this::toTagListItem)
                        .toList(),
                userRepository.findRecentActiveUsers(PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(this::toAuthorRef)
                        .toList(),
                categoryRepository.findRecentHubCategories(PageRequest.of(0, DEFAULT_DISCOVERY_SIZE)).stream()
                        .map(this::toCategoryListItem)
                        .toList());
    }

    private Pageable normalizePostPageable(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
    }

    private void publishDueScheduledPosts() {
        scheduledPostPublicationService.publishDueScheduledPosts();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }
        return trimmed;
    }

    private ContentDto.TagRef toTagRef(RelatedTagProjection projection) {
        return new ContentDto.TagRef(
                projection.getId(),
                projection.getName(),
                projection.getSlug());
    }

    private ContentDto.TagListItem toTagListItem(TagWithPostCountProjection projection) {
        Tag tag = projection.getTag();
        return new ContentDto.TagListItem(
                tag.getId(),
                tag.getName(),
                tag.getSlug(),
                tag.getDescription(),
                projection.getPostCount());
    }

    private ContentDto.CategoryListItem toCategoryListItem(CategoryWithPostCountProjection projection) {
        Category category = projection.getCategory();
        return new ContentDto.CategoryListItem(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                projection.getPostCount());
    }

    private ContentDto.AuthorRef toAuthorRef(UserWithPostCountProjection projection) {
        return toAuthorRef(projection.getUser());
    }

    private ContentDto.AuthorRef toAuthorRef(User user) {
        return new ContentDto.AuthorRef(user.getId(), user.getUsername(), user.getName());
    }
}
