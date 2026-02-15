package com.study.blog.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Post 엔티티용 레포지토리입니다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {
    // 특정 사용자(userId)의 삭제되지 않은 게시글을 조회합니다 (속성 경로 user.id 사용)
    List<Post> findByUser_IdAndDeletedYn(Long userId, String deletedYn);

    Long countByUser_IdAndDeletedYn(Long userId, String deletedYn);

    // (deletedYn = ? AND title LIKE ?) OR (deletedYn = ? AND content LIKE ?)
    // 형태로 생성되어 삭제 여부 조건이 제목/내용 검색 모두에 적용된다.
    Page<Post> findByDeletedYnAndTitleContainingIgnoreCaseOrDeletedYnAndContentContainingIgnoreCase(
            String titleDeletedYn,
            String titleKeyword,
            String contentDeletedYn,
            String contentKeyword,
            Pageable pageable);

    // 키워드가 없을 때는 간단한 조회
    Page<Post> findByDeletedYn(String deletedYn, Pageable pageable);

    @Query("""
            select p
            from Post p
            where p.deletedYn = :deletedYn
              and (:cursorId is null or p.id < :cursorId)
            order by p.id desc
            """)
    List<Post> findCursorPageWithoutKeyword(@Param("deletedYn") String deletedYn,
                                            @Param("cursorId") Long cursorId,
                                            Pageable pageable);

    @Query("""
            select p
            from Post p
            where p.deletedYn = :deletedYn
              and (:cursorId is null or p.id < :cursorId)
              and (
                    p.title like concat('%', :keyword, '%')
                 or p.content like concat('%', :keyword, '%')
              )
            order by p.id desc
            """)
    List<Post> findCursorPageWithKeyword(@Param("deletedYn") String deletedYn,
                                         @Param("keyword") String keyword,
                                         @Param("cursorId") Long cursorId,
                                         Pageable pageable);
}
