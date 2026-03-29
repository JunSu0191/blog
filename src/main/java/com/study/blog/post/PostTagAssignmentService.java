package com.study.blog.post;

import com.study.blog.tag.PostTag;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.tag.Tag;
import com.study.blog.tag.TagRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PostTagAssignmentService {

    private static final String FLAG_NO = "N";
    private static final int MAX_TAG_NAME_LENGTH = 50;
    private static final int MAX_TAG_SLUG_LENGTH = 220;

    private final PostTagRepository postTagRepository;
    private final TagRepository tagRepository;

    public PostTagAssignmentService(PostTagRepository postTagRepository,
                                    TagRepository tagRepository) {
        this.postTagRepository = postTagRepository;
        this.tagRepository = tagRepository;
    }

    public void replaceTags(Post post, List<Long> tagIds, List<String> tagNames) {
        postTagRepository.deleteByPost(post);

        List<Tag> resolvedTags = resolveTags(tagIds, tagNames);
        if (resolvedTags.isEmpty()) {
            return;
        }

        List<PostTag> entries = new ArrayList<>();
        for (Tag tag : resolvedTags) {
            entries.add(PostTag.builder()
                    .post(post)
                    .tag(tag)
                    .deletedYn(FLAG_NO)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        postTagRepository.saveAll(entries);
    }

    private List<Tag> resolveTags(List<Long> tagIds, List<String> tagNames) {
        Map<Long, Tag> resolved = new LinkedHashMap<>();

        for (String tagName : normalizeTagNames(tagNames)) {
            Tag tag = resolveOrCreateTag(tagName);
            resolved.put(tag.getId(), tag);
        }

        for (Tag tag : resolveTagsById(tagIds)) {
            resolved.putIfAbsent(tag.getId(), tag);
        }

        return new ArrayList<>(resolved.values());
    }

    private List<Tag> resolveTagsById(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(tagIds));
        List<Tag> tags = tagRepository.findByIdInAndDeletedYn(uniqueIds, FLAG_NO);
        if (tags.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 태그가 포함되어 있습니다.");
        }

        Map<Long, Tag> tagById = new LinkedHashMap<>();
        for (Tag tag : tags) {
            tagById.put(tag.getId(), tag);
        }

        List<Tag> ordered = new ArrayList<>();
        for (Long tagId : uniqueIds) {
            Tag tag = tagById.get(tagId);
            if (tag != null) {
                ordered.add(tag);
            }
        }
        return ordered;
    }

    private Tag resolveOrCreateTag(String normalizedName) {
        Tag existing = tagRepository.findByNameIgnoreCase(normalizedName).orElse(null);
        if (existing != null) {
            if (!FLAG_NO.equals(existing.getDeletedYn())) {
                existing.setDeletedYn(FLAG_NO);
                existing.setUpdatedAt(LocalDateTime.now());
            }
            if (existing.getSlug() == null || existing.getSlug().isBlank()) {
                existing.setSlug(generateUniqueSlug(normalizedName, existing.getId()));
            }
            return tagRepository.save(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        return tagRepository.save(Tag.builder()
                .name(normalizedName)
                .slug(generateUniqueSlug(normalizedName, null))
                .deletedYn(FLAG_NO)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private List<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            String value = normalizeTagName(tagName);
            if (value != null) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }

    private String normalizeTagName(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_TAG_NAME_LENGTH) {
            throw new IllegalArgumentException("태그 이름은 50자를 초과할 수 없습니다.");
        }
        return normalized;
    }

    private String generateUniqueSlug(String source, Long currentTagId) {
        String base = slugify(source);
        if (base.isBlank()) {
            throw new IllegalArgumentException("유효한 태그 이름이 없습니다.");
        }

        String candidate = base;
        int suffix = 2;
        while (slugExists(candidate, currentTagId)) {
            String suffixToken = "-" + suffix;
            int baseLength = Math.min(base.length(), MAX_TAG_SLUG_LENGTH - suffixToken.length());
            String prefix = base.substring(0, Math.max(baseLength, 1)).replaceAll("-+$", "");
            candidate = prefix + suffixToken;
            suffix++;
        }
        return candidate;
    }

    private boolean slugExists(String slug, Long currentTagId) {
        return tagRepository.findBySlug(slug)
                .filter(tag -> !Objects.equals(tag.getId(), currentTagId))
                .isPresent();
    }

    private String slugify(String source) {
        if (source == null) {
            return "";
        }

        String normalized = Normalizer.normalize(source, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣\\s-]", " ")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (normalized.length() > MAX_TAG_SLUG_LENGTH) {
            normalized = normalized.substring(0, MAX_TAG_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return normalized;
    }
}
