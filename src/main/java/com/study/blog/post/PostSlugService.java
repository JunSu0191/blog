package com.study.blog.post;

import com.study.blog.core.exception.CodedApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class PostSlugService {

    private static final int SLUG_MAX_LENGTH = 220;

    private final PostRepository postRepository;

    public PostSlugService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public String generateUniqueSlug(String title, Long excludePostId) {
        String base = slugify(title);
        return generateUniqueSlugFromBase(base, excludePostId);
    }

    public String generateUniqueSlugFromSource(String source, Long excludePostId) {
        String base = slugify(source);
        return generateUniqueSlugFromBase(base, excludePostId);
    }

    public String normalizeSlug(String source) {
        return slugify(source);
    }

    public boolean isAvailable(String source, Long excludePostId) {
        String normalized = slugify(source);
        if (normalized.isBlank()) {
            return false;
        }
        return !exists(normalized, excludePostId);
    }

    private String generateUniqueSlugFromBase(String base, Long excludePostId) {
        if (base.isBlank()) {
            throw new CodedApiException(
                    PostErrorCode.SLUG_CONFLICT.code(),
                    HttpStatus.CONFLICT,
                    "slug를 생성할 수 없습니다. 제목을 확인해주세요.");
        }

        String candidate = base;
        int suffix = 1;

        while (exists(candidate, excludePostId)) {
            suffix++;
            String append = "-" + suffix;
            String prefix = base;
            if (prefix.length() + append.length() > SLUG_MAX_LENGTH) {
                prefix = prefix.substring(0, Math.max(1, SLUG_MAX_LENGTH - append.length()));
            }
            candidate = prefix + append;

            if (suffix > 5000) {
                throw new CodedApiException(
                        PostErrorCode.SLUG_CONFLICT.code(),
                        HttpStatus.CONFLICT,
                        "slug 충돌이 반복되어 게시글을 저장할 수 없습니다.");
            }
        }

        return candidate;
    }

    private boolean exists(String slug, Long excludePostId) {
        if (excludePostId == null) {
            return postRepository.existsBySlug(slug);
        }
        return postRepository.existsBySlugAndIdNot(slug, excludePostId);
    }

    private String slugify(String title) {
        if (title == null) {
            return "";
        }

        String normalized = Normalizer.normalize(title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣\\s-]", " ")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (normalized.length() > SLUG_MAX_LENGTH) {
            normalized = normalized.substring(0, SLUG_MAX_LENGTH);
            normalized = normalized.replaceAll("-+$", "");
        }

        return normalized;
    }
}
