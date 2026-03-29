package com.study.blog.seed;

import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.tag.PostTagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.blog.seed.enabled=true",
        "app.blog.seed.overwrite-existing=true",
        "app.blog.seed.author-username=sample_seed_author",
        "app.blog.seed.author-password=test-seed-password"
})
class BlogContentSeedOverwriteTest {

    @Autowired
    private BlogContentSeedInitializer blogContentSeedInitializer;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Test
    void shouldOverwriteSeededPostsWithoutPostTagConflict() throws Exception {
        long before = postRepository.count();

        blogContentSeedInitializer.run(new DefaultApplicationArguments(new String[0]));

        long after = postRepository.count();
        assertThat(after).isEqualTo(before);

        List<Post> posts = postRepository.findAll();
        assertThat(posts).isNotEmpty();
        for (Post post : posts) {
            assertThat(postTagRepository.findByPost_IdAndDeletedYn(post.getId(), "N")).isNotEmpty();
        }
    }
}
