package com.chareslm.shopping.user.service.impl;

import com.chareslm.shopping.user.dto.request.UpdateUserProfileRequest;
import com.chareslm.shopping.user.dto.response.UserProfileResponse;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import com.chareslm.shopping.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileMapper userProfileMapper;

    public UserProfileServiceImpl(UserProfileMapper userProfileMapper) { this.userProfileMapper = userProfileMapper; }

    @Override
    @Transactional
    public UserProfileResponse getProfile(Long userId) { return toResponse(getOrCreate(userId)); }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserProfile profile = getOrCreate(userId);
        if (request.nickname() != null) profile.setNickname(trimToNull(request.nickname()));
        if (request.avatarUrl() != null) profile.setAvatarUrl(trimToNull(request.avatarUrl()));
        if (request.realName() != null) profile.setRealName(trimToNull(request.realName()));
        if (request.gender() != null) profile.setGender(request.gender());
        if (request.birthday() != null) profile.setBirthday(request.birthday());
        if (request.bio() != null) profile.setBio(trimToNull(request.bio()));
        userProfileMapper.updateById(profile);
        return toResponse(profile);
    }

    private UserProfile getOrCreate(Long userId) {
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile != null) return profile;
        profile = new UserProfile();
        profile.setUserId(userId);
        profile.setGender("UNKNOWN");
        userProfileMapper.insert(profile);
        return profile;
    }

    private static UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(profile.getUserId(), profile.getNickname(), profile.getAvatarUrl(), profile.getRealName(), profile.getGender(), profile.getBirthday(), profile.getBio());
    }

    private static String trimToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
