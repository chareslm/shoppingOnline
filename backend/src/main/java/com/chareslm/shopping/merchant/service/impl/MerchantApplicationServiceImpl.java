package com.chareslm.shopping.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.dto.MerchantDtos;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationCreatedResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationDetailResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationRequest;
import com.chareslm.shopping.merchant.dto.MerchantDtos.ApplicationSummaryResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.AuditRequest;
import com.chareslm.shopping.merchant.dto.MerchantDtos.FileResponse;
import com.chareslm.shopping.merchant.dto.MerchantDtos.MerchantType;
import com.chareslm.shopping.merchant.entity.MerchantApplication;
import com.chareslm.shopping.merchant.entity.MerchantQualificationFile;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.mapper.MerchantApplicationMapper;
import com.chareslm.shopping.merchant.mapper.MerchantQualificationFileMapper;
import com.chareslm.shopping.merchant.mapper.ShopMapper;
import com.chareslm.shopping.merchant.service.MerchantApplicationService;
import com.chareslm.shopping.merchant.service.QualificationFileStorage;
import com.chareslm.shopping.message.service.MailService;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 商家入驻应用服务：协调文件存储、审核状态机、账号/店铺建档、审计与邮件通知。
 */
@Service
public class MerchantApplicationServiceImpl implements MerchantApplicationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SPECIAL = "!@#$%^&*".toCharArray();

    private final MerchantApplicationMapper applicationMapper;
    private final MerchantQualificationFileMapper fileMapper;
    private final ShopMapper shopMapper;
    private final UserAccountMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserProfileMapper profileMapper;
    private final UserPreferenceMapper preferenceMapper;
    private final QualificationFileStorage storage;
    private final MailService mailService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public MerchantApplicationServiceImpl(
            MerchantApplicationMapper applicationMapper, MerchantQualificationFileMapper fileMapper,
            ShopMapper shopMapper, UserAccountMapper userMapper, RoleMapper roleMapper,
            UserRoleMapper userRoleMapper, UserProfileMapper profileMapper,
            UserPreferenceMapper preferenceMapper, QualificationFileStorage storage,
            MailService mailService, AuditService auditService, PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate) {
        this.applicationMapper = applicationMapper;
        this.fileMapper = fileMapper;
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.profileMapper = profileMapper;
        this.preferenceMapper = preferenceMapper;
        this.storage = storage;
        this.mailService = mailService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public ApplicationCreatedResponse submit(ApplicationRequest request, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.MERCHANT_FILE_INVALID);
        }
        List<QualificationFileStorage.StoredFile> stored = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                stored.add(storage.store(file));
            }
        } catch (RuntimeException exception) {
            stored.forEach(item -> storage.deleteQuietly(item.storageKey()));
            throw exception;
        }
        try {
            MerchantApplication application = transactionTemplate.execute(status -> {
                MerchantApplication entity = toEntity(request);
                applicationMapper.insert(entity);
                for (QualificationFileStorage.StoredFile item : stored) {
                    MerchantQualificationFile file = new MerchantQualificationFile();
                    file.setApplicationId(entity.getId());
                    file.setOriginalName(item.originalName());
                    file.setStorageKey(item.storageKey());
                    file.setContentType(item.contentType());
                    file.setFileSize(item.size());
                    fileMapper.insert(file);
                }
                return entity;
            });
            return new ApplicationCreatedResponse(application.getId(), application.getStatus());
        } catch (DuplicateKeyException exception) {
            stored.forEach(item -> storage.deleteQuietly(item.storageKey()));
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        } catch (RuntimeException exception) {
            stored.forEach(item -> storage.deleteQuietly(item.storageKey()));
            throw exception;
        }
    }

    @Override
    public PageResponse<ApplicationSummaryResponse> list(String status, int page, int pageSize) {
        String normalized = trimToNull(status);
        List<ApplicationSummaryResponse> items = applicationMapper
                .selectPage(normalized, (page - 1) * pageSize, pageSize).stream()
                .map(item -> new ApplicationSummaryResponse(item.getId(), MerchantType.valueOf(item.getMerchantType()),
                        item.getShopName(), maskPhone(item.getContactPhone()), maskEmail(item.getContactEmail()),
                        item.getStatus(), item.getEmailDeliveryStatus(), item.getCreatedAt()))
                .toList();
        return new PageResponse<>(items, applicationMapper.countPage(normalized), page, pageSize);
    }

    @Override
    public ApplicationDetailResponse detail(Long id) {
        MerchantApplication item = requireApplication(id);
        List<FileResponse> files = fileMapper.selectByApplicationId(id).stream()
                .map(file -> new FileResponse(file.getId(), file.getOriginalName(), file.getContentType(),
                        file.getFileSize()))
                .toList();
        return new ApplicationDetailResponse(item.getId(), MerchantType.valueOf(item.getMerchantType()),
                item.getShopName(), item.getSubjectName(), item.getUnifiedSocialCreditCode(),
                item.getResponsiblePersonName(), item.getIdentityDocumentType(),
                maskIdentity(item.getIdentityDocumentNumber()), item.getContactPhone(), item.getContactEmail(),
                item.getStatus(), item.getRejectionReason(), item.getAccountUserId(), item.getAccountReused(),
                item.getEmailDeliveryStatus(), item.getCreatedAt(), files);
    }

    @Override
    public void auditQualification(Long id, AuditRequest request, Long auditorId) {
        requireApplication(id);
        String next = request.approved() ? "QUALIFICATION_APPROVED" : "REJECTED";
        // SQL 同时校验旧状态，受影响行数为 0 表示重复审核或并发状态迁移，按冲突处理。
        if (applicationMapper.auditQualification(id, next, trimToNull(request.reason()), auditorId) != 1) {
            auditService.record(auditorId, "MERCHANT", "QUALIFICATION_AUDIT", "MERCHANT_APPLICATION",
                    id.toString(), false);
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        auditService.record(auditorId, "MERCHANT", "QUALIFICATION_AUDIT", "MERCHANT_APPLICATION",
                id.toString(), true);
    }

    @Override
    public void auditAccount(Long id, AuditRequest request, Long auditorId) {
        if (!request.approved()) {
            requireApplication(id);
            if (applicationMapper.rejectAccount(id, trimToNull(request.reason()), auditorId) != 1) {
                auditService.record(auditorId, "MERCHANT", "ACCOUNT_AUDIT", "MERCHANT_APPLICATION",
                        id.toString(), false);
                throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
            }
            auditService.record(auditorId, "MERCHANT", "ACCOUNT_AUDIT", "MERCHANT_APPLICATION",
                    id.toString(), true);
            return;
        }

        ProvisionedAccount provisioned;
        try {
            provisioned = transactionTemplate.execute(status -> provisionAccount(id, auditorId));
        } catch (RuntimeException exception) {
            auditService.record(auditorId, "MERCHANT", "ACCOUNT_AUDIT", "MERCHANT_APPLICATION",
                    id.toString(), false);
            if (exception instanceof DuplicateKeyException) {
                throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
            }
            throw exception;
        }
        auditService.record(auditorId, "MERCHANT", "ACCOUNT_AUDIT", "MERCHANT_APPLICATION", id.toString(), true);
        deliverEmail(id, provisioned);
    }

    @Override
    public void retryCredentialEmail(Long id, Long auditorId) {
        MerchantApplication application = requireApplication(id);
        if (!"ACCOUNT_APPROVED".equals(application.getStatus())
                || !"MAIL_FAILED".equals(application.getEmailDeliveryStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        String temporaryPassword = null;
        if (!Boolean.TRUE.equals(application.getAccountReused())) {
            temporaryPassword = generatePassword();
            userMapper.updateTemporaryPassword(application.getAccountUserId(), passwordEncoder.encode(temporaryPassword));
        }
        deliverEmail(id, new ProvisionedAccount(application.getAccountUserId(),
                Boolean.TRUE.equals(application.getAccountReused()), temporaryPassword,
                application.getContactEmail(), application.getShopName()));
        auditService.record(auditorId, "MERCHANT", "CREDENTIAL_EMAIL_RETRY", "MERCHANT_APPLICATION",
                id.toString(), true);
    }

    @Override
    public DownloadedFile download(Long applicationId, Long fileId) {
        MerchantQualificationFile file = fileMapper.selectOwnedFile(applicationId, fileId);
        if (file == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        Resource resource = storage.load(file.getStorageKey());
        return new DownloadedFile(resource, file.getOriginalName(), file.getContentType());
    }

    private ProvisionedAccount provisionAccount(Long id, Long auditorId) {
        MerchantApplication application = requireApplication(id);
        if (!"QUALIFICATION_APPROVED".equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        UserAccount user = userMapper.selectByLoginIdentifier(application.getContactEmail());
        boolean reused = user != null;
        String temporaryPassword = null;
        if (user == null) {
            temporaryPassword = generatePassword();
            user = new UserAccount();
            user.setEmail(application.getContactEmail());
            user.setPhone(application.getContactPhone());
            user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            user.setMustChangePassword(true);
            user.setStatus("ACTIVE");
            userMapper.insert(user);

            UserProfile profile = new UserProfile();
            profile.setUserId(user.getId());
            profile.setNickname(application.getResponsiblePersonName());
            profile.setRealName(application.getResponsiblePersonName());
            profileMapper.insert(profile);
            UserPreference preference = new UserPreference();
            preference.setUserId(user.getId());
            preferenceMapper.insert(preference);
        }
        addMerchantOwnerRole(user.getId(), auditorId);

        Shop shop = new Shop();
        shop.setOwnerUserId(user.getId());
        shop.setApplicationId(application.getId());
        shop.setName(application.getShopName());
        shop.setStatus("OPEN");
        shopMapper.insert(shop);

        // 最终状态迁移使用 CAS；若并发审核已抢先完成，事务回滚账号、角色和店铺写入。
        if (applicationMapper.approveAccount(id, user.getId(), reused, auditorId) != 1) {
            throw new BusinessException(ErrorCode.MERCHANT_APPLICATION_CONFLICT);
        }
        return new ProvisionedAccount(user.getId(), reused, temporaryPassword,
                application.getContactEmail(), application.getShopName());
    }

    private void addMerchantOwnerRole(Long userId, Long auditorId) {
        Role role = roleMapper.selectActiveByCode("MERCHANT_OWNER");
        if (role == null) throw new IllegalStateException("MERCHANT_OWNER role is missing");
        long count = userRoleMapper.selectCount(new QueryWrapper<UserRole>()
                .eq("user_id", userId).eq("role_id", role.getId()));
        if (count == 0) {
            UserRole relation = new UserRole();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            relation.setGrantedBy(auditorId);
            userRoleMapper.insert(relation);
        }
    }

    private void deliverEmail(Long applicationId, ProvisionedAccount provisioned) {
        try {
            if (provisioned.reused()) {
                mailService.sendMerchantEnabledNotice(provisioned.email(), provisioned.shopName());
            } else {
                mailService.sendMerchantCredential(provisioned.email(), provisioned.shopName(),
                        provisioned.temporaryPassword());
            }
            applicationMapper.updateEmailStatus(applicationId, "SENT");
        } catch (RuntimeException exception) {
            // SMTP 属于事务提交后的外部副作用：失败只记录可重试状态，不回滚已开通账号和店铺。
            applicationMapper.updateEmailStatus(applicationId, "MAIL_FAILED");
        }
    }

    private MerchantApplication toEntity(ApplicationRequest request) {
        MerchantApplication entity = new MerchantApplication();
        entity.setMerchantType(request.merchantType().name());
        entity.setShopName(request.shopName().trim());
        entity.setSubjectName(trimToNull(request.subjectName()));
        entity.setUnifiedSocialCreditCode(upper(request.unifiedSocialCreditCode()));
        entity.setResponsiblePersonName(request.responsiblePersonName().trim());
        entity.setIdentityDocumentType(request.identityDocumentType().trim().toUpperCase(Locale.ROOT));
        entity.setIdentityDocumentNumber(request.identityDocumentNumber().trim());
        entity.setContactPhone(request.contactPhone().trim());
        entity.setContactEmail(request.contactEmail().trim().toLowerCase(Locale.ROOT));
        entity.setStatus("SUBMITTED");
        entity.setEmailDeliveryStatus("PENDING");
        return entity;
    }

    private MerchantApplication requireApplication(Long id) {
        MerchantApplication application = applicationMapper.selectById(id);
        if (application == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return application;
    }

    private static String generatePassword() {
        List<Character> chars = new ArrayList<>();
        chars.add(random(LOWER));
        chars.add(random(UPPER));
        chars.add(random(DIGITS));
        chars.add(random(SPECIAL));
        char[] all = (new String(LOWER) + new String(UPPER) + new String(DIGITS) + new String(SPECIAL)).toCharArray();
        while (chars.size() < 20) chars.add(random(all));
        Collections.shuffle(chars, RANDOM);
        StringBuilder result = new StringBuilder(chars.size());
        chars.forEach(result::append);
        return result.toString();
    }

    private static char random(char[] source) {
        return source[RANDOM.nextInt(source.length)];
    }

    private static String maskIdentity(String value) {
        if (value == null || value.length() <= 4) return "****";
        return value.substring(0, 2) + "*".repeat(Math.max(4, value.length() - 4))
                + value.substring(value.length() - 2);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, 1) + "***" + email.substring(at);
    }

    private static String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upper(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private record ProvisionedAccount(Long userId, boolean reused, String temporaryPassword,
                                      String email, String shopName) {
    }
}
