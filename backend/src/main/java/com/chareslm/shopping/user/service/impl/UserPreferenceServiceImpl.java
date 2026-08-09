package com.chareslm.shopping.user.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chareslm.shopping.user.dto.request.UpdateUserPreferenceRequest;
import com.chareslm.shopping.user.dto.response.UserPreferenceResponse;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.service.UserPreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {
    private final UserPreferenceMapper userPreferenceMapper;
    private final ObjectMapper objectMapper;

    public UserPreferenceServiceImpl(UserPreferenceMapper userPreferenceMapper, ObjectMapper objectMapper) {
        this.userPreferenceMapper = userPreferenceMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UserPreferenceResponse getPreference(Long userId) { return toResponse(getOrCreate(userId)); }

    @Override
    @Transactional
    public UserPreferenceResponse updatePreference(Long userId, UpdateUserPreferenceRequest request) {
        UserPreference preference = getOrCreate(userId);
        preference.setMarketingEnabled(request.marketingEnabled());
        preference.setOrderNotificationEnabled(request.orderNotificationEnabled());
        preference.setSystemNotificationEnabled(request.systemNotificationEnabled());
        preference.setExtraPreferences(serialize(request.extraPreferences()));
        userPreferenceMapper.updateById(preference);
        return toResponse(preference);
    }

    private UserPreference getOrCreate(Long userId) {
        UserPreference preference = userPreferenceMapper.selectById(userId);
        if (preference != null) return preference;
        preference = new UserPreference();
        preference.setUserId(userId);
        preference.setMarketingEnabled(true);
        preference.setOrderNotificationEnabled(true);
        preference.setSystemNotificationEnabled(true);
        userPreferenceMapper.insert(preference);
        return preference;
    }

    private UserPreferenceResponse toResponse(UserPreference preference) {
        return new UserPreferenceResponse(preference.getUserId(), Boolean.TRUE.equals(preference.getMarketingEnabled()), Boolean.TRUE.equals(preference.getOrderNotificationEnabled()), Boolean.TRUE.equals(preference.getSystemNotificationEnabled()), deserialize(preference.getExtraPreferences()));
    }

    private String serialize(Map<String, Object> preferences) {
        if (preferences == null || preferences.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(preferences); }
        catch (Exception exception) { throw new IllegalArgumentException("extraPreferences must be JSON serializable", exception); }
    }

    private Map<String, Object> deserialize(String preferences) {
        if (preferences == null || preferences.isBlank()) return Map.of();
        try { return Collections.unmodifiableMap(objectMapper.readValue(preferences, new TypeReference<LinkedHashMap<String, Object>>() { })); }
        catch (Exception exception) { throw new IllegalStateException("Stored user preferences are invalid JSON", exception); }
    }
}
