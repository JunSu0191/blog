package com.study.blog.tag;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByIdInAndDeletedYn(Collection<Long> ids, String deletedYn);

    List<Tag> findByDeletedYnOrderByNameAsc(String deletedYn);

    Optional<Tag> findByNameIgnoreCase(String name);

    Optional<Tag> findByNameIgnoreCaseAndDeletedYn(String name, String deletedYn);

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findBySlugAndDeletedYn(String slug, String deletedYn);

    @Query("""
            select t as tag, count(distinct p.id) as postCount
            from Tag t
            left join PostTag pt on pt.tag = t and pt.deletedYn = 'N'
            left join pt.post p
            where t.deletedYn = 'N'
              and (:keyword is null
                   or lower(t.name) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(t.description, '')) like lower(concat('%', :keyword, '%')))
              and (p is null or (
                    p.deletedYn = 'N'
                and p.deletedAt is null
                and p.status = com.study.blog.post.PostStatus.PUBLISHED
                and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
                and p.publishedAt is not null
              ))
            group by t
            order by count(distinct p.id) desc, t.name asc
            """)
    List<TagWithPostCountProjection> searchHubTags(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select t as tag, count(distinct p.id) as postCount
            from Tag t
            left join PostTag pt on pt.tag = t and pt.deletedYn = 'N'
            left join pt.post p
            where t.deletedYn = 'N'
              and (p is null or (
                    p.deletedYn = 'N'
                and p.deletedAt is null
                and p.status = com.study.blog.post.PostStatus.PUBLISHED
                and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
                and p.publishedAt is not null
              ))
            group by t
            order by t.updatedAt desc, t.id desc
            """)
    List<TagWithPostCountProjection> findRecentHubTags(Pageable pageable);
}
