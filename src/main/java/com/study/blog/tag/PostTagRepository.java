package com.study.blog.tag;

import com.study.blog.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    @Query("""
            select pt
            from PostTag pt
            join fetch pt.tag t
            where pt.post.id = :postId
              and pt.deletedYn = :deletedYn
              and t.deletedYn = 'N'
            """)
    List<PostTag> findActiveByPostIdWithTag(@Param("postId") Long postId,
                                            @Param("deletedYn") String deletedYn);

    @Query("""
            select pt
            from PostTag pt
            join fetch pt.tag t
            where pt.post.id in :postIds
              and pt.deletedYn = :deletedYn
              and t.deletedYn = 'N'
            """)
    List<PostTag> findActiveByPostIdsWithTag(@Param("postIds") Collection<Long> postIds,
                                             @Param("deletedYn") String deletedYn);

    List<PostTag> findByPost_IdInAndDeletedYn(Collection<Long> postIds, String deletedYn);

    List<PostTag> findByPost_IdAndDeletedYn(Long postId, String deletedYn);

    void deleteByPost(Post post);

    @Query("""
            select other.tag.id as id,
                   other.tag.name as name,
                   other.tag.slug as slug,
                   count(distinct p.id) as postCount
            from PostTag source
            join source.post p
            join PostTag other on other.post = p
            where source.tag.id = :tagId
              and source.deletedYn = 'N'
              and other.deletedYn = 'N'
              and source.tag.deletedYn = 'N'
              and other.tag.deletedYn = 'N'
              and other.tag.id <> :tagId
              and p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
            group by other.tag.id, other.tag.name, other.tag.slug
            order by count(distinct p.id) desc, other.tag.name asc
            """)
    List<RelatedTagProjection> findRelatedPublicTags(@Param("tagId") Long tagId, Pageable pageable);

    @Query("""
            select t.id as id,
                   t.name as name,
                   t.slug as slug,
                   count(distinct p.id) as postCount
            from PostTag pt
            join pt.post p
            join pt.tag t
            where pt.deletedYn = 'N'
              and t.deletedYn = 'N'
              and p.category.id = :categoryId
              and p.deletedYn = 'N'
              and p.deletedAt is null
              and p.status = com.study.blog.post.PostStatus.PUBLISHED
              and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
              and p.publishedAt is not null
            group by t.id, t.name, t.slug
            order by count(distinct p.id) desc, t.name asc
            """)
    List<RelatedTagProjection> findTopPublicTagsByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
}
