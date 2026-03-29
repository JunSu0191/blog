package com.study.blog.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.study.blog.category.Category;
import com.study.blog.category.CategoryErrorCode;
import com.study.blog.category.CategoryRepository;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.PostContentProcessor.ProcessedContent;
import com.study.blog.post.dto.PostContractDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class PostDraftService {

    private static final String FLAG_NO = "N";

    private final PostDraftRepository postDraftRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PostContentProcessor postContentProcessor;

    public PostDraftService(PostDraftRepository postDraftRepository,
                            CategoryRepository categoryRepository,
                            UserRepository userRepository,
                            PostContentProcessor postContentProcessor) {
        this.postDraftRepository = postDraftRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.postContentProcessor = postContentProcessor;
    }

    public PostContractDto.DraftResponse createDraft(PostContractDto.DraftWriteRequest request, Long authorId) {
        User author = getActiveUserOrThrow(authorId);
        Category category = resolveCategory(request.categoryId());
        ProcessedContent processed = postContentProcessor.process(request.contentJson());

        PostDraft draft = PostDraft.builder()
                .author(author)
                .category(category)
                .title(request.title().trim())
                .subtitle(normalizeNullable(request.subtitle()))
                .thumbnailUrl(normalizeNullable(request.thumbnailUrl()))
                .contentJson(processed.contentJson())
                .contentHtml(processed.contentHtml())
                .autosavedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        PostDraft saved = postDraftRepository.save(draft);
        return toResponse(saved);
    }

    public PostContractDto.DraftResponse updateDraft(Long draftId,
                                                     PostContractDto.DraftWriteRequest request,
                                                     Long actorUserId) {
        PostDraft draft = getDraftOrThrow(draftId);
        assertAuthor(draft, actorUserId);

        Category category = resolveCategory(request.categoryId());
        ProcessedContent processed = postContentProcessor.process(request.contentJson());

        draft.setCategory(category);
        draft.setTitle(request.title().trim());
        draft.setSubtitle(normalizeNullable(request.subtitle()));
        draft.setThumbnailUrl(normalizeNullable(request.thumbnailUrl()));
        draft.setContentJson(processed.contentJson());
        draft.setContentHtml(processed.contentHtml());
        draft.setAutosavedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());

        PostDraft saved = postDraftRepository.save(draft);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PostContractDto.DraftResponse getDraft(Long draftId, Long actorUserId) {
        PostDraft draft = getDraftOrThrow(draftId);
        assertAuthor(draft, actorUserId);
        return toResponse(draft);
    }

    @Transactional(readOnly = true)
    public Page<PostContractDto.DraftResponse> listDrafts(Long actorUserId, Pageable pageable) {
        return postDraftRepository.findByAuthor_Id(actorUserId, pageable)
                .map(this::toResponse);
    }

    public void deleteDraft(Long draftId, Long actorUserId) {
        PostDraft draft = getDraftOrThrow(draftId);
        assertAuthor(draft, actorUserId);
        postDraftRepository.delete(draft);
    }

    private PostDraft getDraftOrThrow(Long draftId) {
        return postDraftRepository.findById(draftId)
                .orElseThrow(() -> new CodedApiException(
                        PostErrorCode.POST_NOT_FOUND.code(),
                        HttpStatus.NOT_FOUND,
                        "초안을 찾을 수 없습니다."));
    }

    private void assertAuthor(PostDraft draft, Long actorUserId) {
        if (actorUserId == null || !draft.getAuthor().getId().equals(actorUserId)) {
            throw new CodedApiException(
                    PostErrorCode.UNAUTHORIZED_POST_ACCESS.code(),
                    HttpStatus.FORBIDDEN,
                    "초안 접근 권한이 없습니다.");
        }
    }

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> FLAG_NO.equalsIgnoreCase(user.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
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

    private PostContractDto.DraftResponse toResponse(PostDraft draft) {
        JsonNode contentJson = postContentProcessor.parseJson(draft.getContentJson());
        return new PostContractDto.DraftResponse(
                draft.getId(),
                draft.getAuthor().getId(),
                draft.getTitle(),
                draft.getSubtitle(),
                draft.getCategory() == null ? null : draft.getCategory().getId(),
                toCategorySummary(draft.getCategory()),
                draft.getThumbnailUrl(),
                contentJson,
                draft.getContentHtml(),
                draft.getAutosavedAt(),
                draft.getUpdatedAt(),
                draft.getCreatedAt());
    }

    private PostContractDto.CategorySummary toCategorySummary(Category category) {
        if (category == null) {
            return null;
        }
        return new PostContractDto.CategorySummary(
                category.getId(),
                category.getName());
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
