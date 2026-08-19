package com.chareslm.shopping.merchant.service.impl;

import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.entity.UserRole;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.dto.MerchantDtos.AuditRequest;
import com.chareslm.shopping.merchant.entity.MerchantApplication;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.mapper.MerchantApplicationMapper;
import com.chareslm.shopping.merchant.mapper.MerchantQualificationFileMapper;
import com.chareslm.shopping.merchant.mapper.ShopMapper;
import com.chareslm.shopping.merchant.service.QualificationFileStorage;
import com.chareslm.shopping.message.service.MailService;
import com.chareslm.shopping.user.entity.UserPreference;
import com.chareslm.shopping.user.entity.UserProfile;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 覆盖审核状态机 CAS、资质通过即开通、撤销/恢复及 SMTP 失败不回滚开通结果等关键边界。
 */
class MerchantApplicationServiceImplTest {
    private MerchantApplicationMapper applicationMapper;
    private ShopMapper shopMapper;
    private UserAccountMapper userMapper;
    private RoleMapper roleMapper;
    private UserRoleMapper userRoleMapper;
    private RefreshTokenMapper refreshTokenMapper;
    private UserProfileMapper profileMapper;
    private UserPreferenceMapper preferenceMapper;
    private MailService mailService;
    private MerchantApplicationServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        applicationMapper = mock(MerchantApplicationMapper.class);
        shopMapper = mock(ShopMapper.class);
        userMapper = mock(UserAccountMapper.class);
        roleMapper = mock(RoleMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        profileMapper = mock(UserProfileMapper.class);
        preferenceMapper = mock(UserPreferenceMapper.class);
        mailService = mock(MailService.class);
        when(mailService.isEnabled()).thenReturn(true);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any(TransactionCallback.class))).thenAnswer(invocation ->
                invocation.<TransactionCallback<Object>>getArgument(0)
                        .doInTransaction(mock(TransactionStatus.class)));
        service = new MerchantApplicationServiceImpl(applicationMapper,
                mock(MerchantQualificationFileMapper.class), shopMapper, userMapper, roleMapper, userRoleMapper,
                refreshTokenMapper, profileMapper, preferenceMapper, mock(QualificationFileStorage.class), mailService,
                mock(AuditService.class), new BCryptPasswordEncoder(), transactions);
    }

    @Test
    void qualificationApprovalProvisionsAccount() {
        UserAccount existing = new UserAccount();
        existing.setId(20L);
        when(applicationMapper.selectById(1L)).thenReturn(application("SUBMITTED"), application("QUALIFICATION_APPROVED"));
        when(applicationMapper.auditQualification(1L, "QUALIFICATION_APPROVED", null, 9L)).thenReturn(1);
        when(userMapper.selectByLoginIdentifier("owner@example.com")).thenReturn(existing);
        when(roleMapper.selectActiveByCode("MERCHANT_OWNER")).thenReturn(role());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);
        when(applicationMapper.approveAccount(1L, 20L, true, 9L)).thenReturn(1);

        service.auditQualification(1L, new AuditRequest(true, null), 9L);

        verify(applicationMapper).auditQualification(1L, "QUALIFICATION_APPROVED", null, 9L);
        verify(mailService).sendMerchantEnabledNotice("owner@example.com", "Demo Shop");
        verify(applicationMapper).updateEmailStatus(1L, "SENT");
    }

    @Test
    void leftoverQualificationApprovedStillProvisions() {
        UserAccount existing = new UserAccount();
        existing.setId(20L);
        when(applicationMapper.selectById(1L)).thenReturn(application("QUALIFICATION_APPROVED"));
        when(userMapper.selectByLoginIdentifier("owner@example.com")).thenReturn(existing);
        when(roleMapper.selectActiveByCode("MERCHANT_OWNER")).thenReturn(role());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);
        when(applicationMapper.approveAccount(1L, 20L, true, 9L)).thenReturn(1);

        service.auditQualification(1L, new AuditRequest(true, null), 9L);

        verify(applicationMapper, never()).auditQualification(eq(1L), eq("QUALIFICATION_APPROVED"), any(), eq(9L));
        verify(applicationMapper).approveAccount(1L, 20L, true, 9L);
    }

    @Test
    void repeatedQualificationAuditOnApprovedAccountIsRejected() {
        when(applicationMapper.selectById(1L)).thenReturn(application("ACCOUNT_APPROVED"));

        assertThrows(BusinessException.class,
                () -> service.auditQualification(1L, new AuditRequest(true, null), 9L));
    }

    @Test
    void accountApprovalReusesExistingEmailAccount() {
        MerchantApplication application = application("QUALIFICATION_APPROVED");
        UserAccount existing = new UserAccount();
        existing.setId(20L);
        when(applicationMapper.selectById(1L)).thenReturn(application);
        when(userMapper.selectByLoginIdentifier("owner@example.com")).thenReturn(existing);
        when(roleMapper.selectActiveByCode("MERCHANT_OWNER")).thenReturn(role());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);
        when(applicationMapper.approveAccount(1L, 20L, true, 9L)).thenReturn(1);

        service.auditAccount(1L, new AuditRequest(true, null), 9L);

        verify(userMapper, never()).insert(any(UserAccount.class));
        verify(mailService).sendMerchantEnabledNotice("owner@example.com", "Demo Shop");
        verify(applicationMapper).updateEmailStatus(1L, "SENT");
    }

    @Test
    void newAccountMailFailureDoesNotRollbackApproval() {
        MerchantApplication application = application("QUALIFICATION_APPROVED");
        when(applicationMapper.selectById(1L)).thenReturn(application);
        when(userMapper.selectByLoginIdentifier("owner@example.com")).thenReturn(null);
        when(userMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setId(21L);
            assertTrue(account.getMustChangePassword());
            return 1;
        });
        when(roleMapper.selectActiveByCode("MERCHANT_OWNER")).thenReturn(role());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);
        when(applicationMapper.approveAccount(1L, 21L, false, 9L)).thenReturn(1);
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp unavailable")).when(mailService)
                .sendMerchantCredential(eq("owner@example.com"), eq("Demo Shop"), any());

        service.auditAccount(1L, new AuditRequest(true, null), 9L);

        verify(profileMapper).insert(any(UserProfile.class));
        verify(preferenceMapper).insert(any(UserPreference.class));
        verify(applicationMapper).updateEmailStatus(1L, "MAIL_FAILED");
    }

    @Test
    void revokeSuspendsShopAndNotifiesOwner() {
        MerchantApplication application = application("ACCOUNT_APPROVED");
        Shop shop = shop("OPEN");
        when(applicationMapper.selectById(1L)).thenReturn(application);
        when(shopMapper.selectByApplicationId(1L)).thenReturn(shop);
        when(shopMapper.updateStatus(5L, "OPEN", "SUSPENDED")).thenReturn(1);

        service.revokeMerchant(1L, 9L);

        verify(userRoleMapper).deleteMerchantOwnerRoleByUserId(20L);
        verify(refreshTokenMapper).revokeActiveByUserId(20L, "MERCHANT_REVOKED");
        verify(mailService).sendMerchantRevokedNotice("owner@example.com", "Demo Shop");
        verify(applicationMapper).updateEmailStatus(1L, "SENT");
    }

    @Test
    void restoreReopensShopAndNotifiesOwner() {
        MerchantApplication application = application("ACCOUNT_APPROVED");
        Shop shop = shop("SUSPENDED");
        when(applicationMapper.selectById(1L)).thenReturn(application);
        when(shopMapper.selectByApplicationId(1L)).thenReturn(shop);
        when(shopMapper.updateStatus(5L, "SUSPENDED", "OPEN")).thenReturn(1);
        when(roleMapper.selectActiveByCode("MERCHANT_OWNER")).thenReturn(role());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        service.restoreMerchant(1L, 9L);

        verify(userRoleMapper).insert(any(UserRole.class));
        verify(mailService).sendMerchantRestoredNotice("owner@example.com", "Demo Shop");
        verify(applicationMapper).updateEmailStatus(1L, "SENT");
    }

    private MerchantApplication application(String status) {
        MerchantApplication application = new MerchantApplication();
        application.setId(1L);
        application.setStatus(status);
        application.setContactEmail("owner@example.com");
        application.setContactPhone("13800138000");
        application.setResponsiblePersonName("Owner");
        application.setShopName("Demo Shop");
        return application;
    }

    private Shop shop(String status) {
        Shop shop = new Shop();
        shop.setId(5L);
        shop.setOwnerUserId(20L);
        shop.setApplicationId(1L);
        shop.setName("Demo Shop");
        shop.setStatus(status);
        return shop;
    }

    private Role role() {
        Role role = new Role();
        role.setId(3L);
        return role;
    }
}
