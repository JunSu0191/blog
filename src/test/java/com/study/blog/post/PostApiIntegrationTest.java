package com.study.blog.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.category.Category;
import com.study.blog.category.CategoryRepository;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.tag.Tag;
import com.study.blog.tag.TagRepository;
import com.study.blog.series.PostSeries;
import com.study.blog.series.PostSeriesRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
class PostApiIntegrationTest {

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
    private TagRepository tagRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private PostSeriesRepository postSeriesRepository;

    @Autowired
    private EntityManager entityManager;

    private Category category;
    private Category anotherCategory;
    private Tag tag;
    private User user;
    private PostSeries series;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("writer")
                .name("Writer")
                .nickname("writerNick")
                .password("password")
                .deletedYn("N")
                .build();
        userRepository.save(user);
        userProfileRepository.save(UserProfile.builder()
                .user(user)
                .displayName("Writer Display")
                .avatarUrl("https://example.com/avatar-writer.png")
                .build());

        category = Category.builder()
                .name("Backend Category")
                .slug("backend-category")
                .description("backend")
                .deletedYn("N")
                .build();
        categoryRepository.save(category);

        anotherCategory = Category.builder()
                .name("Frontend Category")
                .slug("frontend-category")
                .description("frontend")
                .deletedYn("N")
                .build();
        categoryRepository.save(anotherCategory);

        tag = Tag.builder()
                .name("spring-contract")
                .deletedYn("N")
                .build();
        tagRepository.save(tag);

        series = postSeriesRepository.save(PostSeries.builder()
                .owner(user)
                .title("Existing Series")
                .slug("existing-series")
                .build());
    }

    @Test
    @WithMockUser(username = "writer")
    void draftPublishListDetailAndRelatedFlowShouldWork() throws Exception {
        String draftBody = objectMapper.writeValueAsString(Map.of(
                "title", "Draft One",
                "subtitle", "Sub",
                "categoryId", category.getId(),
                "thumbnailUrl", "https://example.com/thumb.png",
                "contentJson", sampleContentJson("Draft body")
        ));

        String draftCreateResponse = mockMvc.perform(post("/api/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdDraftNode = objectMapper.readTree(draftCreateResponse);
        long draftId = createdDraftNode.path("data").path("id").asLong();
        assertThat(createdDraftNode.path("data").path("category").path("id").asLong()).isEqualTo(category.getId());

        String draftUpdateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Draft Updated",
                "subtitle", "Sub Updated",
                "categoryId", category.getId(),
                "thumbnailUrl", "https://example.com/thumb2.png",
                "contentJson", sampleContentJson("Updated draft body")
        ));

        mockMvc.perform(put("/api/posts/drafts/{draftId}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftUpdateBody))
                .andExpect(status().isOk());

        String draftGetResponse = mockMvc.perform(get("/api/posts/drafts/{draftId}", draftId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode draftDataNode = objectMapper.readTree(draftGetResponse).path("data");
        assertThat(draftDataNode.path("title").asText())
                .isEqualTo("Draft Updated");
        assertThat(draftDataNode.path("category").path("id").asLong()).isEqualTo(category.getId());

        mockMvc.perform(get("/api/posts/drafts?page=0&size=10"))
                .andExpect(status().isOk());

        String publishBody = objectMapper.writeValueAsString(Map.of(
                "title", "Published One",
                "subtitle", "Public",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/post-thumb.png",
                "contentJson", sampleContentJson("Hello <script>alert('xss')</script> world"),
                "publishNow", true
        ));

        String publishResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode publishedNode = objectMapper.readTree(publishResponse).path("data");
        long postId = publishedNode.path("id").asLong();

        assertThat(publishedNode.path("status").asText()).isEqualTo("PUBLISHED");
        assertThat(publishedNode.path("contentHtml").asText()).doesNotContain("<script>");
        assertThat(publishedNode.path("category").path("id").asLong()).isEqualTo(category.getId());

        mockMvc.perform(get("/api/posts?q=Published&sort=latest&page=0&size=10"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        String detailResponse = mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode detailNode = objectMapper.readTree(detailResponse).path("data");
        assertThat(detailNode.path("authorName").asText()).isEqualTo("Writer");
        assertThat(detailNode.path("author").path("username").asText()).isEqualTo("writer");
        assertThat(detailNode.path("tags").isArray()).isTrue();
        assertThat(detailNode.path("tags").size()).isEqualTo(1);
        assertThat(detailNode.path("tags").get(0).path("id").asLong()).isEqualTo(tag.getId());
        assertThat(detailNode.path("tags").get(0).path("name").asText()).isEqualTo(tag.getName());

        String secondPublishBody = objectMapper.writeValueAsString(Map.of(
                "title", "Published Two",
                "subtitle", "Public 2",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/post-thumb-2.png",
                "contentJson", sampleContentJson("Second post body"),
                "publishNow", true
        ));

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondPublishBody))
                .andExpect(status().isCreated());

        String relatedResponse = mockMvc.perform(get("/api/posts/{postId}/related?limit=5", postId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode relatedContent = objectMapper.readTree(relatedResponse).path("data");
        assertThat(relatedContent.isArray()).isTrue();
        assertThat(relatedContent.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @WithMockUser(username = "writer")
    void createAndUpdatePostShouldPersistCategoryAndExposeCategoryObject() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Category Linked Post",
                "subtitle", "Sub",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/thumb.png",
                "contentJson", sampleContentJson("body"),
                "publishNow", true
        ));

        String createResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdData = objectMapper.readTree(createResponse).path("data");
        long postId = createdData.path("id").asLong();
        assertThat(createdData.path("category").path("id").asLong()).isEqualTo(category.getId());
        assertThat(createdData.path("category").has("slug")).isFalse();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Category Linked Post Updated",
                "subtitle", "Sub Updated",
                "categoryId", anotherCategory.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/thumb2.png",
                "contentJson", sampleContentJson("updated body"),
                "publishNow", true
        ));

        String updateResponse = mockMvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode updatedData = objectMapper.readTree(updateResponse).path("data");
        assertThat(updatedData.path("category").path("id").asLong()).isEqualTo(anotherCategory.getId());
        assertThat(updatedData.path("category").path("name").asText()).isEqualTo(anotherCategory.getName());
    }

    @Test
    @WithMockUser(username = "writer")
    void createAndUpdatePostShouldPersistExistingSeriesAndOrder() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Series Linked Post",
                "subtitle", "Sub",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/thumb.png",
                "contentJson", sampleContentJson("body"),
                "publishNow", true,
                "seriesId", series.getId(),
                "seriesOrder", 2
        ));

        String createResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdData = objectMapper.readTree(createResponse).path("data");
        long postId = createdData.path("id").asLong();
        assertThat(createdData.path("series").path("id").asLong()).isEqualTo(series.getId());
        assertThat(createdData.path("series").path("title").asText()).isEqualTo(series.getTitle());
        assertThat(createdData.path("series").path("order").asInt()).isEqualTo(1);

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Series Linked Post Updated",
                "subtitle", "Sub Updated",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/thumb2.png",
                "contentJson", sampleContentJson("updated body"),
                "publishNow", true,
                "seriesId", series.getId(),
                "seriesOrder", 1
        ));

        String updateResponse = mockMvc.perform(put("/api/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode updatedData = objectMapper.readTree(updateResponse).path("data");
        assertThat(updatedData.path("series").path("id").asLong()).isEqualTo(series.getId());
        assertThat(updatedData.path("series").path("order").asInt()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "writer")
    void createPostShouldCreateSeriesWhenOnlySeriesTitleIsProvided() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "New Series Post",
                "subtitle", "Sub",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/thumb.png",
                "contentJson", sampleContentJson("body"),
                "publishNow", true,
                "seriesTitle", "React Notes",
                "seriesOrder", 1
        ));

        String createResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdData = objectMapper.readTree(createResponse).path("data");
        assertThat(createdData.path("series").path("id").asLong()).isPositive();
        assertThat(createdData.path("series").path("title").asText()).isEqualTo("React Notes");
        assertThat(createdData.path("series").path("slug").asText()).isEqualTo("react-notes");
        assertThat(createdData.path("series").path("order").asInt()).isEqualTo(1);
        assertThat(postSeriesRepository.existsBySlug("react-notes")).isTrue();
    }

    @Test
    @WithMockUser(username = "writer")
    void draftShouldPersistSeriesFieldsAndExposeSeriesObject() throws Exception {
        String draftBody = objectMapper.writeValueAsString(Map.of(
                "title", "Draft With Series",
                "subtitle", "Sub",
                "categoryId", category.getId(),
                "thumbnailUrl", "https://example.com/thumb.png",
                "contentJson", sampleContentJson("Draft body"),
                "seriesId", series.getId(),
                "seriesOrder", 3
        ));

        String draftCreateResponse = mockMvc.perform(post("/api/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdDraftNode = objectMapper.readTree(draftCreateResponse).path("data");
        long draftId = createdDraftNode.path("id").asLong();
        assertThat(createdDraftNode.path("series").path("id").asLong()).isEqualTo(series.getId());
        assertThat(createdDraftNode.path("series").path("order").asInt()).isEqualTo(3);

        String draftUpdateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Draft With New Series",
                "subtitle", "Sub Updated",
                "categoryId", category.getId(),
                "thumbnailUrl", "https://example.com/thumb2.png",
                "contentJson", sampleContentJson("Updated draft body"),
                "seriesTitle", "Draft Series",
                "seriesOrder", 1
        ));

        String updatedDraftResponse = mockMvc.perform(put("/api/posts/drafts/{draftId}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftUpdateBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode updatedDraftNode = objectMapper.readTree(updatedDraftResponse).path("data");
        assertThat(updatedDraftNode.path("series").path("title").asText()).isEqualTo("Draft Series");
        assertThat(updatedDraftNode.path("series").path("slug").asText()).isEqualTo("draft-series");
        assertThat(updatedDraftNode.path("series").path("order").asInt()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "writer")
    void listPostsShouldFilterByCategoryId() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Backend Post",
                                "subtitle", "Backend",
                                "categoryId", category.getId(),
                                "tagIds", List.of(tag.getId()),
                                "thumbnailUrl", "https://example.com/b1.png",
                                "contentJson", sampleContentJson("backend"),
                                "publishNow", true
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Frontend Post",
                                "subtitle", "Frontend",
                                "categoryId", anotherCategory.getId(),
                                "tagIds", List.of(tag.getId()),
                                "thumbnailUrl", "https://example.com/f1.png",
                                "contentJson", sampleContentJson("frontend"),
                                "publishNow", true
                        ))))
                .andExpect(status().isCreated());

        String filteredResponse = mockMvc.perform(get("/api/posts?categoryId=" + category.getId() + "&page=0&size=10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode content = objectMapper.readTree(filteredResponse).path("data").path("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThanOrEqualTo(1);
        for (JsonNode postNode : content) {
            assertThat(postNode.path("category").path("id").asLong()).isEqualTo(category.getId());
        }
    }

    @Test
    @WithMockUser(username = "writer")
    void createPostWithoutCategoryShouldReturnNullCategory() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "No Category Post",
                "subtitle", "No Category",
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/no-category.png",
                "contentJson", sampleContentJson("no category"),
                "publishNow", true
        ));

        String createResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(createResponse).path("data");
        assertThat(data.path("category").isNull()).isTrue();
        assertThat(data.path("categoryId").isNull()).isTrue();
    }

    @Test
    @WithMockUser(username = "writer")
    void createPostShouldMergeTagIdsAndTagNamesAndExposeTagSlugs() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Tag Merge Post",
                "subtitle", "Tag Merge",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "tags", List.of("React", " react ", ""),
                "thumbnailUrl", "https://example.com/tag-merge.png",
                "contentJson", sampleContentJson("tag merge body"),
                "publishNow", true
        ));

        String createResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdData = objectMapper.readTree(createResponse).path("data");
        long postId = createdData.path("id").asLong();

        Tag createdTag = tagRepository.findByNameIgnoreCaseAndDeletedYn("react", "N").orElseThrow();
        assertThat(postTagRepository.findByPost_IdAndDeletedYn(postId, "N"))
                .extracting(postTag -> postTag.getTag().getId())
                .containsExactlyInAnyOrder(tag.getId(), createdTag.getId());

        assertThat(readTagField(createdData.path("tags"), "name"))
                .containsExactlyInAnyOrder("spring-contract", "react");
        assertThat(readTagField(createdData.path("tags"), "slug"))
                .containsExactlyInAnyOrder("spring-contract", "react");

        String detailResponse = mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode detailTags = objectMapper.readTree(detailResponse).path("data").path("tags");
        assertThat(readTagField(detailTags, "name"))
                .containsExactlyInAnyOrder("spring-contract", "react");
        assertThat(readTagField(detailTags, "slug"))
                .containsExactlyInAnyOrder("spring-contract", "react");
    }

    @Test
    @WithMockUser(username = "writer")
    void deletePostShouldSoftDeleteAndMakeDetailUnavailable() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Delete Me",
                "subtitle", "to be deleted",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/delete-me.png",
                "contentJson", sampleContentJson("delete target"),
                "publishNow", true
        ));

        String createResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long postId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/posts/{postId}", postId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "writer")
    void listPostsShouldExposeAuthorAndImageUrlsForFeedCards() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Feed Card Post",
                "subtitle", "card",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/thumb-feed.png",
                "contentJson", sampleContentJsonWithImages("card body",
                        "https://example.com/image-1.png",
                        "https://example.com/image-2.png"),
                "publishNow", true
        ));

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        String listResponse = mockMvc.perform(get("/api/posts?q=Feed Card Post&sort=latest&page=0&size=10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(listResponse)
                .path("data")
                .path("content")
                .get(0);

        assertThat(first.path("author").path("username").asText()).isEqualTo("writer");
        assertThat(first.path("author").path("name").asText()).isEqualTo("Writer");
        assertThat(first.path("author").path("nickname").asText()).isEqualTo("writerNick");
        assertThat(first.path("author").path("profileImageUrl").asText()).isEqualTo("https://example.com/avatar-writer.png");
        assertThat(first.path("publishedAt").asText()).isNotBlank();
        assertThat(first.path("imageUrls").isArray()).isTrue();
        assertThat(first.path("imageUrls").size()).isEqualTo(2);
        assertThat(first.path("imageUrls").get(0).asText()).isEqualTo("https://example.com/image-1.png");
    }

    @Test
    @WithMockUser(username = "writer")
    void customBlocksShouldRoundTripForDraftAndPublishedPost() throws Exception {
        Map<String, Object> customContentJson = sampleCustomBlockContentJson();

        String draftBody = objectMapper.writeValueAsString(Map.of(
                "title", "Custom Draft",
                "subtitle", "draft",
                "categoryId", category.getId(),
                "thumbnailUrl", "https://example.com/custom-draft.png",
                "contentJson", customContentJson
        ));

        String draftResponse = mockMvc.perform(post("/api/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long draftId = objectMapper.readTree(draftResponse).path("data").path("id").asLong();

        JsonNode storedDraft = objectMapper.readTree(mockMvc.perform(get("/api/posts/drafts/{draftId}", draftId))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .path("data");

        assertThat(storedDraft.path("contentJson").path("content").get(0).path("type").asText()).isEqualTo("callout");
        assertThat(storedDraft.path("contentJson").path("content").get(1).path("type").asText()).isEqualTo("simpleTable");
        assertThat(storedDraft.path("contentJson").path("content").get(2).path("type").asText()).isEqualTo("linkCard");
        assertThat(storedDraft.path("contentJson").path("content").get(3).path("type").asText()).isEqualTo("editorialImage");
        assertThat(storedDraft.path("contentJson").path("content").get(4).path("type").asText()).isEqualTo("twoColumnImages");

        String publishBody = objectMapper.writeValueAsString(Map.of(
                "title", "Custom Post",
                "subtitle", "publish",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/custom-post.png",
                "contentJson", customContentJson,
                "publishNow", true
        ));

        String publishResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long postId = objectMapper.readTree(publishResponse).path("data").path("id").asLong();

        JsonNode detail = objectMapper.readTree(mockMvc.perform(get("/api/posts/{postId}", postId))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .path("data");

        assertThat(detail.path("contentJson").path("content").get(0).path("attrs").path("tone").asText()).isEqualTo("tip");
        assertThat(detail.path("contentJson").path("content").get(1).path("attrs").path("rows").get(0).get(0).asText()).isEqualTo("제목");
        assertThat(detail.path("contentJson").path("content").get(2).path("attrs").path("url").asText()).isEqualTo("https://example.com");
        assertThat(detail.path("contentJson").path("content").get(3).path("attrs").path("src").asText()).isEqualTo("https://example.com/editorial.png");
        assertThat(detail.path("contentJson").path("content").get(4).path("attrs").path("leftSrc").asText()).isEqualTo("https://example.com/left.png");
        assertThat(detail.path("contentHtml").asText()).contains("data-type=\"callout\"");
        assertThat(detail.path("contentHtml").asText()).contains("data-type=\"simpleTable\"");
        assertThat(detail.path("contentHtml").asText()).contains("data-type=\"linkCard\"");
        assertThat(detail.path("contentHtml").asText()).contains("data-type=\"editorialImage\"");
        assertThat(detail.path("contentHtml").asText()).contains("data-type=\"twoColumnImages\"");
        assertThat(detail.path("contentHtml").asText()).contains("data-two-column-side=\"left\"");
    }

    @Test
    @WithMockUser(username = "writer")
    void postsApiShouldPersistAndExposeSeriesSummary() throws Exception {
        String seriesResponse = mockMvc.perform(post("/api/v1/series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Frontend Redesign Log",
                                "slug", "frontend-redesign-log",
                                "description", "series desc"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long seriesId = objectMapper.readTree(seriesResponse).path("data").path("id").asLong();

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Series Linked Post",
                "subtitle", "series",
                "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()),
                "thumbnailUrl", "https://example.com/series-linked.png",
                "contentJson", sampleContentJson("body"),
                "publishNow", true,
                "seriesId", seriesId,
                "seriesOrder", 1
        ));

        String createResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdData = objectMapper.readTree(createResponse).path("data");
        long postId = createdData.path("id").asLong();
        assertThat(createdData.path("series").path("id").asLong()).isEqualTo(seriesId);
        assertThat(createdData.path("series").path("order").asInt()).isEqualTo(1);

        JsonNode detailData = objectMapper.readTree(mockMvc.perform(get("/api/posts/{postId}", postId))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .path("data");
        assertThat(detailData.path("series").path("slug").asText()).isEqualTo("frontend-redesign-log");

        JsonNode listItem = objectMapper.readTree(mockMvc.perform(get("/api/posts?q=Series Linked Post&page=0&size=10"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .path("data")
                .path("content")
                .get(0);
        assertThat(listItem.path("series").path("id").asLong()).isEqualTo(seriesId);
        assertThat(listItem.path("series").path("postCount").asInt()).isEqualTo(1);
    }

    private Map<String, Object> sampleContentJson(String text) {
        return Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of(
                                "type", "heading",
                                "attrs", Map.of("level", 2),
                                "content", List.of(Map.of("type", "text", "text", "Introduction"))),
                        Map.of(
                                "type", "paragraph",
                                "content", List.of(Map.of("type", "text", "text", text)))
                )
        );
    }

    private Map<String, Object> sampleContentJsonWithImages(String text, String firstImage, String secondImage) {
        return Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of(
                                "type", "paragraph",
                                "content", List.of(Map.of("type", "text", "text", text))),
                        Map.of(
                                "type", "image",
                                "attrs", Map.of("src", firstImage, "alt", "first")),
                        Map.of(
                                "type", "image",
                                "attrs", Map.of("src", secondImage, "alt", "second"))
                )
        );
    }

    private Map<String, Object> sampleCustomBlockContentJson() {
        return Map.of(
                "type", "doc",
                "content", List.of(
                        Map.of(
                                "type", "callout",
                                "attrs", Map.of("tone", "tip"),
                                "content", List.of(
                                        Map.of(
                                                "type", "paragraph",
                                                "content", List.of(Map.of("type", "text", "text", "팁 내용"))))),
                        Map.of(
                                "type", "simpleTable",
                                "attrs", Map.of(
                                        "hasHeaderRow", true,
                                        "rows", List.of(
                                                List.of("제목", "항목", "설명"),
                                                List.of("A", "B", "C")))),
                        Map.of(
                                "type", "linkCard",
                                "attrs", Map.of(
                                        "url", "https://example.com",
                                        "title", "example",
                                        "description", "설명",
                                        "domain", "example.com")),
                        Map.of(
                                "type", "editorialImage",
                                "attrs", Map.of(
                                        "src", "https://example.com/editorial.png",
                                        "alt", "이미지",
                                        "caption", "캡션")),
                        Map.of(
                                "type", "twoColumnImages",
                                "attrs", Map.of(
                                        "leftSrc", "https://example.com/left.png",
                                        "rightSrc", "https://example.com/right.png",
                                        "leftAlt", "왼쪽",
                                        "rightAlt", "오른쪽",
                                        "leftCaption", "캡션1",
                                        "rightCaption", "캡션2")))
        );
    }

    private List<String> readTagField(JsonNode tagsNode, String fieldName) {
        List<String> values = new java.util.ArrayList<>();
        for (JsonNode tagNode : tagsNode) {
            values.add(tagNode.path(fieldName).asText());
        }
        return values;
    }
}
