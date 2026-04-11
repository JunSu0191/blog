package com.study.blog.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Post 엔티티용 레포지토리입니다.
 */
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    // 특정 사용자(userId)의 삭제되지 않은 게시글을 조회합니다 (속성 경로 user.id 사용)
    List<Post> findByUser_IdAndDeletedYn(Long userId, String deletedYn);

    Long countByUser_IdAndDeletedYn(Long userId, String deletedYn);

    long countByUser_IdAndDeletedYnAndDeletedAtIsNullAndStatusAndPublishedAtIsNotNull(
            Long userId,
            String deletedYn,
            PostStatus status);

    long countByCategory_IdAndDeletedYnAndDeletedAtIsNull(Long categoryId, String deletedYn);

    long countByDeletedAtIsNull();

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

    @Override
    @EntityGraph(attributePaths = { "user", "category", "series" })
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    Optional<Post> findWithAssociationsById(Long id);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    @Query("""
            select p
            from Post p
            where p.series.id = :seriesId
              and p.deletedYn = 'N'
              and p.deletedAt is null
            order by coalesce(p.seriesOrder, 2147483647) asc, p.id asc
            """)
    List<Post> findActiveBySeriesIdOrderBySeriesOrderAscIdAsc(@Param("seriesId") Long seriesId);

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

    @Query("""
            select p
            from Post p
            where (:keyword is null
                   or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or p.content like concat('%', :keyword, '%'))
              and (:deleted is null
                   or (:deleted = true and p.deletedAt is not null)
                   or (:deleted = false and p.deletedAt is null))
            """)
    Page<Post> searchAdminPosts(@Param("keyword") String keyword,
                                @Param("deleted") Boolean deleted,
                                Pageable pageable);

    Optional<Post> findBySlugAndDeletedYn(String slug, String deletedYn);

    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = { "user", "category" })
    @Query("""
            select p
            from Post p
            where p.slug = :slug
              and p.deletedYn = :deletedYn
            """)
    Optional<Post> findDetailBySlug(@Param("slug") String slug, @Param("deletedYn") String deletedYn);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Post p
            set p.viewCount = coalesce(p.viewCount, 0) + 1
            where p.id = :postId
            """)
    int incrementViewCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Post p
            set p.status = com.study.blog.post.PostStatus.PUBLISHED,
                p.publishedAt = coalesce(p.publishedAt, :now),
                p.scheduledAt = null
            where p.status = com.study.blog.post.PostStatus.SCHEDULED
              and p.scheduledAt is not null
              and p.scheduledAt <= :now
              and p.deletedYn = 'N'
              and p.deletedAt is null
            """)
    int publishDueScheduledPosts(@Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = { "user", "category" })
    @Query("""
            select distinct p
            from Post p
            where p.id <> :excludePostId
              and p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = :status
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and (
                    (:categoryId is not null and p.category.id = :categoryId)
                 or (:hasTags = true and exists (
                    select 1
                    from PostTag pt
                    where pt.post.id = p.id
                      and pt.deletedYn = 'N'
                      and pt.tag.deletedYn = 'N'
                      and pt.tag.id in :tagIds
                 ))
              )
            order by p.publishedAt desc, p.id desc
            """)
    List<Post> findRelatedCandidates(@Param("excludePostId") Long excludePostId,
                                     @Param("categoryId") Long categoryId,
                                     @Param("hasTags") boolean hasTags,
                                     @Param("tagIds") Collection<Long> tagIds,
                                     @Param("status") PostStatus status,
                                     Pageable pageable);

    @EntityGraph(attributePaths = { "user", "category" })
    @Query("""
            select p
            from Post p
            where p.id <> :excludePostId
              and p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = :status
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
            order by p.publishedAt desc, p.id desc
            """)
    List<Post> findLatestPublishedExcluding(@Param("excludePostId") Long excludePostId,
                                            @Param("status") PostStatus status,
                                            Pageable pageable);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    @Query("""
            select p
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and (:keyword is null
                   or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(p.subtitle, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(p.excerpt, '')) like lower(concat('%', :keyword, '%'))
                   or p.content like concat('%', :keyword, '%'))
            """)
    Page<Post> searchPublicPosts(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    @Query("""
            select p
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and p.category.id = :categoryId
            """)
    Page<Post> findPublicPostsByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("""
            select count(p.id)
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and p.category.id = :categoryId
            """)
    long countPublicPostsByCategoryId(@Param("categoryId") Long categoryId);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    @Query("""
            select distinct p
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and exists (
                    select 1
                    from PostTag pt
                    where pt.post = p
                      and pt.deletedYn = 'N'
                      and pt.tag.deletedYn = 'N'
                      and pt.tag.id = :tagId
              )
            """)
    Page<Post> findPublicPostsByTagId(@Param("tagId") Long tagId, Pageable pageable);

    @Query("""
            select count(distinct p.id)
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and exists (
                    select 1
                    from PostTag pt
                    where pt.post = p
                      and pt.deletedYn = 'N'
                      and pt.tag.deletedYn = 'N'
                      and pt.tag.id = :tagId
              )
            """)
    long countPublicPostsByTagId(@Param("tagId") Long tagId);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    @Query("""
            select p
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and p.category.id = :categoryId
            order by coalesce(p.viewCount, 0) desc, coalesce(p.likeCount, 0) desc, p.publishedAt desc, p.id desc
            """)
    List<Post> findFeaturedPublicPostsByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    @Query("""
            select distinct p
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and exists (
                    select 1
                    from PostTag pt
                    where pt.post = p
                      and pt.deletedYn = 'N'
                      and pt.tag.deletedYn = 'N'
                      and pt.tag.id = :tagId
              )
            order by coalesce(p.viewCount, 0) desc, coalesce(p.likeCount, 0) desc, p.publishedAt desc, p.id desc
            """)
    List<Post> findFeaturedPublicPostsByTagId(@Param("tagId") Long tagId, Pageable pageable);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    Page<Post> findByUser_IdAndDeletedYnAndDeletedAtIsNullAndStatusIn(Long userId,
                                                                      String deletedYn,
                                                                      Collection<PostStatus> statuses,
                                                                      Pageable pageable);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    @Query("""
            select p
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and p.series.id = :seriesId
            order by coalesce(p.seriesOrder, 2147483647) asc, p.publishedAt asc, p.id asc
            """)
    List<Post> findPublicPostsBySeriesId(@Param("seriesId") Long seriesId);

    @EntityGraph(attributePaths = { "user", "category", "series" })
    @Query("""
            select p
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and p.series.id = :seriesId
            """)
    Page<Post> findPublicPostsPageBySeriesId(@Param("seriesId") Long seriesId, Pageable pageable);

    @Query("""
            select count(p.id)
            from Post p
            where p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
              and p.series.id = :seriesId
            """)
    long countPublicPostsBySeriesId(@Param("seriesId") Long seriesId);

    @Query("""
            select coalesce(max(p.seriesOrder), 0)
            from Post p
            where p.series.id = :seriesId
            """)
    Integer findMaxSeriesOrder(@Param("seriesId") Long seriesId);

    @Query("""
            select p.series.id as seriesId, count(p.id) as postCount
            from Post p
            where p.series.id in :seriesIds
              and p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
            group by p.series.id
            """)
    List<SeriesPostCountProjection> findPublicSeriesPostCounts(@Param("seriesIds") Collection<Long> seriesIds);
}
