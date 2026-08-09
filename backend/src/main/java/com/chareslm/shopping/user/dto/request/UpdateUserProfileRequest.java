package com.chareslm.shopping.user.dto.request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserProfileRequest(
        @Size(max = 64) String nickname,
        @Size(max = 512) String avatarUrl,
        @Size(max = 64) String realName,
        @Pattern(regexp = "UNKNOWN|MALE|FEMALE") String gender,
        @Past LocalDate birthday,
        @Size(max = 500) String bio
) {
}
