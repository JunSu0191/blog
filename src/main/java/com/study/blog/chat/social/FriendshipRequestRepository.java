package com.study.blog.chat.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRequestRepository extends JpaRepository<FriendshipRequest, Long> {

    Optional<FriendshipRequest> findTopByRequester_IdAndTarget_IdAndStatusOrderByCreatedAtDesc(
            Long requesterId,
            Long targetId,
            FriendshipRequestStatus status);

    @Query("""
            select fr
            from FriendshipRequest fr
            where fr.target.id = :userId
              and fr.status = :status
            order by fr.createdAt desc, fr.id desc
            """)
    List<FriendshipRequest> findReceivedByUserAndStatus(@Param("userId") Long userId,
                                                        @Param("status") FriendshipRequestStatus status);

    @Query("""
            select fr
            from FriendshipRequest fr
            where fr.requester.id = :userId
              and fr.status = :status
            order by fr.createdAt desc, fr.id desc
            """)
    List<FriendshipRequest> findSentByUserAndStatus(@Param("userId") Long userId,
                                                    @Param("status") FriendshipRequestStatus status);

    @Query("""
            select case when count(fr) > 0 then true else false end
            from FriendshipRequest fr
            where fr.status = :blockedStatus
              and ((fr.requester.id = :userA and fr.target.id = :userB)
                   or (fr.requester.id = :userB and fr.target.id = :userA))
            """)
    boolean existsBlockedRelation(@Param("userA") Long userA,
                                  @Param("userB") Long userB,
                                  @Param("blockedStatus") FriendshipRequestStatus blockedStatus);

    @Query("""
            select fr
            from FriendshipRequest fr
            where fr.status = :pendingStatus
              and ((fr.requester.id = :userA and fr.target.id = :userB)
                   or (fr.requester.id = :userB and fr.target.id = :userA))
            order by fr.createdAt desc, fr.id desc
            """)
    List<FriendshipRequest> findPendingBetween(@Param("userA") Long userA,
                                               @Param("userB") Long userB,
                                               @Param("pendingStatus") FriendshipRequestStatus pendingStatus);

    @Modifying
    @Query("""
            update FriendshipRequest fr
            set fr.status = :newStatus
            where fr.status = :fromStatus
              and ((fr.requester.id = :userA and fr.target.id = :userB)
                   or (fr.requester.id = :userB and fr.target.id = :userA))
            """)
    int bulkUpdateStatusBetween(@Param("userA") Long userA,
                                @Param("userB") Long userB,
                                @Param("fromStatus") FriendshipRequestStatus fromStatus,
                                @Param("newStatus") FriendshipRequestStatus newStatus);

    @Modifying
    @Query("""
            delete from FriendshipRequest fr
            where fr.requester.id = :requesterId
              and fr.target.id = :targetId
              and fr.status = :status
              and fr.id <> :excludeId
            """)
    int deleteByRequesterTargetStatusExcludingId(@Param("requesterId") Long requesterId,
                                                 @Param("targetId") Long targetId,
                                                 @Param("status") FriendshipRequestStatus status,
                                                 @Param("excludeId") Long excludeId);
}
