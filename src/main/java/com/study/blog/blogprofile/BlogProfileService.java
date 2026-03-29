package com.study.blog.blogprofile;

import com.study.blog.blogprofile.dto.BlogProfileDto;
import com.study.blog.blogprofile.dto.BlogSettingsDto;
import com.study.blog.core.exception.CodedApiException;
import com.study.blog.core.response.PageResponse;
import com.study.blog.post.PostApplicationService;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostStatus;
import com.study.blog.post.dto.PostContractDto;
import com.study.blog.user.User;
import com.study.blog.user.UserNamePolicy;
import com.study.blog.user.UserProfile;
import com.study.blog.user.UserProfileRepository;
import com.study.blog.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BlogProfileService {

    private static final String FLAG_NO = "N";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PostRepository postRepository;
    private final PostApplicationService postApplicationService;
    private final BlogSettingsService blogSettingsService;
    private final String webBaseUrl;

    public BlogProfileService(UserRepository userRepository,
                              UserProfileRepository userProfileRepository,
                              PostRepository postRepository,
                              PostApplicationService postApplicationService,
                              BlogSettingsService blogSettingsService,
                              @Value("${app.web-base-url:http://localhost:5173}") String webBaseUrl) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.postRepository = postRepository;
        this.postApplicationService = postApplicationService;
        this.blogSettingsService = blogSettingsService;
        this.webBaseUrl = webBaseUrl;
    }

    @Transactional(readOnly = true)
    public BlogProfileDto.PublicProfileResponse getPublicProfile(String rawUsername,
                                                                 String query,
                                                                 String sort,
                                                                 Pageable pageable) {
        String username = UserNamePolicy.normalizeUsername(rawUsername);
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username은 필수입니다.");
        }

        User user = userRepository.findByUsernameAndDeletedYn(username, FLAG_NO)
                .orElseThrow(this::blogProfileNotFound);
        UserProfile profile = userProfileRepository.findByUser_Id(user.getId()).orElse(null);

        Page<PostContractDto.PostListItem> posts = postApplicationService.listPublishedPostsByAuthor(
                user.getId(),
                query,
                sort,
                pageable);

        long publishedPostCount = postRepository.countByUser_IdAndDeletedYnAndDeletedAtIsNullAndStatusAndPublishedAtIsNotNull(
                user.getId(),
                FLAG_NO,
                PostStatus.PUBLISHED);

        BlogProfileDto.UserSummary userSummary = new BlogProfileDto.UserSummary(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getNickname(),
                user.getCreatedAt());

        BlogProfileDto.ProfileSummary profileSummary = new BlogProfileDto.ProfileSummary(
                profile == null ? null : profile.getDisplayName(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getAvatarUrl(),
                profile == null ? null : profile.getWebsiteUrl(),
                profile == null ? null : profile.getLocation());

        BlogProfileDto.StatsSummary statsSummary = new BlogProfileDto.StatsSummary(publishedPostCount);
        BlogSettingsDto.Response blogSettings = blogSettingsService.getPublicSettings(user.getId());

        return new BlogProfileDto.PublicProfileResponse(
                "/" + user.getUsername(),
                normalizeBaseUrl(webBaseUrl) + "/" + user.getUsername(),
                userSummary,
                profileSummary,
                statsSummary,
                blogSettings,
                PageResponse.from(posts));
    }

    private CodedApiException blogProfileNotFound() {
        return new CodedApiException(
                "blog_profile_not_found",
                HttpStatus.NOT_FOUND,
                "블로그 프로필을 찾을 수 없습니다.");
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            return "";
        }
        String trimmed = rawBaseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
