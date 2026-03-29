package com.study.blog.seed;

import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostStatus;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.tag.TagRepository;
import com.study.blog.user.UserRepository;
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
        "app.blog.seed.author-username=sample_seed_author",
        "app.blog.seed.author-password=test-seed-password"
})
class BlogContentSeedInitializerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private BlogContentSeedInitializer blogContentSeedInitializer;

    @Test
    void shouldSeedAuthorTagsAndPosts() {
        List<String> slugs = List.of(
                "seed-rainy-monday-morning-routine",
                "seed-quick-home-dinner-bowl",
                "seed-quiet-window-cafe-spot",
                "seed-30min-fitness-routine",
                "seed-one-day-beach-trip-checklist",
                "seed-first-knitting-month-log",
                "seed-essays-that-grounded-me",
                "seed-weekend-drama-binge-notes",
                "seed-small-room-organizing-flow",
                "seed-monthly-budget-reset",
                "seed-lighter-relationships-conversation-habits",
                "seed-saturday-brunch-at-home",
                "seed-weekend-prop-photo-practice",
                "seed-after-trip-memo-habit",
                "seed-sunday-evening-reset-routine"
        );

        assertThat(userRepository.findByUsername("sample_seed_author")).isPresent();
        assertThat(tagRepository.findByNameIgnoreCaseAndDeletedYn("일상", "N")).isPresent();

        for (String slug : slugs) {
            Post post = postRepository.findBySlugAndDeletedYn(slug, "N").orElse(null);
            assertThat(post).as("missing post slug=%s", slug).isNotNull();
            assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(postTagRepository.findByPost_IdAndDeletedYn(post.getId(), "N")).isNotEmpty();
            assertThat(post.getContentHtml()).contains("<img");
            int imageCount = post.getContentHtml().split("<img").length - 1;
            assertThat(imageCount).isGreaterThanOrEqualTo(5);
        }
    }

    @Test
    void shouldBeIdempotentWhenRunAgain() throws Exception {
        long beforePostCount = postRepository.count();
        long beforeTagCount = tagRepository.count();

        blogContentSeedInitializer.run(new DefaultApplicationArguments(new String[0]));

        assertThat(postRepository.count()).isEqualTo(beforePostCount);
        assertThat(tagRepository.count()).isEqualTo(beforeTagCount);
    }
}
