package com.study.blog.series;

import com.study.blog.content.ContentMapperService;
import com.study.blog.post.PostRepository;
import com.study.blog.post.ScheduledPostPublicationService;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeriesServiceTest {

    @Mock
    private PostSeriesRepository postSeriesRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContentMapperService contentMapperService;
    @Mock
    private SeriesMembershipService seriesMembershipService;
    @Mock
    private ScheduledPostPublicationService scheduledPostPublicationService;

    private SeriesService seriesService;

    @BeforeEach
    void setUp() {
        seriesService = new SeriesService(
                postSeriesRepository,
                postRepository,
                userRepository,
                contentMapperService,
                seriesMembershipService,
                scheduledPostPublicationService);
    }

    @Test
    void resolveOwnedSeriesShouldCreateKoreanSlugFromSeriesTitle() {
        User owner = User.builder().id(1L).username("writer").name("작성자").deletedYn("N").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(postSeriesRepository.existsBySlug("테스트")).thenReturn(false);
        when(postSeriesRepository.save(any(PostSeries.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostSeries series = seriesService.resolveOwnedSeries(1L, null, "테스트");

        assertThat(series).isNotNull();
        assertThat(series.getTitle()).isEqualTo("테스트");
        assertThat(series.getSlug()).isEqualTo("테스트");
    }

    @Test
    void resolveOwnedSeriesShouldSuffixKoreanSlugWhenDuplicateExists() {
        User owner = User.builder().id(1L).username("writer").name("작성자").deletedYn("N").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(postSeriesRepository.existsBySlug("테스트")).thenReturn(true);
        when(postSeriesRepository.existsBySlug("테스트-2")).thenReturn(false);
        when(postSeriesRepository.save(any(PostSeries.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostSeries series = seriesService.resolveOwnedSeries(1L, null, "테스트");

        assertThat(series.getSlug()).isEqualTo("테스트-2");
    }

    @Test
    void resolveOwnedSeriesShouldRejectSymbolOnlySeriesTitle() {
        User owner = User.builder().id(1L).username("writer").name("작성자").deletedYn("N").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> seriesService.resolveOwnedSeries(1L, null, "🔥🔥🔥"));

        assertThat(ex.getMessage()).isEqualTo("시리즈 slug를 생성할 수 없습니다.");
    }
}
