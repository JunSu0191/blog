package com.study.blog.blogprofile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.category.Category;
import com.study.blog.category.CategoryRepository;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostStatus;
import com.study.blog.post.PostVisibility;
import com.study.blog.user.User;
import com.study.blog.user.UserProfile;
import com.study.blog.user.UserProfileRepository;
import com.study.blog.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class BlogProfileApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private User author;

    @BeforeEach
    void setUp() {
        author = userRepository.save(User.builder()
                .username("sample_blog_owner")
                .name("샘플사용자")
                .nickname("sample-owner")
                .password("password")
                .deletedYn("N")
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(author)
                .displayName("Kakao Blog")
                .bio("integration test profile")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Integration Category")
                .slug("integration-category")
                .deletedYn("N")
                .build());

        postRepository.save(Post.builder()
                .user(author)
                .category(category)
                .title("published-post")
                .slug("published-post")
                .content("published content")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .contentHtml("<p>published content</p>")
                .excerpt("published content")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now().minusHours(2))
                .deletedYn("N")
                .build());

        postRepository.save(Post.builder()
                .user(author)
                .category(category)
                .title("scheduled-post")
                .slug("scheduled-post")
                .content("scheduled content")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .contentHtml("<p>scheduled content</p>")
                .excerpt("scheduled content")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .deletedYn("N")
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void getPublicProfileShouldReturnOkWhenScheduledPostsNeedPublishing() throws Exception {
        String responseBody = mockMvc.perform(get("/api/blogs/{username}", author.getUsername())
                        .param("sort", "latest")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(responseBody).path("data");

        assertThat(data.path("user").path("username").asText()).isEqualTo("sample_blog_owner");
        assertThat(data.path("posts").path("content").isArray()).isTrue();
        assertThat(data.path("posts").path("content").size()).isEqualTo(1);
        assertThat(data.path("stats").path("publishedPostCount").asLong()).isEqualTo(1L);
    }
}
