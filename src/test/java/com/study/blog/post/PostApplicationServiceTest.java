package com.study.blog.post;

import com.study.blog.category.Category;
import com.study.blog.category.CategoryRepository;
import com.study.blog.post.dto.PostContractDto;
import com.study.blog.tag.PostTag;
import com.study.blog.tag.PostTagRepository;
import com.study.blog.tag.Tag;
import com.study.blog.tag.TagRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserProfileRepository;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostApplicationServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostTagRepository postTagRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ScheduledPostPublicationService scheduledPostPublicationService;

    private PostApplicationService postApplicationService;

    @BeforeEach
    void setUp() {
        PostContentProcessor postContentProcessor = new PostContentProcessor(new com.fasterxml.jackson.databind.ObjectMapper());
        PostSlugService postSlugService = new PostSlugService(postRepository);
        PostTagAssignmentService postTagAssignmentService = new PostTagAssignmentService(postTagRepository, tagRepository);
        postApplicationService = new PostApplicationService(
                postRepository,
                postTagRepository,
                categoryRepository,
                userRepository,
                userProfileRepository,
                postContentProcessor,
                postSlugService,
                postTagAssignmentService,
                scheduledPostPublicationService);
    }

    @Test
    void getRelatedPostsShouldPrioritizeCategoryAndTagOverlap() {
        User user = User.builder().id(1L).username("writer").name("Writer").deletedYn("N").build();
        Category category = Category.builder().id(10L).name("Backend").slug("backend").deletedYn("N").build();
        Tag javaTag = Tag.builder().id(100L).name("java").deletedYn("N").build();

        Post source = publishedPost(1L, "source", user, category, LocalDateTime.now().minusDays(2));
        Post candidateA = publishedPost(2L, "candidate-a", user, category, LocalDateTime.now().minusDays(1));
        Post candidateB = publishedPost(3L, "candidate-b", user, category, LocalDateTime.now().minusDays(20));
        Post candidateC = publishedPost(4L, "candidate-c", user, null, LocalDateTime.now().minusDays(1));

        when(postRepository.findById(1L)).thenReturn(Optional.of(source));
        when(postRepository.findRelatedCandidates(anyLong(), any(), anyBoolean(), anyCollection(), any(), any(Pageable.class)))
                .thenReturn(List.of(candidateB, candidateC, candidateA));

        when(postTagRepository.findActiveByPostIdsWithTag(anyCollection(), anyString()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<Long> postIds = invocation.getArgument(0);
                    if (postIds.contains(source.getId()) && postIds.size() == 1) {
                        return List.of(postTag(source, javaTag));
                    }
                    return List.of(
                            postTag(candidateA, javaTag),
                            postTag(candidateC, javaTag));
                });

        List<PostContractDto.RelatedPostResponse> related = postApplicationService.getRelatedPosts(1L, 3);

        assertThat(related).hasSize(3);
        assertThat(related.get(0).id()).isEqualTo(2L);
    }

    private Post publishedPost(Long id,
                               String slug,
                               User user,
                               Category category,
                               LocalDateTime publishedAt) {
        return Post.builder()
                .id(id)
                .slug(slug)
                .title(slug)
                .user(user)
                .category(category)
                .status(PostStatus.PUBLISHED)
                .publishedAt(publishedAt)
                .deletedYn("N")
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .viewCount(100L)
                .likeCount(10L)
                .readTimeMinutes(3)
                .excerpt("excerpt")
                .build();
    }

    private PostTag postTag(Post post, Tag tag) {
        return PostTag.builder()
                .post(post)
                .tag(tag)
                .deletedYn("N")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
