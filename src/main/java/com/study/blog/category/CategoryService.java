package com.study.blog.category;

import com.study.blog.category.dto.CategoryDto;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.PostDraftRepository;
import com.study.blog.post.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class CategoryService {

    private static final String FLAG_NO = "N";
    private static final String FLAG_Y = "Y";
    private static final int MAX_SLUG_LENGTH = 220;

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final PostDraftRepository postDraftRepository;

    public CategoryService(CategoryRepository categoryRepository, PostRepository postRepository, PostDraftRepository postDraftRepository) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.postDraftRepository = postDraftRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto.CategorySummary> listCategories() {
        return categoryRepository.findByDeletedYnOrderByNameAsc(FLAG_NO).stream().map(this::toSummary).toList();
    }

    public CategoryDto.CategorySummary createCategory(CategoryDto.UpsertRequest request) {
        String normalizedName = normalizeName(request.name());
        String normalizedSlug = resolveSlug(request.slug(), normalizedName);

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CodedApiException(CategoryErrorCode.CATEGORY_DUPLICATE_NAME.code(), HttpStatus.CONFLICT, "이미 사용 중인 카테고리 이름입니다.");
        }
        if (categoryRepository.existsBySlug(normalizedSlug)) {
            throw new CodedApiException(CategoryErrorCode.CATEGORY_DUPLICATE_SLUG.code(), HttpStatus.CONFLICT, "이미 사용 중인 카테고리 slug입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        Category category = Category.builder().name(normalizedName).slug(normalizedSlug).deletedYn(FLAG_NO).createdAt(now).updatedAt(now).build();
        return toSummary(categoryRepository.save(category));
    }

    public CategoryDto.CategorySummary updateCategory(Long categoryId, CategoryDto.UpsertRequest request) {
        Category category = getActiveCategoryOrThrow(categoryId);
        String normalizedName = normalizeName(request.name());
        String normalizedSlug = resolveSlug(request.slug(), normalizedName);

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, categoryId)) {
            throw new CodedApiException(CategoryErrorCode.CATEGORY_DUPLICATE_NAME.code(), HttpStatus.CONFLICT, "이미 사용 중인 카테고리 이름입니다.");
        }
        if (categoryRepository.existsBySlugAndIdNot(normalizedSlug, categoryId)) {
            throw new CodedApiException(CategoryErrorCode.CATEGORY_DUPLICATE_SLUG.code(), HttpStatus.CONFLICT, "이미 사용 중인 카테고리 slug입니다.");
        }

        category.setName(normalizedName);
        category.setSlug(normalizedSlug);
        category.setUpdatedAt(LocalDateTime.now());
        return toSummary(categoryRepository.save(category));
    }

    public void deleteCategory(Long categoryId) {
        Category category = getActiveCategoryOrThrow(categoryId);

        long postCount = postRepository.countByCategory_IdAndDeletedYnAndDeletedAtIsNull(categoryId, FLAG_NO);
        long draftCount = postDraftRepository.countByCategory_Id(categoryId);
        if (postCount > 0 || draftCount > 0) {
            throw new CodedApiException(CategoryErrorCode.CATEGORY_IN_USE.code(), HttpStatus.CONFLICT, "사용 중인 카테고리는 삭제할 수 없습니다.");
        }

        category.setDeletedYn(FLAG_Y);
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Category getActiveCategoryOrThrow(Long categoryId) {
        validateCategoryId(categoryId);
        return categoryRepository.findByIdAndDeletedYn(categoryId, FLAG_NO).orElseThrow(() -> new CodedApiException(CategoryErrorCode.CATEGORY_NOT_FOUND.code(), HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
    }

    public void validateCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0L) {
            throw new CodedApiException(CategoryErrorCode.INVALID_CATEGORY_ID.code(), HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리 ID입니다.");
        }
    }

    private CategoryDto.CategorySummary toSummary(Category category) {
        return new CategoryDto.CategorySummary(category.getId(), category.getName(), category.getSlug());
    }

    private String normalizeName(String name) {
        String normalized = normalizeNullable(name);
        if (normalized == null) {
            throw new IllegalArgumentException("카테고리 이름은 필수입니다.");
        }
        return normalized;
    }

    private String resolveSlug(String requestedSlug, String fallbackName) {
        String base = normalizeNullable(requestedSlug);
        String source = base == null ? fallbackName : base;

        String slug = slugify(source);
        if (slug.isBlank()) {
            throw new IllegalArgumentException("카테고리 slug를 생성할 수 없습니다.");
        }
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }

    private String slugify(String source) {
        if (source == null) {
            return "";
        }
        return Normalizer.normalize(source, Normalizer.Form.NFKD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣\\s-]", " ").trim().replaceAll("\\s+", "-").replaceAll("-+", "-");
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

