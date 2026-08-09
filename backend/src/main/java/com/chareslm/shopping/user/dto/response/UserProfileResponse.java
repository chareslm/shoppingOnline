package com.chareslm.shopping.user.dto.response;

import java.time.LocalDate;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String avatarUrl,
        String realName,
        String gender,
        LocalDate birthday,
        String bio
) {
}
