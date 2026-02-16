package com.study.blog.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            select n from Notification n
            where n.user.id = :userId
              and n.archivedAt is null
              and (:cursorId is null or n.id < :cursorId)
            order by n.createdAt desc, n.id desc
            """)
    List<Notification> findByUserIdWithCursor(@Param("userId") Long userId,
                                              @Param("cursorId") Long cursorId,
                                              Pageable pageable);

    @Modifying
    @Query("""
            update Notification n
            set n.readAt = :now
            where n.user.id = :userId
              and n.readAt is null
              and n.archivedAt is null
            """)
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    long countByArchivedAtIsNull();
}
