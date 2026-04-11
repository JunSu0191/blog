package com.study.blog.content;

import com.study.blog.content.dto.ContentDto;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.post.SeriesPostCountProjection;
import com.study.blog.tag.PostTag;
import com.study.blog.tag.PostTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ContentMapperService {

    private static final String FLAG_NO = "N";

    private final PostTagRepository postTagRepository;
    private final PostRepository postRepository;

    public ContentMapperService(PostTagRepository postTagRepository, PostRepository postRepository) {
        this.postTagRepository = postTagRepository;
        this.postRepository = postRepository;
    }

    public Page<ContentDto.PostCard> toPostCards(Page<Post> page) {
        List<ContentDto.PostCard> content = toPostCards(page.getContent());
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    public List<ContentDto.PostCard> toPostCards(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ContentDto.TagRef>> tagsByPostId = getTagsByPostIds(posts.stream().map(Post::getId).toList());
        Map<Long, Long> publicSeriesPostCountBySeriesId = getPublicSeriesPostCountBySeriesId(posts);
        return posts.stream()
                .map(post -> toPostCard(post, tagsByPostId.getOrDefault(post.getId(), List.of()), publicSeriesPostCountBySeriesId))
                .toList();
    }

    public ContentDto.PostCard toPostCard(Post post,
                                          List<ContentDto.TagRef> tags,
                                          Map<Long, Long> publicSeriesPostCountBySeriesId) {
        return new ContentDto.PostCard(
                post.getId(),
                post.getTitle(),
                post.getSubtitle(),
                post.getExcerpt(),
                post.getSlug(),
                post.getThumbnailUrl(),
                post.getCategory() == null ? null : new ContentDto.CategoryRef(
                        post.getCategory().getId(),
                        post.getCategory().getName(),
                        post.getCategory().getSlug()),
                tags == null ? List.of() : tags,
                new ContentDto.AuthorRef(
                        post.getUser().getId(),
                        post.getUser().getUsername(),
                        post.getUser().getName()),
                toSeriesRef(post, publicSeriesPostCountBySeriesId),
                post.getPublishedAt(),
                post.getReadTimeMinutes() == null ? 0 : post.getReadTimeMinutes(),
                normalize(post.getViewCount()),
                normalize(post.getLikeCount()));
    }

    public ContentDto.FeaturedPost toFeaturedPost(Post post) {
        if (post == null) {
            return null;
        }
        return new ContentDto.FeaturedPost(
                post.getId(),
                post.getTitle(),
                post.getSubtitle(),
                post.getThumbnailUrl(),
                post.getSlug());
    }

    private Map<Long, List<ContentDto.TagRef>> getTagsByPostIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        List<PostTag> postTags = postTagRepository.findActiveByPostIdsWithTag(postIds, FLAG_NO);
        Map<Long, List<ContentDto.TagRef>> tagsByPostId = new LinkedHashMap<>();

        for (PostTag postTag : postTags) {
            Long postId = postTag.getPost().getId();
            List<ContentDto.TagRef> tags = tagsByPostId.computeIfAbsent(postId, key -> new ArrayList<>());
            ContentDto.TagRef tagRef = new ContentDto.TagRef(
                    postTag.getTag().getId(),
                    postTag.getTag().getName(),
                    postTag.getTag().getSlug());
            if (tags.stream().noneMatch(existing -> Objects.equals(existing.id(), tagRef.id()))) {
                tags.add(tagRef);
            }
        }

        return tagsByPostId;
    }

    private Map<Long, Long> getPublicSeriesPostCountBySeriesId(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return Map.of();
        }

        Set<Long> seriesIds = posts.stream()
                .map(Post::getSeries)
                .filter(Objects::nonNull)
                .map(series -> series.getId())
                .collect(Collectors.toSet());

        if (seriesIds.isEmpty()) {
            return Map.of();
        }

        return postRepository.findPublicSeriesPostCounts(seriesIds).stream()
                .collect(Collectors.toMap(SeriesPostCountProjection::getSeriesId, SeriesPostCountProjection::getPostCount));
    }

    private ContentDto.SeriesRef toSeriesRef(Post post, Map<Long, Long> publicSeriesPostCountBySeriesId) {
        if (post.getSeries() == null) {
            return null;
        }
        return new ContentDto.SeriesRef(
                post.getSeries().getId(),
                post.getSeries().getTitle(),
                post.getSeries().getSlug(),
                post.getSeriesOrder(),
                publicSeriesPostCountBySeriesId.getOrDefault(post.getSeries().getId(), 0L));
    }

    private long normalize(Long value) {
        return value == null ? 0L : value;
    }
}
