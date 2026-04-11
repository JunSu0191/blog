package com.study.blog.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.category.Category;
import com.study.blog.category.CategoryRepository;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostStatus;
import com.study.blog.post.PostVisibility;
import com.study.blog.tag.PostTag;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.tag.Tag;
import com.study.blog.tag.TagRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class ContentServiceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    private User writer;
    private User reader;
    private User admin;
    private Category reactCategory;
    private Tag reactTag;
    private Tag typescriptTag;
    private Post reactPost;
    private Post secondReactPost;

    @BeforeEach
    void setUp() {
        writer = userRepository.save(User.builder()
                .username("writer")
                .name("Writer React")
                .nickname("writer")
                .password("password")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build());

        reader = userRepository.save(User.builder()
                .username("reader")
                .name("Reader")
                .nickname("reader")
                .password("password")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build());

        admin = userRepository.save(User.builder()
                .username("admin")
                .name("Admin")
                .nickname("admin")
                .password("password")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build());

        reactCategory = categoryRepository.save(Category.builder()
                .name("React")
                .slug("react")
                .description("React category")
                .deletedYn("N")
                .build());

        reactTag = tagRepository.save(Tag.builder()
                .name("react")
                .slug("react")
                .description("React tag")
                .deletedYn("N")
                .build());

        typescriptTag = tagRepository.save(Tag.builder()
                .name("typescript")
                .slug("typescript")
                .description("TypeScript tag")
                .deletedYn("N")
                .build());

        reactPost = createPublishedPost(writer, reactCategory, "React Guide", "react-guide", reactTag);
        secondReactPost = createPublishedPost(writer, reactCategory, "React Patterns", "react-patterns", reactTag, typescriptTag);
    }

    @Test
    @WithMockUser(username = "reader")
    void bookmarkFlowShouldPersistStatusAndList() throws Exception {
        String bookmarkResponse = mockMvc.perform(post("/api/v1/posts/{postId}/bookmark", reactPost.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode bookmarkData = objectMapper.readTree(bookmarkResponse).path("data");
        assertThat(bookmarkData.path("bookmarked").asBoolean()).isTrue();
        assertThat(bookmarkData.path("postId").asLong()).isEqualTo(reactPost.getId());

        String statusResponse = mockMvc.perform(get("/api/v1/posts/{postId}/bookmark-status", reactPost.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(statusResponse).path("data").path("bookmarked").asBoolean()).isTrue();

        String listResponse = mockMvc.perform(get("/api/v1/me/bookmarks?page=0&size=20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode listData = objectMapper.readTree(listResponse).path("data");
        assertThat(listData.path("totalElements").asInt()).isEqualTo(1);
        assertThat(listData.path("content").get(0).path("title").asText()).isEqualTo("React Guide");
        assertThat(listData.path("content").get(0).path("tags").get(0).path("slug").asText()).isEqualTo("react");

        mockMvc.perform(delete("/api/v1/posts/{postId}/bookmark", reactPost.getId()))
                .andExpect(status().isNoContent());

        String afterDeleteStatus = mockMvc.perform(get("/api/v1/posts/{postId}/bookmark-status", reactPost.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(afterDeleteStatus).path("data").path("bookmarked").asBoolean()).isFalse();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminRecommendationFlowShouldManageSlots() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "postId", reactPost.getId(),
                "slot", 1
        ));

        String createResponse = mockMvc.perform(post("/api/v1/admin/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long recommendationId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        String listResponse = mockMvc.perform(get("/api/v1/admin/recommendations"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode listData = objectMapper.readTree(listResponse).path("data");
        assertThat(listData.isArray()).isTrue();
        assertThat(listData.get(0).path("slot").asInt()).isEqualTo(1);
        assertThat(listData.get(0).path("post").path("title").asText()).isEqualTo("React Guide");

        mockMvc.perform(delete("/api/v1/admin/recommendations/{recommendationId}", recommendationId))
                .andExpect(status().isNoContent());

        String emptyListResponse = mockMvc.perform(get("/api/v1/admin/recommendations"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(emptyListResponse).path("data").size()).isZero();
    }

    @Test
    @WithMockUser(username = "writer")
    void seriesFlowShouldExposeOrderedPosts() throws Exception {
        String createSeriesBody = objectMapper.writeValueAsString(Map.of(
                "title", "React Series",
                "slug", "react-series",
                "description", "React 연재",
                "coverImageUrl", "https://example.com/react-series.png"
        ));

        String createSeriesResponse = mockMvc.perform(post("/api/v1/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSeriesBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long seriesId = objectMapper.readTree(createSeriesResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/series/{seriesId}/posts", seriesId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "postId", reactPost.getId(),
                                "order", 1
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/series/{seriesId}/posts", seriesId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "postId", secondReactPost.getId(),
                                "order", 2
                        ))))
                .andExpect(status().isOk());

        String detailResponse = mockMvc.perform(get("/api/v1/series/{seriesId}", seriesId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(detailResponse).path("data");
        assertThat(data.path("title").asText()).isEqualTo("React Series");
        assertThat(data.path("coverImageUrl").asText()).isEqualTo("https://example.com/react-series.png");
        assertThat(data.path("postCount").asInt()).isEqualTo(2);
        assertThat(data.path("posts").get(0).path("title").asText()).isEqualTo("React Guide");
        assertThat(data.path("posts").get(1).path("title").asText()).isEqualTo("React Patterns");
        assertThat(data.path("posts").get(0).path("series").path("order").asInt()).isEqualTo(1);

        String postsResponse = mockMvc.perform(get("/api/v1/series/{seriesId}/posts?page=0&size=20", seriesId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(postsResponse).path("data").path("content").size()).isEqualTo(2);

        String listResponse = mockMvc.perform(get("/api/v1/series?page=0&size=20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(listResponse).path("data").path("content").size()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "writer")
    void seriesApiShouldSupportLegacyApiPrefixAndReturnNotFoundForMissingSeries() throws Exception {
        String createSeriesResponse = mockMvc.perform(post("/api/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Legacy Series",
                                "slug", "legacy-series"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long seriesId = objectMapper.readTree(createSeriesResponse).path("data").path("id").asLong();

        String detailResponse = mockMvc.perform(get("/api/series/{seriesId}", seriesId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode detailData = objectMapper.readTree(detailResponse).path("data");
        assertThat(detailData.path("id").asLong()).isEqualTo(seriesId);
        assertThat(detailData.path("title").asText()).isEqualTo("Legacy Series");

        String missingResponse = mockMvc.perform(get("/api/series/{seriesId}", 999999L))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode missingData = objectMapper.readTree(missingResponse);
        assertThat(missingData.path("message").asText()).isEqualTo("시리즈를 찾을 수 없습니다.");
        assertThat(missingData.path("data").path("code").asText()).isEqualTo("series_not_found");
    }

    @Test
    void searchEndpointsShouldReturnPostsTagsAuthorsAndCategories() throws Exception {
        String searchResponse = mockMvc.perform(get("/api/v1/search?q=react&page=0&size=10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode searchData = objectMapper.readTree(searchResponse).path("data");
        assertThat(searchData.path("posts").path("content").size()).isGreaterThanOrEqualTo(1);
        assertThat(searchData.path("tags").get(0).path("slug").asText()).isEqualTo("react");
        assertThat(searchData.path("authors").get(0).path("username").asText()).isEqualTo("writer");
        assertThat(searchData.path("categories").get(0).path("slug").asText()).isEqualTo("react");

        String trendingResponse = mockMvc.perform(get("/api/v1/search/trending"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(trendingResponse).path("data").path("posts").path("content").size()).isGreaterThanOrEqualTo(1);

        String recentResponse = mockMvc.perform(get("/api/v1/search/recent"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(recentResponse).path("data").path("posts").path("content").size()).isGreaterThanOrEqualTo(1);
    }

    private Post createPublishedPost(User author, Category category, String title, String slug, Tag... tags) {
        Post post = postRepository.save(Post.builder()
                .user(author)
                .category(category)
                .title(title)
                .subtitle(title + " subtitle")
                .slug(slug)
                .excerpt(title + " excerpt")
                .content(title + " content")
                .contentHtml("<p>" + title + "</p>")
                .thumbnailUrl("https://example.com/" + slug + ".png")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .publishedAt(LocalDateTime.now().minusDays(1))
                .readTimeMinutes(5)
                .deletedYn("N")
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .viewCount(100L)
                .likeCount(10L)
                .build());

        for (Tag tag : tags) {
            postTagRepository.save(PostTag.builder()
                    .post(post)
                    .tag(tag)
                    .deletedYn("N")
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return post;
    }
}
