package com.study.blog.blogprofile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlogSettingsRepository extends JpaRepository<BlogSettings, Long> {

    Optional<BlogSettings> findByUser_Id(Long userId);
}
