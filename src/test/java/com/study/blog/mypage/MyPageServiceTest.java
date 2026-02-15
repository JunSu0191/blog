package com.study.blog.mypage;

import com.study.blog.comment.CommentService;
import com.study.blog.like.PostLikeRepository;
import com.study.blog.mypage.dto.MyPageDto;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostService;
import com.study.blog.user.User;
import com.study.blog.user.UserProfile;
import com.study.blog.user.UserProfileRepository;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PostService postService;
    @Mock
    private CommentService commentService;

    private MyPageService myPageService;

    @BeforeEach
    void setUp() {
        myPageService = new MyPageService(
                userRepository,
                userProfileRepository,
                postRepository,
                postLikeRepository,
                postService,
                commentService);
    }

    @Test
    void getSummaryShouldReturnProfileAndStats() {
        User user = User.builder().id(1L).username("u1").name("U1").deletedYn("N").build();
        UserProfile profile = UserProfile.builder()
                .id(2L)
                .user(user)
                .displayName("display")
                .bio("bio")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        when(postRepository.countByUser_IdAndDeletedYn(1L, "N")).thenReturn(3L);
        when(commentService.getCommentCountByUser(1L)).thenReturn(5L);
        when(postLikeRepository.countByUser_IdAndDeletedYn(1L, "N")).thenReturn(7L);

        MyPageDto.SummaryResponse response = myPageService.getSummary(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("U1");
        assertThat(response.getProfile().getDisplayName()).isEqualTo("display");
        assertThat(response.getStats().getPostCount()).isEqualTo(3L);
        assertThat(response.getStats().getCommentCount()).isEqualTo(5L);
        assertThat(response.getStats().getLikedPostCount()).isEqualTo(7L);
    }

    @Test
    void upsertProfileShouldCreateWhenMissing() {
        User user = User.builder().id(1L).username("u1").name("U1").deletedYn("N").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postRepository.countByUser_IdAndDeletedYn(1L, "N")).thenReturn(0L);
        when(commentService.getCommentCountByUser(1L)).thenReturn(0L);
        when(postLikeRepository.countByUser_IdAndDeletedYn(1L, "N")).thenReturn(0L);

        MyPageDto.UpdateProfileRequest req = new MyPageDto.UpdateProfileRequest();
        req.setName("New Name");
        req.setDisplayName("display");

        MyPageDto.SummaryResponse response = myPageService.upsertProfile(1L, req);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getProfile().getDisplayName()).isEqualTo("display");
    }

    @Test
    void getSummaryShouldFallbackNameToUsernameWhenNameIsBlank() {
        User user = User.builder().id(1L).username("u1").name(" ").deletedYn("N").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(postRepository.countByUser_IdAndDeletedYn(1L, "N")).thenReturn(0L);
        when(commentService.getCommentCountByUser(1L)).thenReturn(0L);
        when(postLikeRepository.countByUser_IdAndDeletedYn(1L, "N")).thenReturn(0L);

        MyPageDto.SummaryResponse response = myPageService.getSummary(1L);

        assertThat(response.getName()).isEqualTo("u1");
    }
}
