package com.study.blog.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.dto.PostContractDto;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PostContentProcessor {

    private static final int EXCERPT_MAX_LENGTH = 220;
    private static final double WORDS_PER_MINUTE = 220.0;

    private final ObjectMapper objectMapper;
    private final Safelist safelist;

    public PostContentProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.safelist = Safelist.relaxed()
                .addTags("h1", "h2", "h3", "h4", "h5", "h6", "hr",
                        "aside", "section", "figure", "figcaption", "div", "span", "table", "thead", "tbody", "tr", "th", "td")
                .addAttributes(":all", "class", "data-type", "data-callout-tone", "data-two-column-side",
                        "data-has-header-row", "data-url", "data-domain")
                .addAttributes("img", "src", "alt", "title", "width", "height")
                .addAttributes("a", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https", "data");
    }

    public ProcessedContent process(JsonNode contentJson) {
        if (contentJson == null || contentJson.isNull()) {
            throw invalidContent("contentJson이 비어 있습니다.");
        }

        String normalizedJson = writeJson(contentJson);

        RenderContext context = new RenderContext();
        String rawHtml = renderNode(contentJson, context, true);
        String sanitizedHtml;
        try {
            sanitizedHtml = Jsoup.clean(rawHtml, safelist);
        } catch (Exception ex) {
            throw invalidContent("본문 HTML 정제에 실패했습니다.");
        }

        String plainText = normalizeWhitespace(context.text.toString());
        String excerpt = buildExcerpt(plainText);
        int readTimeMinutes = calculateReadTimeMinutes(plainText);
        String tocJson = writeTocJson(context.toc);

        return new ProcessedContent(
                normalizedJson,
                sanitizedHtml,
                plainText,
                excerpt,
                readTimeMinutes,
                tocJson,
                context.toc);
    }

    public List<PostContractDto.TocItem> parseTocJson(String tocJson) {
        if (tocJson == null || tocJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tocJson, new TypeReference<List<PostContractDto.TocItem>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    public JsonNode parseJson(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(jsonText);
        } catch (JsonProcessingException ex) {
            throw invalidContent("본문 JSON 파싱에 실패했습니다.");
        }
    }

    private String renderNode(JsonNode node, RenderContext context, boolean root) {
        String type = node.path("type").asText("");
        String html;

        switch (type) {
            case "doc" -> html = renderChildren(node.path("content"), context);
            case "paragraph" -> {
                String body = renderChildren(node.path("content"), context);
                context.text.append("\n");
                html = "<p>" + body + "</p>";
            }
            case "heading" -> {
                int level = clamp(node.path("attrs").path("level").asInt(2), 1, 6);
                String headingText = normalizeWhitespace(extractPlainText(node.path("content")));
                String headingId = buildHeadingId(headingText, context.headingCounters);
                if (!headingText.isBlank()) {
                    context.toc.add(new PostContractDto.TocItem(headingId, headingText, level));
                }
                context.text.append("\n");
                String body = renderChildren(node.path("content"), context);
                html = "<h" + level + " id=\"" + HtmlUtils.htmlEscape(headingId) + "\">" + body + "</h" + level + ">";
            }
            case "text" -> {
                String text = node.path("text").asText("");
                context.text.append(text);
                String escaped = HtmlUtils.htmlEscape(text);
                html = applyMarks(escaped, node.path("marks"));
            }
            case "hardBreak" -> {
                context.text.append("\n");
                html = "<br/>";
            }
            case "bulletList" -> {
                context.text.append("\n");
                html = "<ul>" + renderChildren(node.path("content"), context) + "</ul>";
            }
            case "orderedList" -> {
                context.text.append("\n");
                html = "<ol>" + renderChildren(node.path("content"), context) + "</ol>";
            }
            case "listItem" -> html = "<li>" + renderChildren(node.path("content"), context) + "</li>";
            case "blockquote" -> {
                context.text.append("\n");
                html = "<blockquote>" + renderChildren(node.path("content"), context) + "</blockquote>";
            }
            case "callout" -> html = renderCallout(node, context);
            case "simpleTable" -> html = renderSimpleTable(node, context);
            case "linkCard" -> html = renderLinkCard(node, context);
            case "editorialImage" -> html = renderEditorialImage(node, context);
            case "twoColumnImages" -> html = renderTwoColumnImages(node, context);
            case "codeBlock" -> {
                String codeText = extractPlainText(node.path("content"));
                context.text.append(codeText).append("\n");
                html = "<pre><code>" + HtmlUtils.htmlEscape(codeText) + "</code></pre>";
            }
            case "image" -> {
                String src = node.path("attrs").path("src").asText("");
                String alt = node.path("attrs").path("alt").asText("");
                String title = node.path("attrs").path("title").asText("");
                if (src.isBlank()) {
                    html = "";
                } else {
                    html = "<img src=\"" + HtmlUtils.htmlEscape(src)
                            + "\" alt=\"" + HtmlUtils.htmlEscape(alt)
                            + "\" title=\"" + HtmlUtils.htmlEscape(title) + "\"/>";
                }
            }
            case "horizontalRule" -> html = "<hr/>";
            default -> html = renderUnknownNode(node, context);
        }

        if (root && html.isBlank()) {
            throw invalidContent("본문 JSON에서 유효한 콘텐츠를 만들 수 없습니다.");
        }
        return html;
    }

    private String renderCallout(JsonNode node, RenderContext context) {
        String tone = normalizeAttr(node.path("attrs").path("tone").asText("default"));
        context.text.append("\n");
        return "<aside class=\"bp-callout\" data-type=\"callout\" data-callout-tone=\"" + HtmlUtils.htmlEscape(tone) + "\">"
                + renderChildren(node.path("content"), context)
                + "</aside>";
    }

    private String renderSimpleTable(JsonNode node, RenderContext context) {
        JsonNode rowsNode = node.path("attrs").path("rows");
        boolean hasHeaderRow = node.path("attrs").path("hasHeaderRow").asBoolean(false);
        if (!rowsNode.isArray() || rowsNode.isEmpty()) {
            return "<div class=\"bp-simple-table\" data-type=\"simpleTable\" data-has-header-row=\""
                    + hasHeaderRow + "\"></div>";
        }

        StringBuilder headBuilder = new StringBuilder();
        StringBuilder bodyBuilder = new StringBuilder();
        for (int rowIndex = 0; rowIndex < rowsNode.size(); rowIndex++) {
            JsonNode rowNode = rowsNode.get(rowIndex);
            if (!rowNode.isArray()) {
                continue;
            }
            StringBuilder rowBuilder = new StringBuilder();
            for (JsonNode cellNode : rowNode) {
                String value = normalizeCellValue(cellNode);
                if (!value.isBlank()) {
                    context.text.append(value).append(' ');
                }
                String tagName = hasHeaderRow && rowIndex == 0 ? "th" : "td";
                rowBuilder.append("<").append(tagName).append(">")
                        .append(HtmlUtils.htmlEscape(value))
                        .append("</").append(tagName).append(">");
            }
            if (hasHeaderRow && rowIndex == 0) {
                headBuilder.append("<tr>").append(rowBuilder).append("</tr>");
            } else {
                bodyBuilder.append("<tr>").append(rowBuilder).append("</tr>");
            }
        }

        context.text.append("\n");
        String thead = headBuilder.isEmpty() ? "" : "<thead>" + headBuilder + "</thead>";
        String tbody = bodyBuilder.isEmpty() ? "" : "<tbody>" + bodyBuilder + "</tbody>";
        return "<div class=\"bp-simple-table\" data-type=\"simpleTable\" data-has-header-row=\"" + hasHeaderRow + "\">"
                + "<table>" + thead + tbody + "</table></div>";
    }

    private String renderLinkCard(JsonNode node, RenderContext context) {
        JsonNode attrs = node.path("attrs");
        String url = normalizeNullable(attrs.path("url").asText(null));
        String title = normalizeNullable(attrs.path("title").asText(null));
        String description = normalizeNullable(attrs.path("description").asText(null));
        String domain = normalizeNullable(attrs.path("domain").asText(null));

        appendTextIfPresent(context, title);
        appendTextIfPresent(context, description);
        appendTextIfPresent(context, domain);
        context.text.append("\n");

        String content = joinHtml(
                title == null ? "" : "<strong class=\"bp-link-card__title\">" + HtmlUtils.htmlEscape(title) + "</strong>",
                description == null ? "" : "<p class=\"bp-link-card__description\">" + HtmlUtils.htmlEscape(description) + "</p>",
                domain == null ? "" : "<span class=\"bp-link-card__domain\">" + HtmlUtils.htmlEscape(domain) + "</span>");

        if (url == null) {
            return "<div class=\"bp-link-card\" data-type=\"linkCard\" data-domain=\""
                    + escapeAttr(domain) + "\">" + content + "</div>";
        }

        return "<a class=\"bp-link-card\" data-type=\"linkCard\" data-url=\"" + escapeAttr(url)
                + "\" data-domain=\"" + escapeAttr(domain)
                + "\" href=\"" + HtmlUtils.htmlEscape(url)
                + "\" target=\"_blank\" rel=\"noopener noreferrer\">"
                + content + "</a>";
    }

    private String renderEditorialImage(JsonNode node, RenderContext context) {
        JsonNode attrs = node.path("attrs");
        String src = normalizeNullable(attrs.path("src").asText(null));
        String alt = normalizeNullable(attrs.path("alt").asText(null));
        String title = normalizeNullable(attrs.path("title").asText(null));
        String caption = normalizeNullable(attrs.path("caption").asText(null));
        appendTextIfPresent(context, alt);
        appendTextIfPresent(context, caption);
        context.text.append("\n");

        if (src == null) {
            return "<figure class=\"bp-editorial-image\" data-type=\"editorialImage\"></figure>";
        }

        String img = "<img src=\"" + HtmlUtils.htmlEscape(src)
                + "\" alt=\"" + escapeAttr(alt)
                + "\" title=\"" + escapeAttr(title) + "\"/>";
        String figcaption = caption == null ? "" : "<figcaption>" + HtmlUtils.htmlEscape(caption) + "</figcaption>";
        return "<figure class=\"bp-editorial-image\" data-type=\"editorialImage\">"
                + img + figcaption + "</figure>";
    }

    private String renderTwoColumnImages(JsonNode node, RenderContext context) {
        JsonNode attrs = node.path("attrs");
        String left = renderTwoColumnFigure(
                normalizeNullable(attrs.path("leftSrc").asText(null)),
                normalizeNullable(attrs.path("leftAlt").asText(null)),
                normalizeNullable(attrs.path("leftCaption").asText(null)),
                "left",
                context);
        String right = renderTwoColumnFigure(
                normalizeNullable(attrs.path("rightSrc").asText(null)),
                normalizeNullable(attrs.path("rightAlt").asText(null)),
                normalizeNullable(attrs.path("rightCaption").asText(null)),
                "right",
                context);
        context.text.append("\n");
        return "<section class=\"bp-two-column-images\" data-type=\"twoColumnImages\">" + left + right + "</section>";
    }

    private String renderTwoColumnFigure(String src,
                                         String alt,
                                         String caption,
                                         String side,
                                         RenderContext context) {
        appendTextIfPresent(context, alt);
        appendTextIfPresent(context, caption);
        if (src == null) {
            return "<figure class=\"bp-two-column-images__item\" data-two-column-side=\"" + side + "\"></figure>";
        }
        String img = "<img src=\"" + HtmlUtils.htmlEscape(src)
                + "\" alt=\"" + escapeAttr(alt) + "\"/>";
        String figcaption = caption == null ? "" : "<figcaption>" + HtmlUtils.htmlEscape(caption) + "</figcaption>";
        return "<figure class=\"bp-two-column-images__item\" data-two-column-side=\"" + side + "\">"
                + img + figcaption + "</figure>";
    }

    private String renderUnknownNode(JsonNode node, RenderContext context) {
        String type = normalizeAttr(node.path("type").asText("unknown"));
        String body = renderChildren(node.path("content"), context);
        if (body.isBlank()) {
            return "<div class=\"bp-unknown-block\" data-type=\"" + HtmlUtils.htmlEscape(type) + "\"></div>";
        }
        return "<div class=\"bp-unknown-block\" data-type=\"" + HtmlUtils.htmlEscape(type) + "\">" + body + "</div>";
    }

    private String renderChildren(JsonNode children, RenderContext context) {
        if (children == null || !children.isArray() || children.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode child : children) {
            builder.append(renderNode(child, context, false));
        }
        return builder.toString();
    }

    private String applyMarks(String escapedText, JsonNode marks) {
        if (marks == null || !marks.isArray() || marks.isEmpty()) {
            return escapedText;
        }

        String value = escapedText;
        for (JsonNode mark : marks) {
            String type = mark.path("type").asText("");
            value = switch (type) {
                case "bold" -> "<strong>" + value + "</strong>";
                case "italic" -> "<em>" + value + "</em>";
                case "code" -> "<code>" + value + "</code>";
                case "strike" -> "<s>" + value + "</s>";
                case "underline" -> "<u>" + value + "</u>";
                case "link" -> {
                    String href = mark.path("attrs").path("href").asText("");
                    String target = mark.path("attrs").path("target").asText("");
                    String rel = mark.path("attrs").path("rel").asText("noopener noreferrer");
                    if (href.isBlank()) {
                        yield value;
                    }
                    String safeHref = HtmlUtils.htmlEscape(href);
                    String safeTarget = target.isBlank() ? "_blank" : HtmlUtils.htmlEscape(target);
                    String safeRel = HtmlUtils.htmlEscape(rel);
                    yield "<a href=\"" + safeHref + "\" target=\"" + safeTarget + "\" rel=\"" + safeRel + "\">"
                            + value + "</a>";
                }
                default -> value;
            };
        }
        return value;
    }

    private String extractPlainText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }

        if (node.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode child : node) {
                builder.append(extractPlainText(child));
            }
            return builder.toString();
        }

        if (node.has("text")) {
            return node.path("text").asText("");
        }

        String type = node.path("type").asText("");
        JsonNode attrs = node.path("attrs");
        if ("simpleTable".equals(type) && attrs.path("rows").isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode rowNode : attrs.path("rows")) {
                if (!rowNode.isArray()) {
                    continue;
                }
                for (JsonNode cellNode : rowNode) {
                    builder.append(normalizeCellValue(cellNode)).append(' ');
                }
                builder.append('\n');
            }
            return builder.toString();
        }
        if ("linkCard".equals(type)) {
            return joinText(
                    normalizeNullable(attrs.path("title").asText(null)),
                    normalizeNullable(attrs.path("description").asText(null)),
                    normalizeNullable(attrs.path("domain").asText(null)));
        }
        if ("editorialImage".equals(type)) {
            return joinText(
                    normalizeNullable(attrs.path("alt").asText(null)),
                    normalizeNullable(attrs.path("caption").asText(null)));
        }
        if ("twoColumnImages".equals(type)) {
            return joinText(
                    normalizeNullable(attrs.path("leftAlt").asText(null)),
                    normalizeNullable(attrs.path("leftCaption").asText(null)),
                    normalizeNullable(attrs.path("rightAlt").asText(null)),
                    normalizeNullable(attrs.path("rightCaption").asText(null)));
        }

        return extractPlainText(node.path("content"));
    }

    private String buildHeadingId(String headingText, Map<String, Integer> counter) {
        String base = headingText.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (base.isBlank()) {
            base = "section";
        }

        int next = counter.getOrDefault(base, 0) + 1;
        counter.put(base, next);
        if (next == 1) {
            return base;
        }
        return base + "-" + next;
    }

    private int calculateReadTimeMinutes(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return 0;
        }

        String[] words = plainText.trim().split("\\s+");
        int wordCount = words.length;
        if (wordCount <= 1 && plainText.length() > 400) {
            return Math.max(1, (int) Math.ceil(plainText.length() / 400.0));
        }
        return Math.max(1, (int) Math.ceil(wordCount / WORDS_PER_MINUTE));
    }

    private String buildExcerpt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        if (plainText.length() <= EXCERPT_MAX_LENGTH) {
            return plainText;
        }
        return plainText.substring(0, EXCERPT_MAX_LENGTH).trim();
    }

    private String writeJson(JsonNode contentJson) {
        try {
            return objectMapper.writeValueAsString(contentJson);
        } catch (JsonProcessingException ex) {
            throw invalidContent("본문 JSON 직렬화에 실패했습니다.");
        }
    }

    private String writeTocJson(List<PostContractDto.TocItem> toc) {
        try {
            return objectMapper.writeValueAsString(toc);
        } catch (JsonProcessingException ex) {
            throw invalidContent("목차(JSON) 직렬화에 실패했습니다.");
        }
    }

    private String normalizeWhitespace(String source) {
        if (source == null) {
            return "";
        }
        return source.replaceAll("\\s+", " ").trim();
    }

    private String normalizeCellValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return normalizeWhitespace(node.asText(""));
    }

    private void appendTextIfPresent(RenderContext context, String value) {
        String normalized = normalizeNullable(value);
        if (normalized != null) {
            context.text.append(normalized).append(' ');
        }
    }

    private String joinText(String... values) {
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String candidate = normalizeNullable(value);
            if (candidate != null) {
                normalized.add(candidate);
            }
        }
        return String.join(" ", normalized);
    }

    private String joinHtml(String... html) {
        StringBuilder builder = new StringBuilder();
        for (String part : html) {
            if (part != null && !part.isBlank()) {
                builder.append(part);
            }
        }
        return builder.toString();
    }

    private String escapeAttr(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeAttr(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? "" : normalized;
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private CodedApiException invalidContent(String message) {
        return new CodedApiException(PostErrorCode.INVALID_CONTENT.code(), HttpStatus.BAD_REQUEST, message);
    }

    private static class RenderContext {
        private final StringBuilder text = new StringBuilder();
        private final List<PostContractDto.TocItem> toc = new ArrayList<>();
        private final Map<String, Integer> headingCounters = new LinkedHashMap<>();
    }

    public record ProcessedContent(
            String contentJson,
            String contentHtml,
            String plainText,
            String excerpt,
            int readTimeMinutes,
            String tocJson,
            List<PostContractDto.TocItem> toc
    ) {
    }
}
