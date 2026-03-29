package com.study.blog.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostDraftRepository extends JpaRepository<PostDraft, Long> {

    @EntityGraph(attributePaths = { "author", "category" })
    Optional<PostDraft> findByIdAndAuthor_Id(Long id, Long authorId);

    @EntityGraph(attributePaths = { "author", "category" })
    Page<PostDraft> findByAuthor_Id(Long authorId, Pageable pageable);

    long countByCategory_Id(Long categoryId);
}
