package com.study.blog.category;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CategorySeedInitializer implements ApplicationRunner {

    private static final String FLAG_NO = "N";

    private static final List<SeedCategory> DEFAULT_BLOG_CATEGORIES = List.of(
            new SeedCategory("일상기록", "daily-life", "하루의 장면과 감정을 가볍게 남기는 기록", 10),
            new SeedCategory("집밥레시피", "home-cooking", "평일 저녁 집밥, 간단 요리, 장보기 메모", 20),
            new SeedCategory("카페산책", "cafe-hop", "동네 카페 탐방, 디저트, 조용한 공간 기록", 30),
            new SeedCategory("운동루틴", "fitness-routine", "홈트, 러닝, 스트레칭 등 꾸준한 몸관리", 40),
            new SeedCategory("여행메모", "travel-diary", "당일치기부터 짧은 여행까지 이동과 풍경 기록", 50),
            new SeedCategory("취미생활", "hobbies", "뜨개, 그림, 사진 등 취미를 이어가는 과정", 60),
            new SeedCategory("독서노트", "reading-notes", "읽은 책에서 남기고 싶은 문장과 생각", 70),
            new SeedCategory("영화드라마", "screen-notes", "주말에 본 작품 감상과 추천 메모", 80),
            new SeedCategory("집꾸미기", "home-life", "원룸 정리, 인테리어, 살림 동선 개선 기록", 90),
            new SeedCategory("돈관리", "money-log", "가계부, 소비 점검, 저축 루틴 정리", 100),
            new SeedCategory("관계와마음", "relationships", "대화 습관, 감정 관리, 인간관계 메모", 110),
            new SeedCategory("주말프로젝트", "weekend-projects", "주말에 시도한 작은 프로젝트와 결과", 120)
    );

    private final CategoryRepository categoryRepository;
    private final boolean seedEnabled;
    private final boolean overwriteExisting;

    public CategorySeedInitializer(CategoryRepository categoryRepository,
                                   @Value("${app.category.seed.enabled:false}") boolean seedEnabled,
                                   @Value("${app.category.seed.overwrite-existing:false}") boolean overwriteExisting) {
        this.categoryRepository = categoryRepository;
        this.seedEnabled = seedEnabled;
        this.overwriteExisting = overwriteExisting;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        SeedResult result = seedDefaults(overwriteExisting);
        log.info("Category seed completed. created={}, updated={}, skipped={}, overwriteExisting={}",
                result.created(), result.updated(), result.skipped(), overwriteExisting);
    }

    @Transactional
    public SeedResult seedDefaults(boolean overwriteExisting) {
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (SeedCategory item : DEFAULT_BLOG_CATEGORIES) {
            Optional<Category> bySlug = categoryRepository.findBySlug(item.slug());
            Optional<Category> byName = categoryRepository.findFirstByNameIgnoreCase(item.name());

            if (bySlug.isPresent() && byName.isPresent() && !bySlug.get().getId().equals(byName.get().getId())) {
                skipped++;
                log.warn("Category seed skipped due to conflicting slug/name ownership: slug={}, name={}",
                        item.slug(), item.name());
                continue;
            }

            Category existing = bySlug.orElseGet(() -> byName.orElse(null));
            if (existing == null) {
                categoryRepository.save(Category.builder()
                        .name(item.name())
                        .slug(item.slug())
                        .description(item.description())
                        .sortOrder(item.sortOrder())
                        .deletedYn(FLAG_NO)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
                created++;
                continue;
            }

            if (!overwriteExisting && FLAG_NO.equalsIgnoreCase(existing.getDeletedYn())) {
                skipped++;
                continue;
            }

            existing.setName(item.name());
            existing.setSlug(item.slug());
            existing.setDescription(item.description());
            existing.setSortOrder(item.sortOrder());
            existing.setDeletedYn(FLAG_NO);
            existing.setUpdatedAt(LocalDateTime.now());
            categoryRepository.save(existing);
            updated++;
        }

        return new SeedResult(created, updated, skipped);
    }

    private record SeedCategory(
            String name,
            String slug,
            String description,
            Integer sortOrder
    ) {
    }

    public record SeedResult(
            int created,
            int updated,
            int skipped
    ) {
    }
}
