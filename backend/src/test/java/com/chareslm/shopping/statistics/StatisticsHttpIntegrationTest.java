package com.chareslm.shopping.statistics;

import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.security.context.LoginUser;
import com.chareslm.shopping.statistics.controller.MerchantStatisticsController;
import com.chareslm.shopping.statistics.controller.PlatformStatisticsController;
import com.chareslm.shopping.statistics.controller.SelfStatisticsController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class StatisticsHttpIntegrationTest {
    private static final LocalDateTime START_AT = LocalDateTime.of(2098, 8, 1, 0, 0);
    private static final LocalDateTime END_AT = LocalDateTime.of(2098, 8, 3, 0, 0);

    @Autowired
    private PlatformStatisticsController platformStatisticsController;

    @Autowired
    private MerchantStatisticsController merchantStatisticsController;

    @Autowired
    private SelfStatisticsController selfStatisticsController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformStatisticsRequireDedicatedAuthority() {
        authenticate(Set.of());
        assertThrows(AccessDeniedException.class,
                () -> platformStatisticsController.overview(START_AT, END_AT, "Asia/Shanghai", "DAY"));

        authenticate(Set.of("statistics:platform:view"));
        var response = platformStatisticsController.overview(START_AT, END_AT, "Asia/Shanghai", "DAY");
        assertEquals("v1", response.data().metricVersion());
        assertEquals("Asia/Shanghai", response.data().timezone());
    }

    @Test
    void merchantAuthorityStillRequiresAnOwnedOpenShop() {
        authenticate(Set.of("statistics:shop:view"));
        BusinessException exception = assertThrows(BusinessException.class,
                () -> merchantStatisticsController.overview(START_AT, END_AT, "Asia/Shanghai", "DAY"));
        assertEquals(40301, exception.getCode());
    }

    @Test
    void invalidRangeIsRejectedForAuthorizedCaller() {
        authenticate(Set.of("statistics:platform:view"));
        BusinessException exception = assertThrows(BusinessException.class,
                () -> platformStatisticsController.trends(START_AT,
                        LocalDateTime.of(2098, 9, 3, 0, 0), "Asia/Shanghai", "DAY"));
        assertEquals(40001, exception.getCode());
    }

    @Test
    void selfStatisticsRequireDedicatedAuthorityAndUseCurrentPrincipal() {
        authenticate(Set.of());
        assertThrows(AccessDeniedException.class,
                () -> selfStatisticsController.overview(START_AT, END_AT, "Asia/Shanghai", "DAY"));

        authenticate(Set.of("statistics:self:view"));
        var response = selfStatisticsController.overview(START_AT, END_AT, "Asia/Shanghai", "DAY");
        assertEquals("v1", response.data().metricVersion());
        assertEquals(0, response.data().metrics().paidOrderCount());
    }

    private void authenticate(Set<String> permissions) {
        LoginUser principal = new LoginUser(919_999L, "statistics_test", Set.of("SUPER_ADMIN"),
                permissions, false, 1L);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
