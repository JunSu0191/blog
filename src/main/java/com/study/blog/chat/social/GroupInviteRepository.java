package com.study.blog.chat.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

    Optional<GroupInvite> findTopByGroupThread_IdAndInvitee_IdAndStatusOrderByCreatedAtDesc(
            Long groupThreadId,
            Long inviteeId,
            GroupInviteStatus status);

    List<GroupInvite> findByInvitee_IdAndStatusOrderByCreatedAtDesc(Long inviteeId, GroupInviteStatus status);

    @Modifying
    @Query("""
            update GroupInvite gi
            set gi.status = :expiredStatus, gi.respondedAt = :now
            where gi.status = :pendingStatus
              and gi.expiresAt < :now
            """)
    int markExpiredInvites(@Param("now") LocalDateTime now,
                           @Param("pendingStatus") GroupInviteStatus pendingStatus,
                           @Param("expiredStatus") GroupInviteStatus expiredStatus);
}
