package com.study.blog.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.category.seed.enabled=true"
})
class CategorySeedInitializerTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategorySeedInitializer categorySeedInitializer;

    @Test
    void shouldSeedDefaultBlogCategoriesWhenEnabled() {
        Set<String> expectedSlugs = Set.of(
                "daily-life",
                "home-cooking",
                "cafe-hop",
                "fitness-routine",
                "travel-diary",
                "hobbies",
                "reading-notes",
                "screen-notes",
                "home-life",
                "money-log",
                "relationships",
                "weekend-projects"
        );

        Set<String> actualSlugs = categoryRepository.findByDeletedYnOrderByNameAsc("N").stream()
                .map(Category::getSlug)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(actualSlugs).containsAll(expectedSlugs);
    }

    @Test
    void shouldBeIdempotentOnRepeatedSeedRun() throws Exception {
        long before = categoryRepository.count();

        categorySeedInitializer.run(new DefaultApplicationArguments(new String[0]));

        long after = categoryRepository.count();
        assertThat(after).isEqualTo(before);
    }
}
