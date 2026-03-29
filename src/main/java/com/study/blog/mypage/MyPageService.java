package com.study.blog.mypage;

import com.study.blog.comment.CommentService;
import com.study.blog.comment.dto.CommentDto;
import com.study.blog.like.PostLikeRepository;
import com.study.blog.mypage.dto.MyPageDto;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostService;
import com.study.blog.post.dto.PostDto;
import com.study.blog.user.User;
import com.study.blog.user.UserProfile;
import com.study.blog.user.UserProfileRepository;
import com.study.blog.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MyPageService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostService postService;
    private final CommentService commentService;

    public MyPageService(UserRepository userRepository,
                         UserProfileRepository userProfileRepository,
                         PostRepository postRepository,
                         PostLikeRepository postLikeRepository,
                         PostService postService,
                         CommentService commentService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postService = postService;
        this.commentService = commentService;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public MyPageDto.SummaryResponse getSummary(Long userId) {
        User user = getUserOrThrow(userId);
        UserProfile profile = userProfileRepository.findByUser_Id(userId).orElse(null);
        return toSummaryResponse(user, profile);
    }

    public MyPageDto.SummaryResponse upsertProfile(Long userId, MyPageDto.UpdateProfileRequest req) {
        User user = getUserOrThrow(userId);
        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> UserProfile.builder().user(user).createdAt(LocalDateTime.now()).build());

        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName().trim());
        }
        if (req.getNickname() != null) {
            String nickname = trimToNull(req.getNickname());
            if (nickname != null && !nickname.equals(user.getNickname()) && userRepository.existsByNickname(nickname)) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            if (nickname != null) {
                user.setNickname(nickname);
            }
        }
        if (req.getEmail() != null) {
            String email = trimToNull(req.getEmail());
            String currentEmail = user.getEmail();
            if (email != null && (currentEmail == null || !email.equalsIgnoreCase(currentEmail))
                    && userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }
            user.setEmail(email);
            user.setEmailVerifiedAt(null);
        }
        if (req.getPhoneNumber() != null) {
            String phoneNumber = normalizePhone(req.getPhoneNumber());
            if (phoneNumber != null && !phoneNumber.equals(user.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
            }
            user.setPhoneNumber(phoneNumber);
            user.setPhoneVerifiedAt(null);
        }
        if (req.getDisplayName() != null) {
            profile.setDisplayName(trimToNull(req.getDisplayName()));
        }
        if (req.getBio() != null) {
            profile.setBio(trimToNull(req.getBio()));
        }
        if (req.getAvatarUrl() != null) {
            profile.setAvatarUrl(trimToNull(req.getAvatarUrl()));
        }
        if (req.getWebsiteUrl() != null) {
            profile.setWebsiteUrl(trimToNull(req.getWebsiteUrl()));
        }
        if (req.getLocation() != null) {
            profile.setLocation(trimToNull(req.getLocation()));
        }
        profile.setUpdatedAt(LocalDateTime.now());

        userProfileRepository.save(profile);
        return toSummaryResponse(user, profile);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<PostDto.Response> getMyPosts(Long userId) {
        return postService.listByUser(userId);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CommentDto.Response> getMyComments(Long userId) {
        return commentService.listByUser(userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private MyPageDto.SummaryResponse toSummaryResponse(User user, UserProfile profile) {
        MyPageDto.SummaryResponse response = new MyPageDto.SummaryResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setNickname(user.getNickname());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());

        MyPageDto.ProfileResponse profileResponse = new MyPageDto.ProfileResponse();
        if (profile != null) {
            profileResponse.setDisplayName(profile.getDisplayName());
            profileResponse.setBio(profile.getBio());
            profileResponse.setAvatarUrl(profile.getAvatarUrl());
            profileResponse.setWebsiteUrl(profile.getWebsiteUrl());
            profileResponse.setLocation(profile.getLocation());
        }
        response.setProfile(profileResponse);

        MyPageDto.StatsResponse stats = new MyPageDto.StatsResponse();
        stats.setPostCount(normalize(postRepository.countByUser_IdAndDeletedYn(user.getId(), "N")));
        stats.setCommentCount(normalize(commentService.getCommentCountByUser(user.getId())));
        stats.setLikedPostCount(normalize(postLikeRepository.countByUser_IdAndDeletedYn(user.getId(), "N")));
        response.setStats(stats);

        return response;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePhone(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("[^0-9+]", "");
        return digits.isBlank() ? null : digits;
    }

    private long normalize(Long value) {
        return value == null ? 0L : value;
    }
}
