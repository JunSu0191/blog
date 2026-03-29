package com.study.blog.recommendation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRecommendationRepository extends JpaRepository<AdminRecommendation, Long> {

    @EntityGraph(attributePaths = { "post" })
    List<AdminRecommendation> findAllByOrderBySlotAsc();

    Optional<AdminRecommendation> findBySlot(Integer slot);

    Optional<AdminRecommendation> findByPost_Id(Long postId);
}
