package com.study.blog.chat.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    boolean existsByUser_IdAndFriendUser_Id(Long userId, Long friendUserId);

    Optional<Friendship> findByUser_IdAndFriendUser_Id(Long userId, Long friendUserId);

    List<Friendship> findByUser_IdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("""
            delete from Friendship f
            where (f.user.id = :userA and f.friendUser.id = :userB)
               or (f.user.id = :userB and f.friendUser.id = :userA)
            """)
    int deletePair(@Param("userA") Long userA, @Param("userB") Long userB);
}
