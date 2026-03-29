package com.study.blog.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.category.Category;
import com.study.blog.category.CategoryRepository;
import com.study.blog.category.CategorySeedInitializer;
import com.study.blog.post.Post;
import com.study.blog.post.PostContentProcessor;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostStatus;
import com.study.blog.tag.PostTag;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.tag.Tag;
import com.study.blog.tag.TagRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserNamePolicy;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class BlogContentSeedInitializer implements ApplicationRunner {

    private static final String FLAG_NO = "N";

    private static final List<String> DEFAULT_TAGS = List.of(
            "일상", "기록", "아침루틴", "퇴근후",
            "집밥", "간단요리", "장보기", "브런치",
            "카페", "디저트", "동네산책", "사진",
            "홈트", "러닝", "스트레칭", "운동습관",
            "여행", "주말여행", "여행준비", "풍경",
            "취미", "뜨개", "그림", "소확행",
            "독서", "책추천", "에세이", "문장수집",
            "영화", "드라마", "감상노트",
            "집꾸미기", "인테리어", "정리정돈",
            "가계부", "저축", "소비기록",
            "관계", "마음기록", "셀프케어",
            "주말프로젝트"
    );

    private static final List<PostSeed> DEFAULT_POSTS = List.of(
            new PostSeed(
                    "비 오는 월요일을 버티는 아침 루틴",
                    "주초 컨디션을 지키기 위한 작은 순서들",
                    "seed-rainy-monday-morning-routine",
                    "daily-life",
                    List.of("일상", "아침루틴", "셀프케어", "기록"),
                    List.of(
                            "비가 오는 날은 시작 속도가 느려져서, 출근 준비 전에 따뜻한 물 한 잔을 먼저 마셨다.",
                            "휴대폰 알림을 10분만 늦게 확인하고 창문을 열어 공기를 바꾸니 머리가 훨씬 가벼워졌다.",
                            "완벽한 루틴보다 지키기 쉬운 순서를 정해두니 월요일 아침의 부담이 확실히 줄었다."
                    )
            ),
            new PostSeed(
                    "퇴근 후 20분 집밥 한 그릇",
                    "바쁜 평일에도 가능한 간단 저녁 루틴",
                    "seed-quick-home-dinner-bowl",
                    "home-cooking",
                    List.of("집밥", "간단요리", "퇴근후", "장보기"),
                    List.of(
                            "퇴근하고 나면 긴 요리보다 냉장고 재료로 바로 만들 수 있는 한 그릇 메뉴가 가장 실용적이었다.",
                            "주말에 손질해둔 채소와 냉동해둔 밥을 꺼내면 조리 시간 20분 안에 저녁이 완성된다.",
                            "배달을 줄이니 비용도 아끼고, 하루를 내가 마무리했다는 기분이 들어 만족도가 높아졌다."
                    )
            ),
            new PostSeed(
                    "동네 카페에서 찾은 조용한 창가 자리",
                    "사람 적은 시간대와 메뉴 조합 기록",
                    "seed-quiet-window-cafe-spot",
                    "cafe-hop",
                    List.of("카페", "디저트", "동네산책", "사진"),
                    List.of(
                            "새로 생긴 동네 카페를 아침에 찾아갔더니 창가 자리에 햇빛이 오래 머물러 있었다.",
                            "사장님이 추천해준 시즌 디저트와 플랫화이트 조합이 의외로 잘 어울려서 메모해뒀다.",
                            "사람이 적은 시간대를 알게 되니 글을 쓰거나 생각을 정리하기 훨씬 좋은 공간이 되었다."
                    )
            ),
            new PostSeed(
                    "작심삼일을 줄인 30분 운동 루틴",
                    "평일과 주말을 나눠서 지속한 방법",
                    "seed-30min-fitness-routine",
                    "fitness-routine",
                    List.of("홈트", "러닝", "스트레칭", "운동습관"),
                    List.of(
                            "운동을 오래 못 이어가던 이유는 시간이 아니라 시작 기준이 너무 높았기 때문이었다.",
                            "평일에는 20분 홈트와 10분 스트레칭만 하고, 주말에만 가볍게 러닝을 추가하는 방식으로 바꿨다.",
                            "기록 앱에 체크 표시가 쌓이기 시작하니 몸보다 먼저 마음이 안정되는 느낌을 받았다."
                    )
            ),
            new PostSeed(
                    "당일치기 바다 여행 준비 체크리스트",
                    "짧은 일정에서도 여유를 만드는 메모",
                    "seed-one-day-beach-trip-checklist",
                    "travel-diary",
                    List.of("여행", "주말여행", "여행준비", "풍경"),
                    List.of(
                            "당일치기 바다 여행은 짐을 줄이고 이동 시간을 정확히 계산하는 것만으로 절반은 성공한다.",
                            "기차 시간, 근처 식당, 돌아오는 막차를 미리 정리해두니 현지에서 선택이 훨씬 빨라졌다.",
                            "짧은 일정이어도 파도 소리를 듣고 오면 다음 주를 버틸 여유가 생긴다는 걸 다시 느꼈다."
                    )
            ),
            new PostSeed(
                    "처음 시작한 뜨개질 한 달 기록",
                    "완성도보다 꾸준함을 남기는 취미 루틴",
                    "seed-first-knitting-month-log",
                    "hobbies",
                    List.of("취미", "뜨개", "소확행", "기록"),
                    List.of(
                            "처음 시작한 뜨개질은 모양이 고르지 않아도 손이 바빠지는 시간 자체가 꽤 위로가 됐다.",
                            "퇴근 후 30분만 실을 만지기로 정하니 부담이 줄어서 자연스럽게 매일 손이 갔다.",
                            "완성도보다 반복의 리듬에 익숙해지면서 작은 성취감이 하루 끝을 부드럽게 만들었다."
                    )
            ),
            new PostSeed(
                    "요즘 마음을 다잡아준 에세이 3권",
                    "생활 속 속도를 조절해준 문장들",
                    "seed-essays-that-grounded-me",
                    "reading-notes",
                    List.of("독서", "책추천", "에세이", "문장수집"),
                    List.of(
                            "최근에 읽은 에세이 세 권은 모두 속도를 늦추는 연습에 대해 비슷한 이야기를 하고 있었다.",
                            "좋은 문장을 발견하면 바로 적어두고, 다음 날 다시 읽어보면 생각이 조금 더 선명해진다.",
                            "책을 많이 읽는 것보다 내 생활에 남는 한 문장을 만드는 일이 더 중요하다는 걸 배웠다."
                    )
            ),
            new PostSeed(
                    "주말 밤에 몰아본 드라마 감상 노트",
                    "기억에 남는 장면을 적어두는 습관",
                    "seed-weekend-drama-binge-notes",
                    "screen-notes",
                    List.of("드라마", "영화", "감상노트", "소확행"),
                    List.of(
                            "주말 밤에는 긴 설명보다 감정선이 또렷한 작품을 고르면 휴식 효과가 더 좋았다.",
                            "한 편이 끝날 때마다 좋았던 장면을 두세 줄로 적어두면 기억이 오래 남는다.",
                            "다음 작품을 고를 때도 내 취향 기준이 분명해져서 시간을 덜 쓰게 됐다."
                    )
            ),
            new PostSeed(
                    "원룸을 넓게 쓰는 정리 동선 만들기",
                    "가구를 안 바꾸고도 체감 공간 넓히기",
                    "seed-small-room-organizing-flow",
                    "home-life",
                    List.of("집꾸미기", "인테리어", "정리정돈", "기록"),
                    List.of(
                            "원룸이 답답하게 느껴질 때는 가구를 바꾸기보다 이동 동선을 먼저 점검하는 게 효과적이었다.",
                            "자주 쓰는 물건을 손 닿는 위치로 옮기고 바닥에 남는 물건을 줄이니 공간이 훨씬 넓어 보였다.",
                            "큰 지출 없이도 생활 흐름이 정리되면 집에 머무는 시간이 편안해진다."
                    )
            ),
            new PostSeed(
                    "이번 달 가계부 점검과 소비 리셋",
                    "지출 패턴을 다시 보는 월말 정리",
                    "seed-monthly-budget-reset",
                    "money-log",
                    List.of("가계부", "저축", "소비기록", "기록"),
                    List.of(
                            "이번 달은 카드 내역을 주간 단위로 나눠 보면서 충동 소비가 언제 발생하는지 먼저 확인했다.",
                            "고정비와 변동비를 분리해서 보니 줄일 수 있는 항목이 예상보다 분명하게 보였다.",
                            "무조건 아끼는 방식보다 지출 이유를 이해하는 방식이 다음 달 계획을 세우기에 훨씬 좋았다."
                    )
            ),
            new PostSeed(
                    "관계가 가벼워진 대화 습관 4가지",
                    "오해를 줄이고 마음을 지키는 방식",
                    "seed-lighter-relationships-conversation-habits",
                    "relationships",
                    List.of("관계", "마음기록", "셀프케어", "기록"),
                    List.of(
                            "대화가 엇갈릴 때는 바로 해석하지 않고 상대의 문장을 한 번 더 확인하는 습관이 도움이 됐다.",
                            "답장을 늦게 보내야 할 때는 이유를 짧게라도 먼저 전달하면 불필요한 오해가 줄어들었다.",
                            "관계를 지키는 일은 거창한 이벤트보다 작은 배려를 꾸준히 반복하는 데 가까웠다."
                    )
            ),
            new PostSeed(
                    "토요일 오전, 나만의 브런치 만들기",
                    "집에서 천천히 보내는 주말 시작",
                    "seed-saturday-brunch-at-home",
                    "home-cooking",
                    List.of("집밥", "브런치", "디저트", "소확행"),
                    List.of(
                            "토요일 오전 브런치는 냉장고 재료를 정리하면서 주말 분위기를 내기 좋은 루틴이 됐다.",
                            "계란, 버섯, 토마토처럼 실패가 적은 재료만 고르면 요리에 대한 부담이 크게 줄어든다.",
                            "예쁜 접시에 담아 천천히 먹는 시간만으로도 집에서 보내는 주말의 만족감이 높아졌다."
                    )
            ),
            new PostSeed(
                    "주말 소품 사진 찍는 법 연습",
                    "빛 방향에 따라 달라지는 분위기 기록",
                    "seed-weekend-prop-photo-practice",
                    "weekend-projects",
                    List.of("주말프로젝트", "사진", "취미", "풍경"),
                    List.of(
                            "이번 주말 프로젝트는 집 안 소품을 활용해 빛 방향별 사진을 찍어보는 작은 실험이었다.",
                            "같은 물건도 오전 햇빛과 저녁 조명에서 전혀 다른 분위기가 나와서 비교하는 재미가 컸다.",
                            "찍은 결과를 폴더별로 정리해두니 다음 촬영 아이디어를 떠올리는 속도가 빨라졌다."
                    )
            ),
            new PostSeed(
                    "여행 후 남는 건 사진보다 메모였다",
                    "짧은 기록이 다음 여행을 바꾸는 이유",
                    "seed-after-trip-memo-habit",
                    "travel-diary",
                    List.of("여행", "기록", "사진", "마음기록"),
                    List.of(
                            "여행이 끝난 뒤 며칠 안에 짧은 메모를 남기면 그때의 감정이 훨씬 또렷하게 복원된다.",
                            "사진만 보면 장소는 기억나지만, 메모를 함께 보면 내가 왜 좋았는지까지 떠오른다.",
                            "다음 여행 계획도 이전 메모를 참고하니 불필요한 이동을 줄이고 만족도가 높아졌다."
                    )
            ),
            new PostSeed(
                    "일요일 저녁 불안 줄이는 준비 루틴",
                    "월요일을 덜 무겁게 만드는 작은 정리",
                    "seed-sunday-evening-reset-routine",
                    "daily-life",
                    List.of("일상", "퇴근후", "셀프케어", "마음기록"),
                    List.of(
                            "일요일 저녁에는 다음 주 할 일을 길게 쓰기보다 월요일에 꼭 할 세 가지만 정리한다.",
                            "옷, 도시락, 출근 가방을 미리 준비해두면 아침에 결정해야 할 일이 줄어든다.",
                            "불안을 완전히 없애기보다 작은 준비로 줄여가는 방식이 일상을 오래 지키는 데 도움이 됐다."
                    )
            )
    );

    private final CategorySeedInitializer categorySeedInitializer;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final PostContentProcessor postContentProcessor;
    private final boolean seedEnabled;
    private final boolean overwriteExisting;
    private final String authorUsername;
    private final String authorPassword;

    public BlogContentSeedInitializer(CategorySeedInitializer categorySeedInitializer,
                                      CategoryRepository categoryRepository,
                                      UserRepository userRepository,
                                      TagRepository tagRepository,
                                      PostRepository postRepository,
                                      PostTagRepository postTagRepository,
                                      PasswordEncoder passwordEncoder,
                                      ObjectMapper objectMapper,
                                      PostContentProcessor postContentProcessor,
                                      @Value("${app.blog.seed.enabled:false}") boolean seedEnabled,
                                      @Value("${app.blog.seed.overwrite-existing:false}") boolean overwriteExisting,
                                      @Value("${app.blog.seed.author-username:seed_writer}") String authorUsername,
                                      @Value("${app.blog.seed.author-password:}") String authorPassword) {
        this.categorySeedInitializer = categorySeedInitializer;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.postRepository = postRepository;
        this.postTagRepository = postTagRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.postContentProcessor = postContentProcessor;
        this.seedEnabled = seedEnabled;
        this.overwriteExisting = overwriteExisting;
        this.authorUsername = authorUsername;
        this.authorPassword = authorPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        CategorySeedInitializer.SeedResult categoryResult = categorySeedInitializer.seedDefaults(overwriteExisting);
        User author = upsertAuthor();
        TagSeedResult tagResult = upsertTags(DEFAULT_TAGS);
        Map<String, Category> categoryBySlug = loadCategoryBySlug();

        int postsCreated = 0;
        int postsUpdated = 0;
        int postsSkipped = 0;

        for (int i = 0; i < DEFAULT_POSTS.size(); i++) {
            PostSeed seed = DEFAULT_POSTS.get(i);
            Category category = categoryBySlug.get(seed.categorySlug());
            if (category == null) {
                postsSkipped++;
                log.warn("Blog seed skipped post due to missing category slug={}", seed.categorySlug());
                continue;
            }

            Post existing = postRepository.findBySlug(seed.slug()).orElse(null);
            boolean shouldUpdateExisting = existing != null
                    && (overwriteExisting
                    || !"N".equalsIgnoreCase(existing.getDeletedYn())
                    || existing.getDeletedAt() != null);

            if (existing != null && !shouldUpdateExisting) {
                postsSkipped++;
                continue;
            }

            LocalDateTime publishedAt = LocalDateTime.now().minusDays(DEFAULT_POSTS.size() - i);
            PostContentProcessor.ProcessedContent processedContent = postContentProcessor.process(buildContentJson(seed));

            Post target = existing == null ? new Post() : existing;
            if (target.getCreatedAt() == null) {
                target.setCreatedAt(publishedAt.minusHours(2));
            }

            target.setUser(author);
            target.setCategory(category);
            target.setTitle(seed.title());
            target.setSubtitle(seed.subtitle());
            target.setSlug(seed.slug());
            target.setExcerpt(processedContent.excerpt());
            target.setContent(processedContent.plainText());
            target.setContentJson(processedContent.contentJson());
            target.setContentHtml(processedContent.contentHtml());
            target.setTocJson(processedContent.tocJson());
            target.setThumbnailUrl(seededImageUrl(seed.slug(), "thumb", 1200, 630));
            target.setStatus(PostStatus.PUBLISHED);
            target.setPublishedAt(publishedAt);
            target.setReadTimeMinutes(processedContent.readTimeMinutes());
            target.setDeletedYn(FLAG_NO);
            target.setDeletedAt(null);
            target.setViewCount(300L + (long) i * 47L);
            target.setLikeCount(20L + (long) i * 5L);
            target.setUpdatedAt(LocalDateTime.now());

            Post saved = postRepository.save(target);
            replacePostTags(saved, seed.tags());

            if (existing == null) {
                postsCreated++;
            } else {
                postsUpdated++;
            }
        }

        log.info(
                "Blog seed completed. author={}, categories(created={}, updated={}, skipped={}), tags(created={}, restored={}, kept={}), posts(created={}, updated={}, skipped={}), overwriteExisting={}",
                author.getUsername(),
                categoryResult.created(), categoryResult.updated(), categoryResult.skipped(),
                tagResult.created(), tagResult.restored(), tagResult.kept(),
                postsCreated, postsUpdated, postsSkipped,
                overwriteExisting
        );
    }

    private Map<String, Category> loadCategoryBySlug() {
        Map<String, Category> bySlug = new LinkedHashMap<>();
        for (Category category : categoryRepository.findByDeletedYnOrderByNameAsc(FLAG_NO)) {
            bySlug.put(category.getSlug(), category);
        }
        return bySlug;
    }

    private User upsertAuthor() {
        String normalizedUsername = UserNamePolicy.normalizeUsername(authorUsername);
        if (normalizedUsername == null || normalizedUsername.isBlank()) {
            throw new IllegalStateException("app.blog.seed.author-username 값이 유효하지 않습니다.");
        }
        if (authorPassword == null || authorPassword.isBlank()) {
            throw new IllegalStateException("app.blog.seed.author-password 값이 비어 있습니다.");
        }

        User existing = userRepository.findByUsername(normalizedUsername).orElse(null);
        if (existing == null) {
            User created = User.builder()
                    .username(normalizedUsername)
                    .name(UserNamePolicy.resolveName(null, normalizedUsername))
                    .nickname(normalizedUsername)
                    .password(passwordEncoder.encode(authorPassword))
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .mustChangePassword(false)
                    .deletedYn(FLAG_NO)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return userRepository.save(created);
        }

        boolean changed = false;
        if (!FLAG_NO.equalsIgnoreCase(existing.getDeletedYn())) {
            existing.setDeletedYn(FLAG_NO);
            changed = true;
        }
        if (existing.getStatus() == UserStatus.SUSPENDED) {
            existing.setStatus(UserStatus.ACTIVE);
            changed = true;
        }
        if (existing.getNickname() == null || existing.getNickname().isBlank()) {
            existing.setNickname(existing.getUsername());
            changed = true;
        }
        if (overwriteExisting && authorPassword != null && !authorPassword.isBlank()) {
            existing.setPassword(passwordEncoder.encode(authorPassword));
            changed = true;
        }
        if (changed) {
            existing.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(existing);
        }
        return existing;
    }

    private TagSeedResult upsertTags(List<String> tagNames) {
        int created = 0;
        int restored = 0;
        int kept = 0;

        for (String rawName : tagNames) {
            String normalized = normalizeTagName(rawName);
            if (normalized == null) {
                continue;
            }

            Tag existing = tagRepository.findByNameIgnoreCase(normalized).orElse(null);
            if (existing == null) {
                tagRepository.save(Tag.builder()
                        .name(normalized)
                        .deletedYn(FLAG_NO)
                        .createdAt(LocalDateTime.now())
                        .build());
                created++;
                continue;
            }

            if (!FLAG_NO.equalsIgnoreCase(existing.getDeletedYn())) {
                existing.setDeletedYn(FLAG_NO);
                tagRepository.save(existing);
                restored++;
                continue;
            }
            kept++;
        }

        return new TagSeedResult(created, restored, kept);
    }

    private void replacePostTags(Post post, List<String> tagNames) {
        postTagRepository.deleteByPost(post);
        postTagRepository.flush();

        List<PostTag> links = new ArrayList<>();
        for (String rawName : new LinkedHashSet<>(tagNames)) {
            String normalized = normalizeTagName(rawName);
            if (normalized == null) {
                continue;
            }
            Tag tag = tagRepository.findByNameIgnoreCaseAndDeletedYn(normalized, FLAG_NO).orElse(null);
            if (tag == null) {
                continue;
            }

            links.add(PostTag.builder()
                    .post(post)
                    .tag(tag)
                    .deletedYn(FLAG_NO)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        postTagRepository.saveAll(links);
    }

    private JsonNode buildContentJson(PostSeed seed) {
        var doc = objectMapper.createObjectNode();
        var content = doc.putArray("content");
        doc.put("type", "doc");

        addHeading(content, 2, seed.title());
        addParagraph(content, seed.subtitle());
        addImage(content, seed, "hero", 1440, 900, "대표 이미지");

        addHeading(content, 3, "오늘의 장면");
        addParagraph(content, seed.paragraphs().get(0));
        addImage(content, seed, "context", 1280, 800, "장면 이미지");

        addHeading(content, 3, "기억하고 싶은 순간");
        addParagraph(content, seed.paragraphs().get(1));
        addImage(content, seed, "approach", 1280, 800, "메모 이미지");

        addHeading(content, 3, "다음에 해볼 것");
        addParagraph(content, seed.paragraphs().get(2));
        addImage(content, seed, "result", 1280, 800, "정리 이미지");

        addHeading(content, 3, "작은 체크리스트");
        addParagraph(content, "아래 체크리스트는 다음 일상을 조금 더 편하게 만들기 위해 적어둔 메모입니다.");
        addHeading(content, 3, "관련 태그");
        addBulletList(content, seed.tags().stream()
                .map(tag -> "#" + tag)
                .toList());
        addImage(content, seed, "gallery", 1280, 800, "마무리 이미지");

        return doc;
    }

    private void addImage(com.fasterxml.jackson.databind.node.ArrayNode content,
                          PostSeed seed,
                          String slot,
                          int width,
                          int height,
                          String label) {
        var image = objectMapper.createObjectNode();
        image.put("type", "image");
        var attrs = image.putObject("attrs");
        String src = seededImageUrl(seed.slug(), slot, width, height);
        attrs.put("src", src);
        attrs.put("alt", seed.title() + " - " + label);
        attrs.put("title", seed.title() + " / " + label);
        content.add(image);
    }

    private void addHeading(com.fasterxml.jackson.databind.node.ArrayNode content, int level, String text) {
        var heading = objectMapper.createObjectNode();
        heading.put("type", "heading");
        heading.putObject("attrs").put("level", level);
        heading.putArray("content").add(textNode(text));
        content.add(heading);
    }

    private void addParagraph(com.fasterxml.jackson.databind.node.ArrayNode content, String text) {
        var paragraph = objectMapper.createObjectNode();
        paragraph.put("type", "paragraph");
        paragraph.putArray("content").add(textNode(text));
        content.add(paragraph);
    }

    private void addBulletList(com.fasterxml.jackson.databind.node.ArrayNode content, List<String> items) {
        var bulletList = objectMapper.createObjectNode();
        bulletList.put("type", "bulletList");
        var listItems = bulletList.putArray("content");

        for (String item : items) {
            var listItem = objectMapper.createObjectNode();
            listItem.put("type", "listItem");
            var listItemContent = listItem.putArray("content");

            var paragraph = objectMapper.createObjectNode();
            paragraph.put("type", "paragraph");
            paragraph.putArray("content").add(textNode(item));
            listItemContent.add(paragraph);
            listItems.add(listItem);
        }
        content.add(bulletList);
    }

    private com.fasterxml.jackson.databind.node.ObjectNode textNode(String text) {
        var textNode = objectMapper.createObjectNode();
        textNode.put("type", "text");
        textNode.put("text", text);
        return textNode;
    }

    private String normalizeTagName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String seededImageUrl(String slug, String slot, int width, int height) {
        return "https://picsum.photos/seed/" + slug + "-" + slot + "/" + width + "/" + height;
    }

    private record TagSeedResult(
            int created,
            int restored,
            int kept
    ) {
    }

    private record PostSeed(
            String title,
            String subtitle,
            String slug,
            String categorySlug,
            List<String> tags,
            List<String> paragraphs
    ) {
        private PostSeed {
            Objects.requireNonNull(title);
            Objects.requireNonNull(slug);
            Objects.requireNonNull(categorySlug);
            Objects.requireNonNull(tags);
            Objects.requireNonNull(paragraphs);
        }
    }
}
