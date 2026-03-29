package com.study.blog.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndDeletedYn(String username, String deletedYn);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByDeletedYnOrderByIdAsc(String deletedYn);

    Optional<User> findFirstByDeletedYnOrderByIdAsc(String deletedYn);

    Optional<User> findByEmailAndDeletedYn(String email, String deletedYn);

    Optional<User> findByPhoneNumberAndDeletedYn(String phoneNumber, String deletedYn);

    long countByDeletedYn(String deletedYn);

    long countByRoleAndDeletedYn(UserRole role, String deletedYn);

    @Query("""
            select u
            from User u
            where u.deletedYn = 'N'
              and (:keyword is null
                   or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<User> searchAdminUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select u as user, count(distinct p.id) as postCount
            from User u
            left join Post p on p.user = u
                and p.deletedYn = 'N'
                and p.deletedAt is null
                and p.status = com.study.blog.post.PostStatus.PUBLISHED
                and p.visibility = com.study.blog.post.PostVisibility.PUBLIC
                and p.publishedAt is not null
            where u.deletedYn = 'N'
              and u.status = com.study.blog.user.UserStatus.ACTIVE
              and (:keyword is null
                   or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.name) like lower(concat('%', :keyword, '%')))
            group by u
            order by count(distinct p.id) desc, u.username asc
            """)
    List<UserWithPostCountProjection> searchPublicAuthors(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            select u
            from User u
            where u.deletedYn = 'N'
              and u.status = com.study.blog.user.UserStatus.ACTIVE
            order by u.createdAt desc, u.id desc
            """)
    List<User> findRecentActiveUsers(Pageable pageable);
}
