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

    @Test
    void processShouldRenderCustomBlocksWithoutDroppingDataAttributes() throws Exception {
        JsonNode contentJson = objectMapper.readTree("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "callout",
                      "attrs": { "tone": "tip" },
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [{ "type": "text", "text": "팁 내용" }]
                        }
                      ]
                    },
                    {
                      "type": "simpleTable",
                      "attrs": {
                        "hasHeaderRow": true,
                        "rows": [
                          ["제목", "항목"],
                          ["A", "B"]
                        ]
                      }
                    },
                    {
                      "type": "linkCard",
                      "attrs": {
                        "url": "https://example.com",
                        "title": "example",
                        "description": "설명",
                        "domain": "example.com"
                      }
                    },
                    {
                      "type": "editorialImage",
                      "attrs": {
                        "src": "https://example.com/editorial.png",
                        "alt": "이미지",
                        "caption": "캡션"
                      }
                    },
                    {
                      "type": "twoColumnImages",
                      "attrs": {
                        "leftSrc": "https://example.com/left.png",
                        "rightSrc": "https://example.com/right.png",
                        "leftAlt": "왼쪽",
                        "rightAlt": "오른쪽",
                        "leftCaption": "캡션1",
                        "rightCaption": "캡션2"
                      }
                    }
                  ]
                }
                """);

        ProcessedContent result = postContentProcessor.process(contentJson);

        assertThat(result.contentJson()).contains("\"type\":\"callout\"");
        assertThat(result.contentHtml()).contains("data-type=\"callout\"");
        assertThat(result.contentHtml()).contains("data-callout-tone=\"tip\"");
        assertThat(result.contentHtml()).contains("data-type=\"simpleTable\"");
        assertThat(result.contentHtml()).contains("data-type=\"linkCard\"");
        assertThat(result.contentHtml()).contains("data-type=\"editorialImage\"");
        assertThat(result.contentHtml()).contains("data-type=\"twoColumnImages\"");
        assertThat(result.contentHtml()).contains("data-two-column-side=\"left\"");
        assertThat(result.contentHtml()).contains("bp-link-card");
        assertThat(result.excerpt()).contains("팁 내용");
    }

    @Test
    void parseJsonShouldReturnNullWhenSourceMissing() {
        assertThat(postContentProcessor.parseJson(null)).isNull();
        assertThat(postContentProcessor.parseJson("   ")).isNull();
    }
}
