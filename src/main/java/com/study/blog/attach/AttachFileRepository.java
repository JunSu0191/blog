package com.study.blog.attach;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachFileRepository extends JpaRepository<AttachFile, Long> {
    List<AttachFile> findByPost_IdAndDeletedYn(Long postId, String deletedYn);
}
