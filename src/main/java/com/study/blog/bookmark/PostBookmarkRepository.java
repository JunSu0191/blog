package com.study.blog.bookmark;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {

    Optional<PostBookmark> findByPost_IdAndUser_Id(Long postId, Long userId);

    @EntityGraph(attributePaths = { "post", "post.user", "post.category", "post.series" })
    @Query("""
            select b
            from PostBookmark b
            where b.user.id = :userId
              and b.deletedYn = 'N'
              and b.post.deletedYn = 'N'
              and b.post.deletedAt is null
              and (
                    (b.post.status = com.study.blog.post.PostStatus.PUBLISHED
                     and b.post.publishedAt is not null
                     and b.post.visibility <> com.study.blog.post.PostVisibility.PRIVATE)
                 or b.post.user.id = :userId
              )
            """)
    Page<PostBookmark> findVisibleBookmarksByUserId(@Param("userId") Long userId, Pageable pageable);
}
