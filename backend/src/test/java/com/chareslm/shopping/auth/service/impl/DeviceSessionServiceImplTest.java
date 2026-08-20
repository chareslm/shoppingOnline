package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.response.DeviceSessionResponse;
import com.chareslm.shopping.auth.entity.UserDevice;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.UserDeviceMapper;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceSessionServiceImplTest {
    private UserDeviceMapper userDeviceMapper;
    private RefreshTokenMapper refreshTokenMapper;
    private AuditService auditService;
    private DeviceSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        userDeviceMapper = mock(UserDeviceMapper.class);
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        auditService = mock(AuditService.class);
        service = new DeviceSessionServiceImpl(userDeviceMapper, refreshTokenMapper, auditService);
    }

    @Test
    void listDevicesReturnsOnlyRequestedUsersDevicesAndMarksCurrentSession() {
        LocalDateTime now = LocalDateTime.now();
        UserDevice current = device(201L, 101L, "WEB", "Chrome", "192.168.1.20", now);
        UserDevice revoked = device(202L, 101L, "ANDROID", "Pixel", "2001:db8::1", now.minusDays(1));
        when(userDeviceMapper.selectByUserId(101L)).thenReturn(List.of(current, revoked));
        when(refreshTokenMapper.selectLatestActiveExpiry(101L, 201L)).thenReturn(now.plusDays(7));
        when(refreshTokenMapper.selectLatestActiveExpiry(101L, 202L)).thenReturn(null);

        List<DeviceSessionResponse> responses = service.listDevices(101L, 201L);

        assertEquals(2, responses.size());
        assertTrue(responses.getFirst().current());
        assertEquals("ACTIVE", responses.getFirst().status());
        assertEquals("192.168.*.*", responses.getFirst().maskedIp());
        assertFalse(responses.get(1).current());
        assertEquals("REVOKED", responses.get(1).status());
        assertEquals("2001:****", responses.get(1).maskedIp());
        assertNull(responses.get(1).sessionExpiresAt());
        verify(userDeviceMapper).selectByUserId(101L);
    }

    @Test
    void revokeDeviceRevokesTokensAndRemainsIdempotentWhenNoActiveTokenExists() {
        when(userDeviceMapper.selectById(201L)).thenReturn(device(201L, 101L, "WEB", "Chrome", null,
                LocalDateTime.now()));
        when(refreshTokenMapper.revokeActiveByUserAndDevice(101L, 201L, "DEVICE_REVOKED")).thenReturn(0);

        service.revokeDevice(101L, 201L);

        verify(refreshTokenMapper).revokeActiveByUserAndDevice(101L, 201L, "DEVICE_REVOKED");
        verify(userDeviceMapper).markRevoked(101L, 201L);
        verify(auditService).record(101L, "AUTH", "DEVICE_REVOKE", "DEVICE", "201", true);
    }

    @Test
    void revokeDeviceHidesForeignDeviceAsNotFound() {
        when(userDeviceMapper.selectById(301L)).thenReturn(device(301L, 999L, "WEB", "Other", null,
                LocalDateTime.now()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.revokeDevice(101L, 301L));

        assertEquals(40401, exception.getCode());
        verify(refreshTokenMapper, never()).revokeActiveByUserAndDevice(101L, 301L, "DEVICE_REVOKED");
        verify(userDeviceMapper, never()).markRevoked(101L, 301L);
        verify(auditService).record(101L, "AUTH", "DEVICE_REVOKE", "DEVICE", "301", false);
    }

    @Test
    void revokeOtherDevicesKeepsCurrentDeviceSession() {
        when(userDeviceMapper.selectById(201L)).thenReturn(device(201L, 101L, "WEB", "Chrome", null,
                LocalDateTime.now()));

        service.revokeOtherDevices(101L, 201L);

        verify(refreshTokenMapper).revokeActiveByUserExceptDevice(101L, 201L, "OTHER_DEVICES_REVOKED");
        verify(userDeviceMapper).markOtherDevicesRevoked(101L, 201L);
        verify(auditService).record(101L, "AUTH", "OTHER_DEVICES_REVOKE", "DEVICE", "201", true);
    }

    @Test
    void revokeOtherDevicesRejectsDeviceOutsideAuthenticationSubject() {
        when(userDeviceMapper.selectById(301L)).thenReturn(device(301L, 999L, "WEB", "Other", null,
                LocalDateTime.now()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.revokeOtherDevices(101L, 301L));

        assertEquals(40101, exception.getCode());
        verify(refreshTokenMapper, never()).revokeActiveByUserExceptDevice(101L, 301L,
                "OTHER_DEVICES_REVOKED");
        verify(userDeviceMapper, never()).markOtherDevicesRevoked(101L, 301L);
    }

    @Test
    void maskIpDoesNotExposeUnknownAddressFormats() {
        assertNull(DeviceSessionServiceImpl.maskIp(null));
        assertEquals("::****", DeviceSessionServiceImpl.maskIp("::1"));
        assertEquals("****", DeviceSessionServiceImpl.maskIp("localhost"));
    }

    private static UserDevice device(Long id, Long userId, String type, String name, String ip,
                                     LocalDateTime lastActiveAt) {
        UserDevice device = new UserDevice();
        device.setId(id);
        device.setUserId(userId);
        device.setDeviceType(type);
        device.setDeviceName(name);
        device.setLastIp(ip);
        device.setLastActiveAt(lastActiveAt);
        device.setCreatedAt(lastActiveAt.minusDays(5));
        device.setStatus("ACTIVE");
        return device;
    }
}
