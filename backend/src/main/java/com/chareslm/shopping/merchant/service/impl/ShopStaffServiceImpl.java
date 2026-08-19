package com.chareslm.shopping.merchant.service.impl;

import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.dto.request.CreateShopStaffRequest;
import com.chareslm.shopping.merchant.dto.request.ShopStaffAuditRequest;
import com.chareslm.shopping.merchant.dto.response.ShopStaffResponse;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.entity.ShopStaff;
import com.chareslm.shopping.merchant.mapper.ShopMapper;
import com.chareslm.shopping.merchant.mapper.ShopStaffMapper;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.merchant.service.ShopStaffService;
import com.chareslm.shopping.merchant.util.TemporaryPasswords;
import com.chareslm.shopping.message.service.MailService;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShopStaffServiceImpl implements ShopStaffService {
    private final MerchantShopQueryService merchantShopQueryService;
    private final ShopMapper shopMapper;
    private final ShopStaffMapper shopStaffMapper;
    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuditService auditService;

    @Override
    public List<ShopStaffResponse> list(Long ownerUserId) {
        Shop shop = merchantShopQueryService.requireOpenShop(ownerUserId);
        return shopStaffMapper.selectByShopId(shop.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ShopStaffResponse create(Long ownerUserId, CreateShopStaffRequest request) {
        Shop shop = merchantShopQueryService.requireOpenShop(ownerUserId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = StringUtils.hasText(request.username()) ? request.username().trim() : null;
        if (identifierTaken(username, email)) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        Role role = roleMapper.selectActiveByCode("CUSTOMER_SERVICE");
        if (role == null) {
            throw new IllegalStateException("CUSTOMER_SERVICE role is missing");
        }
        String placeholderPassword = TemporaryPasswords.issue(false);
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(placeholderPassword));
        user.setMustChangePassword(true);
        user.setStatus("DISABLED");
        try {
            userAccountMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(request.displayName().trim());
        userProfileMapper.insert(profile);
        UserPreference preference = new UserPreference();
        preference.setUserId(user.getId());
        userPreferenceMapper.insert(preference);

        UserRole relation = new UserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(role.getId());
        relation.setGrantedBy(ownerUserId);
        userRoleMapper.insert(relation);

        ShopStaff staff = new ShopStaff();
        staff.setShopId(shop.getId());
        staff.setUserId(user.getId());
        staff.setDisplayName(request.displayName().trim());
        staff.setStatus("PENDING_AUDIT");
        staff.setEmailDeliveryStatus("PENDING");
        shopStaffMapper.insert(staff);
        auditService.record(ownerUserId, "MERCHANT", "STAFF_CREATE", "SHOP_STAFF", staff.getId().toString(), true);
        return toResponse(staff, user);
    }

    @Override
    @Transactional
    public ShopStaffResponse retryCredentialEmail(Long ownerUserId, Long staffId) {
        Shop shop = merchantShopQueryService.requireOpenShop(ownerUserId);
        ShopStaff staff = requireOwnedStaff(shop.getId(), staffId);
        if (!"ACTIVE".equals(staff.getStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        return rotateAndDeliver(ownerUserId, staff, "STAFF_CREDENTIAL_RETRY");
    }

    @Override
    public List<ShopStaffResponse> listForAdmin(String status) {
        return shopStaffMapper.selectByStatus(status).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ShopStaffResponse audit(Long operatorId, Long staffId, ShopStaffAuditRequest request) {
        ShopStaff staff = requireStaff(staffId);
        if (!"PENDING_AUDIT".equals(staff.getStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        if ("REJECT".equals(request.result())) {
            staff.setStatus("REJECTED");
            staff.setAuditRemark(trimToNull(request.remark()));
            shopStaffMapper.updateById(staff);
            auditService.record(operatorId, "MERCHANT", "STAFF_AUDIT_REJECT", "SHOP_STAFF", staffId.toString(), true);
            return toResponse(staff);
        }
        Shop shop = requireShop(staff.getShopId());
        UserAccount user = requireUser(staff.getUserId());
        String temporaryPassword = TemporaryPasswords.issue(mailService.isEnabled());
        userAccountMapper.updateTemporaryPassword(user.getId(), passwordEncoder.encode(temporaryPassword));
        userAccountMapper.updateStatus(user.getId(), "ACTIVE");
        staff.setStatus("ACTIVE");
        staff.setAuditRemark(trimToNull(request.remark()));
        shopStaffMapper.updateById(staff);
        String mailStatus = deliver(user.getEmail(), shop.getName(), loginHint(user), temporaryPassword);
        staff.setEmailDeliveryStatus(mailStatus);
        shopStaffMapper.updateEmailStatus(staff.getId(), mailStatus);
        auditService.record(operatorId, "MERCHANT", "STAFF_AUDIT_APPROVE", "SHOP_STAFF", staffId.toString(), true);
        return toResponse(staff, userAccountMapper.selectById(user.getId()));
    }

    @Override
    @Transactional
    public ShopStaffResponse revoke(Long operatorId, Long staffId, String remark) {
        ShopStaff staff = requireStaff(staffId);
        if (!"ACTIVE".equals(staff.getStatus()) && !"DISABLED".equals(staff.getStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        UserAccount user = requireUser(staff.getUserId());
        staff.setStatus("REVOKED");
        staff.setAuditRemark(trimToNull(remark));
        shopStaffMapper.updateById(staff);
        userAccountMapper.updateStatus(user.getId(), "DISABLED");
        refreshTokenMapper.revokeActiveByUserId(user.getId(), "STAFF_REVOKED");
        auditService.record(operatorId, "MERCHANT", "STAFF_REVOKE", "SHOP_STAFF", staffId.toString(), true);
        return toResponse(staff, userAccountMapper.selectById(user.getId()));
    }

    @Override
    @Transactional
    public ShopStaffResponse restore(Long operatorId, Long staffId, String remark) {
        ShopStaff staff = requireStaff(staffId);
        if (!"REVOKED".equals(staff.getStatus()) && !"REJECTED".equals(staff.getStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        UserAccount user = requireUser(staff.getUserId());
        String temporaryPassword = TemporaryPasswords.issue(mailService.isEnabled());
        userAccountMapper.updateTemporaryPassword(user.getId(), passwordEncoder.encode(temporaryPassword));
        userAccountMapper.updateStatus(user.getId(), "ACTIVE");
        refreshTokenMapper.revokeActiveByUserId(user.getId(), "STAFF_RESTORED");
        staff.setStatus("ACTIVE");
        staff.setAuditRemark(trimToNull(remark));
        shopStaffMapper.updateById(staff);
        Shop shop = requireShop(staff.getShopId());
        String mailStatus = deliver(user.getEmail(), shop.getName(), loginHint(user), temporaryPassword);
        staff.setEmailDeliveryStatus(mailStatus);
        shopStaffMapper.updateEmailStatus(staff.getId(), mailStatus);
        auditService.record(operatorId, "MERCHANT", "STAFF_RESTORE", "SHOP_STAFF", staffId.toString(), true);
        return toResponse(staff, userAccountMapper.selectById(user.getId()));
    }

    @Override
    @Transactional
    public ShopStaffResponse retryCredentialEmailForAdmin(Long operatorId, Long staffId) {
        ShopStaff staff = requireStaff(staffId);
        if (!"ACTIVE".equals(staff.getStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        return rotateAndDeliver(operatorId, staff, "STAFF_CREDENTIAL_RETRY");
    }

    private ShopStaffResponse rotateAndDeliver(Long operatorId, ShopStaff staff, String action) {
        UserAccount user = requireUser(staff.getUserId());
        if (user.getEmail() == null || !Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String temporaryPassword = TemporaryPasswords.issue(mailService.isEnabled());
        userAccountMapper.updateTemporaryPassword(user.getId(), passwordEncoder.encode(temporaryPassword));
        refreshTokenMapper.revokeActiveByUserId(user.getId(), "TEMPORARY_PASSWORD_ROTATED");
        Shop shop = requireShop(staff.getShopId());
        String mailStatus = deliver(user.getEmail(), shop.getName(), loginHint(user), temporaryPassword);
        staff.setEmailDeliveryStatus(mailStatus);
        shopStaffMapper.updateEmailStatus(staff.getId(), mailStatus);
        auditService.record(operatorId, "MERCHANT", action, "SHOP_STAFF", staff.getId().toString(), true);
        return toResponse(staff, user);
    }

    private ShopStaff requireOwnedStaff(Long shopId, Long staffId) {
        ShopStaff staff = shopStaffMapper.selectByIdAndShopId(staffId, shopId);
        if (staff == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return staff;
    }

    private ShopStaff requireStaff(Long staffId) {
        ShopStaff staff = shopStaffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return staff;
    }

    private Shop requireShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return shop;
    }

    private UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return user;
    }

    private boolean identifierTaken(String username, String email) {
        return userAccountMapper.selectByLoginIdentifier(email) != null
                || (username != null && userAccountMapper.selectByLoginIdentifier(username) != null);
    }

    private String deliver(String email, String shopName, String loginHint, String temporaryPassword) {
        if (!mailService.isEnabled()) {
            return "SKIPPED";
        }
        try {
            mailService.sendCustomerServiceCredential(email, shopName, loginHint, temporaryPassword);
            return "SENT";
        } catch (RuntimeException exception) {
            return "MAIL_FAILED";
        }
    }

    private ShopStaffResponse toResponse(ShopStaff staff) {
        return toResponse(staff, userAccountMapper.selectById(staff.getUserId()));
    }

    private ShopStaffResponse toResponse(ShopStaff staff, UserAccount user) {
        String shopName = staff.getShopName();
        if (shopName == null && staff.getShopId() != null) {
            Shop shop = shopMapper.selectById(staff.getShopId());
            shopName = shop == null ? null : shop.getName();
        }
        String email = user == null ? null : user.getEmail();
        return new ShopStaffResponse(
                staff.getId(),
                staff.getShopId(),
                shopName,
                staff.getUserId(),
                staff.getDisplayName(),
                maskEmail(email),
                user == null ? null : user.getUsername(),
                staff.getStatus(),
                staff.getAuditRemark(),
                staff.getEmailDeliveryStatus(),
                user != null && Boolean.TRUE.equals(user.getMustChangePassword()),
                staff.getCreatedAt());
    }

    private static String loginHint(UserAccount user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }
}
