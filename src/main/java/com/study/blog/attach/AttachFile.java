package com.study.blog.attach;

import com.study.blog.post.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attach_files", indexes = {
        @Index(name = "idx_attach_files_post_id", columnList = "post_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "stored_name", length = 255)
    private String storedName;

    @Column(length = 500)
    private String path;

    @Column(name = "deleted_yn", columnDefinition = "CHAR(1)")
    private String deletedYn = "N";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
