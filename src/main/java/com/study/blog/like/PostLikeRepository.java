package com.study.blog.like;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPost_IdAndUser_Id(Long postId, Long userId);

    Long countByUser_IdAndDeletedYn(Long userId, String deletedYn);
}
