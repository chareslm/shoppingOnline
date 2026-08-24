package com.chareslm.shopping.merchant.service.impl;

import com.chareslm.shopping.auth.entity.Role;
import com.chareslm.shopping.auth.entity.UserAccount;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.RoleMapper;
import com.chareslm.shopping.auth.mapper.UserAccountMapper;
import com.chareslm.shopping.auth.mapper.UserRoleMapper;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.merchant.dto.request.CreateShopStaffRequest;
import com.chareslm.shopping.merchant.dto.request.ShopStaffAuditRequest;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.entity.ShopStaff;
import com.chareslm.shopping.merchant.mapper.ShopMapper;
import com.chareslm.shopping.merchant.mapper.ShopStaffMapper;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.message.service.MailService;
import com.chareslm.shopping.user.mapper.UserPreferenceMapper;
import com.chareslm.shopping.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopStaffServiceImplTest {
    private MerchantShopQueryService shops;
    private ShopMapper shopMapper;
    private ShopStaffMapper staffMapper;
    private UserAccountMapper users;
    private RoleMapper roles;
    private MailService mail;
    private ShopStaffServiceImpl service;

    @BeforeEach
    void setUp() {
        shops = mock(MerchantShopQueryService.class);
        shopMapper = mock(ShopMapper.class);
        staffMapper = mock(ShopStaffMapper.class);
        users = mock(UserAccountMapper.class);
        roles = mock(RoleMapper.class);
        mail = mock(MailService.class);
        when(mail.isEnabled()).thenReturn(true);
        service = new ShopStaffServiceImpl(shops, shopMapper, staffMapper, users, roles, mock(UserRoleMapper.class),
                mock(UserProfileMapper.class), mock(UserPreferenceMapper.class), mock(RefreshTokenMapper.class),
                new BCryptPasswordEncoder(), mail, mock(AuditService.class));
    }

    @Test
    void createSubmitsPendingAuditWithoutMail() {
        Shop shop = new Shop();
        shop.setId(8L);
        shop.setName("Demo Shop");
        shop.setStatus("OPEN");
        when(shops.requireOpenShop(3L)).thenReturn(shop);
        when(shopMapper.selectById(8L)).thenReturn(shop);
        when(users.selectByLoginIdentifier(anyString())).thenReturn(null);
        Role role = new Role();
        role.setId(7L);
        role.setCode("CUSTOMER_SERVICE");
        when(roles.selectActiveByCode("CUSTOMER_SERVICE")).thenReturn(role);
        when(users.insert(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0, UserAccount.class);
            account.setId(21L);
            assertEquals("DISABLED", account.getStatus());
            return 1;
        });
        when(staffMapper.insert(any(ShopStaff.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ShopStaff.class).setId(31L);
            return 1;
        });

        var created = service.create(3L, new CreateShopStaffRequest("cs@example.com", "客服小王", "cs_wang"));

        assertEquals("客服小王", created.displayName());
        assertEquals("Demo Shop", created.shopName());
        assertEquals("PENDING_AUDIT", created.status());
        verify(mail, never()).sendCustomerServiceCredential(anyString(), anyString(), anyString(), anyString());
        verify(users).insert(any(UserAccount.class));
    }

    @Test
    void adminApproveActivatesAccountAndSendsMail() {
        ShopStaff staff = new ShopStaff();
        staff.setId(31L);
        staff.setShopId(8L);
        staff.setUserId(21L);
        staff.setDisplayName("客服小王");
        staff.setStatus("PENDING_AUDIT");
        Shop shop = new Shop();
        shop.setId(8L);
        shop.setName("Demo Shop");
        UserAccount user = new UserAccount();
        user.setId(21L);
        user.setEmail("cs@example.com");
        user.setUsername("cs_wang");
        when(staffMapper.selectById(31L)).thenReturn(staff);
        when(shopMapper.selectById(8L)).thenReturn(shop);
        when(users.selectById(21L)).thenReturn(user);

        var approved = service.audit(9L, 31L, new ShopStaffAuditRequest("APPROVE", "ok"));

        assertEquals("ACTIVE", approved.status());
        assertEquals("Demo Shop", approved.shopName());
        verify(users).updateStatus(21L, "ACTIVE");
        verify(mail).sendCustomerServiceCredential(eq("cs@example.com"), eq("Demo Shop"), eq("cs_wang"), anyString());
    }
}
