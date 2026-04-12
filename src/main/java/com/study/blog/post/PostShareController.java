package com.study.blog.post;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class PostShareController {

    private final PostShareService postShareService;

    public PostShareController(PostShareService postShareService) {
        this.postShareService = postShareService;
    }

    @GetMapping(value = "/posts/{postId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> renderPostSharePage(@PathVariable Long postId, HttpServletRequest request) {
        return postShareService.renderPostHtml(postId, request)
                .map(html -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache().cachePrivate().mustRevalidate())
                        .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                        .body(html))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
