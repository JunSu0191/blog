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

    List<User> findByDeletedYnOrderByIdAsc(String deletedYn);

    Optional<User> findFirstByDeletedYnOrderByIdAsc(String deletedYn);

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
}
