package com.study.blog.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserAvatarService {

    private final UserProfileRepository userProfileRepository;

    public UserAvatarService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public String getAvatarUrl(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileRepository.findByUser_Id(userId)
                .map(UserProfile::getAvatarUrl)
                .orElse(null);
    }

    public Map<Long, String> getAvatarUrls(Collection<Long> userIds) {
        Set<Long> normalizedIds = userIds == null
                ? Set.of()
                : userIds.stream().filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> avatarUrls = new LinkedHashMap<>();
        userProfileRepository.findByUser_IdIn(normalizedIds).forEach(profile -> {
            if (profile.getUser() != null && profile.getUser().getId() != null) {
                avatarUrls.put(profile.getUser().getId(), profile.getAvatarUrl());
            }
        });
        return avatarUrls;
    }
}
