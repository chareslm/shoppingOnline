package com.chareslm.shopping.merchant.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 商家入驻模块的请求、响应及受限枚举类型集合。
 */
public final class MerchantDtos {
    private MerchantDtos() {
    }

    public record ApplicationRequest(
            @NotNull MerchantType merchantType,
            @NotBlank @Size(max = 128) String shopName,
            @Size(max = 128) String subjectName,
            @Size(max = 32) String unifiedSocialCreditCode,
            @NotBlank @Size(max = 64) String responsiblePersonName,
            @NotBlank @Size(max = 32) String identityDocumentType,
            @NotBlank @Size(max = 64) String identityDocumentNumber,
            @NotBlank @Pattern(regexp = "^1\\d{10}$") String contactPhone,
            @NotBlank @Email @Size(max = 254) String contactEmail
    ) {
        @AssertTrue(message = "subject name and social credit code are required for this merchant type")
        public boolean hasRequiredSubjectIdentity() {
            return merchantType == null || merchantType == MerchantType.INDIVIDUAL
                    || (hasText(subjectName) && hasText(unifiedSocialCreditCode));
        }
    }

    public record AuditRequest(@NotNull Boolean approved, @Size(max = 500) String reason) {
        @AssertTrue(message = "rejection reason is required")
        public boolean hasReasonWhenRejected() {
            return approved == null || approved || hasText(reason);
        }
    }

    public record ApplicationCreatedResponse(Long id, String status) {
    }

    public record ApplicationSummaryResponse(Long id, MerchantType merchantType, String shopName,
                                             String contactPhone, String contactEmail, String status,
                                             String emailDeliveryStatus, LocalDateTime createdAt) {
    }

    public record ApplicationDetailResponse(Long id, MerchantType merchantType, String shopName,
                                            String subjectName, String unifiedSocialCreditCode,
                                            String responsiblePersonName,
                                            String identityDocumentType, String maskedIdentityDocumentNumber,
                                            String contactPhone, String contactEmail, String status,
                                            String rejectionReason, Long accountUserId, Boolean accountReused,
                                            String emailDeliveryStatus, LocalDateTime createdAt,
                                            List<FileResponse> files) {
    }

    public record FileResponse(Long id, String originalName, String contentType, Long fileSize) {
    }

    public enum MerchantType {
        ENTERPRISE, SOLE_PROPRIETOR, INDIVIDUAL
    }

    public static boolean isPendingStatus(String status) {
        return Set.of("SUBMITTED", "QUALIFICATION_APPROVED").contains(status);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
