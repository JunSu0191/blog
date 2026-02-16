package com.study.blog.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Comment 엔티티용 레포지토리입니다.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 게시글의 삭제되지 않은 댓글 조회 (부모 댓글만)
    List<Comment> findByPost_IdAndParentIsNullAndDeletedYnOrderByCreatedAtDesc(Long postId, String deletedYn);

    // 특정 부모 댓글의 대댓글 조회
    List<Comment> findByParent_IdAndDeletedYnOrderByCreatedAtDesc(Long parentId, String deletedYn);

    // 특정 게시글의 모든 댓글 조회 (페이지네이션)
    Page<Comment> findByPost_IdAndDeletedYn(Long postId, String deletedYn, Pageable pageable);

    // 특정 사용자의 댓글 조회
    List<Comment> findByUser_IdAndDeletedYn(Long userId, String deletedYn);

    List<Comment> findByUser_IdAndDeletedYnOrderByCreatedAtDesc(Long userId, String deletedYn);

    // 삭제되지 않은 댓글 단건 조회
    Optional<Comment> findByIdAndDeletedYn(Long id, String deletedYn);

    // 특정 게시글의 댓글 수 조회
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.deletedYn = :deletedYn")
    Long countByPostIdAndDeletedYn(@Param("postId") Long postId, @Param("deletedYn") String deletedYn);

    Long countByUser_IdAndDeletedYn(Long userId, String deletedYn);

    long countByDeletedAtIsNull();

    @Query("""
            select c
            from Comment c
            where (:keyword is null
                   or c.content like concat('%', :keyword, '%'))
              and (:deleted is null
                   or (:deleted = true and c.deletedAt is not null)
                   or (:deleted = false and c.deletedAt is null))
            """)
    Page<Comment> searchAdminComments(@Param("keyword") String keyword,
                                      @Param("deleted") Boolean deleted,
                                      Pageable pageable);
}
