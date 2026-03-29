package com.study.blog.category;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostStatus;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class CategoryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .username("writer")
                .name("Writer")
                .nickname("writer-nick")
                .password("password")
                .deletedYn("N")
                .build());
    }

    @Test
    void listCategoriesShouldReturnIdNameSlug() throws Exception {
        categoryRepository.save(Category.builder()
                .name("Backend")
                .slug("backend")
                .deletedYn("N")
                .build());
        categoryRepository.save(Category.builder()
                .name("Frontend")
                .slug("frontend")
                .deletedYn("N")
                .build());

        String response = mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(2);
        assertThat(data.get(0).path("id").isNumber()).isTrue();
        assertThat(data.get(0).path("name").asText()).isNotBlank();
        assertThat(data.get(0).path("slug").asText()).isNotBlank();
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void adminCrudShouldWorkAndSoftDeleteCategory() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Java",
                "slug", "java"
        ));

        String createResponse = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long categoryId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "name", "Java Core",
                "slug", "java-core"
        ));

        String updateResponse = mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(updateResponse).path("data").path("slug").asText()).isEqualTo("java-core");

        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isNoContent());

        Category deleted = categoryRepository.findById(categoryId).orElseThrow();
        assertThat(deleted.getDeletedYn()).isEqualTo("Y");
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void deleteInUseCategoryShouldReturnCategoryInUseCode() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .name("Spring")
                .slug("spring")
                .deletedYn("N")
                .build());

        postRepository.save(Post.builder()
                .user(user)
                .category(category)
                .title("Post")
                .slug("post-slug")
                .content("content")
                .status(PostStatus.PUBLISHED)
                .deletedYn("N")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .readTimeMinutes(1)
                .viewCount(0L)
                .likeCount(0L)
                .build());

        String response = mockMvc.perform(delete("/api/categories/{id}", category.getId()))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode errorData = objectMapper.readTree(response).path("data");
        assertThat(errorData.path("code").asText()).isEqualTo(CategoryErrorCode.CATEGORY_IN_USE.code());
    }
}

