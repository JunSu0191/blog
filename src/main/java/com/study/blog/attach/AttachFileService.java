package com.study.blog.attach;

import com.study.blog.attach.dto.AttachFileDto;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttachFileService {

    private final AttachFileRepository attachFileRepository;
    private final PostRepository postRepository;

    public AttachFileService(AttachFileRepository attachFileRepository, PostRepository postRepository) {
        this.attachFileRepository = attachFileRepository;
        this.postRepository = postRepository;
    }

    public AttachFileDto.Response create(AttachFileDto.Request req) {
        Post post = null;
        if (req.postId != null) {
            post = postRepository.findById(req.postId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found: " + req.postId));
        }

        AttachFile af = AttachFile.builder()
                .post(post)
                .originalName(req.originalName)
                .storedName(req.storedName)
                .path(req.path)
                .build();

        AttachFile saved = attachFileRepository.save(af);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<AttachFileDto.Response> get(Long id) {
        return attachFileRepository.findById(id)
                .filter(a -> "N".equals(a.getDeletedYn()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AttachFileDto.Response> listByPost(Long postId) {
        return attachFileRepository.findByPost_IdAndDeletedYn(postId, "N")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        AttachFile af = attachFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AttachFile not found: " + id));
        af.setDeletedYn("Y");
        attachFileRepository.save(af);
    }

    private AttachFileDto.Response toResponse(AttachFile a) {
        AttachFileDto.Response r = new AttachFileDto.Response();
        r.id = a.getId();
        r.postId = a.getPost() != null ? a.getPost().getId() : null;
        r.originalName = a.getOriginalName();
        r.storedName = a.getStoredName();
        r.path = a.getPath();
        r.deletedYn = a.getDeletedYn();
        r.createdAt = a.getCreatedAt();
        return r;
    }
}
