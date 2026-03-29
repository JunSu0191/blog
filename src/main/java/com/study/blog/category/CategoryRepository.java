package com.study.blog.category;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndDeletedYn(Long id, String deletedYn);

    Optional<Category> findBySlug(String slug);

    Optional<Category> findFirstByNameIgnoreCase(String name);

    List<Category> findByDeletedYnOrderByNameAsc(String deletedYn);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("""
            select c as category, count(distinct p.id) as postCount
            from Category c
            left join Post p on p.category = c
                and p.deletedYn = 'N'
                and p.deletedAt is null
                and p.status = com.study.blog.post.PostStatus.PUBLISHED
                and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
                and p.publishedAt is not null
            where c.deletedYn = 'N'
              and (:keyword is null
                   or lower(c.name) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(c.description, '')) like lower(concat('%', :keyword, '%')))
            group by c
            order by count(distinct p.id) desc, c.name asc
            """)
    List<CategoryWithPostCountProjection> searchHubCategories(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select c as category, count(distinct p.id) as postCount
            from Category c
            left join Post p on p.category = c
                and p.deletedYn = 'N'
                and p.deletedAt is null
                and p.status = com.study.blog.post.PostStatus.PUBLISHED
                and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
                and p.publishedAt is not null
            where c.deletedYn = 'N'
            group by c
            order by c.updatedAt desc, c.id desc
            """)
    List<CategoryWithPostCountProjection> findRecentHubCategories(Pageable pageable);
}
