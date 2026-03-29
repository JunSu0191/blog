package com.study.blog.series;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostSeriesRepository extends JpaRepository<PostSeries, Long> {

    Optional<PostSeries> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query("""
            select s as series, count(distinct p.id) as postCount
            from PostSeries s
            left join Post p on p.series = s
                and p.deletedYn = 'N'
                and p.deletedAt is null
                and p.status = com.study.blog.post.PostStatus.PUBLISHED
                and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
                and p.publishedAt is not null
            group by s
            having count(distinct p.id) > 0
            order by max(coalesce(p.publishedAt, s.updatedAt)) desc, s.id desc
            """)
    Page<SeriesWithPostCountProjection> findPublicSeries(Pageable pageable);
}
