package com.study.blog.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.post.PostContentProcessor.ProcessedContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostContentProcessorTest {

    private PostContentProcessor postContentProcessor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        postContentProcessor = new PostContentProcessor(objectMapper);
    }

    @Test
    void processShouldSanitizeAndBuildTocAndReadTime() throws Exception {
        JsonNode contentJson = objectMapper.readTree("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "heading",
                      "attrs": { "level": 2 },
                      "content": [ { "type": "text", "text": "Intro" } ]
                    },
                    {
                      "type": "paragraph",
                      "content": [
                        { "type": "text", "text": "Hello" },
                        { "type": "text", "text": " world" },
                        { "type": "text", "text": " <script>alert('xss')</script>" }
                      ]
                    }
                  ]
                }
                """);

        ProcessedContent result = postContentProcessor.process(contentJson);

        assertThat(result.contentHtml()).contains("<h2").contains("Intro");
        assertThat(result.contentHtml()).doesNotContain("<script>");
        assertThat(result.toc()).hasSize(1);
        assertThat(result.toc().get(0).text()).isEqualTo("Intro");
        assertThat(result.readTimeMinutes()).isGreaterThanOrEqualTo(1);
        assertThat(result.excerpt()).isNotBlank();
    }

    @Test
    void parseTocJsonShouldReturnEmptyListWhenInvalidJson() {
        assertThat(postContentProcessor.parseTocJson("not-json")).isEmpty();
    }
}
