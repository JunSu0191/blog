package com.study.blog.post;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ScheduledPostPublicationService {

    private final PostRepository postRepository;

    public ScheduledPostPublicationService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishDueScheduledPosts() {
        postRepository.publishDueScheduledPosts(LocalDateTime.now());
    }
}
