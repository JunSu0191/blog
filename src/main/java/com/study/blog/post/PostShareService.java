package com.study.blog.post;

import com.study.blog.core.web.PublicUrlBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PostShareService {

    private static final String SITE_NAME = "blog-pause";
    private static final String DEFAULT_OG_IMAGE_PATH = "/og/default-post.png";
    private static final int DESCRIPTION_MAX_LENGTH = 160;

    private final PostRepository postRepository;
    private final ScheduledPostPublicationService scheduledPostPublicationService;
    private final ResourceLoader resourceLoader;
    private final PublicUrlBuilder publicUrlBuilder;
    private final String spaIndexLocation;
    private final String defaultOgImageUrl;

    public PostShareService(PostRepository postRepository,
                            ScheduledPostPublicationService scheduledPostPublicationService,
                            ResourceLoader resourceLoader,
                            PublicUrlBuilder publicUrlBuilder,
                            @Value("${app.share.spa-index-location:}") String spaIndexLocation,
                            @Value("${app.share.default-og-image-url:}") String defaultOgImageUrl) {
        this.postRepository = postRepository;
        this.scheduledPostPublicationService = scheduledPostPublicationService;
        this.resourceLoader = resourceLoader;
        this.publicUrlBuilder = publicUrlBuilder;
        this.spaIndexLocation = spaIndexLocation;
        this.defaultOgImageUrl = defaultOgImageUrl;
    }

    public Optional<String> renderPostHtml(Long postId, HttpServletRequest request) {
        publishDueScheduledPosts();
        return postRepository.findWithAssociationsById(postId)
                .filter(this::isShareablePost)
                .map(post -> buildShareMeta(post, request))
                .map(this::renderHtml);
    }

    ShareMeta buildShareMeta(Post post, HttpServletRequest request) {
        String canonicalUrl = publicUrlBuilder.build(request, "/posts/" + post.getId());
        String description = buildDescription(post);
        String imageUrl = resolveImageUrl(post, request);

        return new ShareMeta(
                post.getTitle() + " | " + SITE_NAME,
                post.getTitle(),
                description,
                imageUrl,
                canonicalUrl
        );
    }

    String buildDescription(Post post) {
        String subtitle = normalize(post.getSubtitle());
        if (subtitle != null) {
            return truncate(subtitle);
        }

        String excerpt = normalize(post.getExcerpt());
        if (excerpt != null) {
            return truncate(excerpt);
        }

        String contentHtml = normalize(post.getContentHtml());
        if (contentHtml != null) {
            String plainText = normalize(Jsoup.parse(contentHtml).text());
            if (plainText != null) {
                return truncate(plainText);
            }
        }

        String content = normalize(post.getContent());
        return content == null ? "" : truncate(content);
    }

    String resolveImageUrl(Post post, HttpServletRequest request) {
        String thumbnailUrl = normalize(post.getThumbnailUrl());
        if (thumbnailUrl != null) {
            return toAbsoluteUrl(thumbnailUrl, request);
        }

        String fallback = normalize(defaultOgImageUrl);
        if (fallback != null) {
            return toAbsoluteUrl(fallback, request);
        }

        return publicUrlBuilder.build(request, DEFAULT_OG_IMAGE_PATH);
    }

    private String toAbsoluteUrl(String value, HttpServletRequest request) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("//")) {
            return request.getScheme() + ":" + value;
        }
        return publicUrlBuilder.build(request, value.startsWith("/") ? value : "/" + value);
    }

    private String renderHtml(ShareMeta meta) {
        Document document = Jsoup.parse(loadSpaIndexHtml());
        document.outputSettings().prettyPrint(false);

        Element head = document.head();
        if (head == null) {
            head = document.prependElement("head");
        }

        head.select("title").remove();
        head.select("meta[name=description]").remove();
        head.select("meta[property=og:type]").remove();
        head.select("meta[property=og:site_name]").remove();
        head.select("meta[property=og:title]").remove();
        head.select("meta[property=og:description]").remove();
        head.select("meta[property=og:image]").remove();
        head.select("meta[property=og:url]").remove();
        head.select("link[rel=canonical]").remove();
        head.select("meta[name=twitter:card]").remove();
        head.select("meta[name=twitter:title]").remove();
        head.select("meta[name=twitter:description]").remove();
        head.select("meta[name=twitter:image]").remove();

        head.prependElement("meta").attr("name", "twitter:image").attr("content", meta.imageUrl());
        head.prependElement("meta").attr("name", "twitter:description").attr("content", meta.description());
        head.prependElement("meta").attr("name", "twitter:title").attr("content", meta.ogTitle());
        head.prependElement("meta").attr("name", "twitter:card").attr("content", "summary_large_image");
        head.prependElement("link").attr("rel", "canonical").attr("href", meta.canonicalUrl());
        head.prependElement("meta").attr("property", "og:url").attr("content", meta.canonicalUrl());
        head.prependElement("meta").attr("property", "og:image").attr("content", meta.imageUrl());
        head.prependElement("meta").attr("property", "og:description").attr("content", meta.description());
        head.prependElement("meta").attr("property", "og:title").attr("content", meta.ogTitle());
        head.prependElement("meta").attr("property", "og:site_name").attr("content", SITE_NAME);
        head.prependElement("meta").attr("property", "og:type").attr("content", "article");
        head.prependElement("meta").attr("name", "description").attr("content", meta.description());
        head.prependElement("title").text(meta.pageTitle());

        return document.outerHtml();
    }

    private String loadSpaIndexHtml() {
        if (!StringUtils.hasText(spaIndexLocation)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "공유 HTML 템플릿 위치가 설정되지 않았습니다.");
        }

        Resource resource = resourceLoader.getResource(spaIndexLocation);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "공유 HTML 템플릿을 찾을 수 없습니다.");
        }

        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "공유 HTML 템플릿을 읽을 수 없습니다.", ex);
        }
    }

    private boolean isShareablePost(Post post) {
        return post != null
                && "N".equalsIgnoreCase(post.getDeletedYn())
                && post.getDeletedAt() == null
                && post.getStatus() == PostStatus.PUBLISHED
                && post.getPublishedAt() != null
                && post.getVisibility() != PostVisibility.PRIVATE;
    }

    private String truncate(String value) {
        if (value.length() <= DESCRIPTION_MAX_LENGTH) {
            return value;
        }

        int boundary = value.lastIndexOf(' ', DESCRIPTION_MAX_LENGTH - 1);
        if (boundary < DESCRIPTION_MAX_LENGTH / 2) {
            boundary = DESCRIPTION_MAX_LENGTH - 1;
        }
        return value.substring(0, boundary).trim() + "…";
    }

    private void publishDueScheduledPosts() {
        scheduledPostPublicationService.publishDueScheduledPosts();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    record ShareMeta(
            String pageTitle,
            String ogTitle,
            String description,
            String imageUrl,
            String canonicalUrl
    ) {
    }
}
