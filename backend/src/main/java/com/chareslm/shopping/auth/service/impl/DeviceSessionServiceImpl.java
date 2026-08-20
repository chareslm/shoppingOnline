package com.chareslm.shopping.auth.service.impl;

import com.chareslm.shopping.auth.dto.response.DeviceSessionResponse;
import com.chareslm.shopping.auth.entity.UserDevice;
import com.chareslm.shopping.auth.mapper.RefreshTokenMapper;
import com.chareslm.shopping.auth.mapper.UserDeviceMapper;
import com.chareslm.shopping.auth.service.AuditService;
import com.chareslm.shopping.auth.service.DeviceSessionService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceSessionServiceImpl implements DeviceSessionService {
    private final UserDeviceMapper userDeviceMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final AuditService auditService;

    public DeviceSessionServiceImpl(UserDeviceMapper userDeviceMapper, RefreshTokenMapper refreshTokenMapper,
                                    AuditService auditService) {
        this.userDeviceMapper = userDeviceMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceSessionResponse> listDevices(Long userId, Long currentDeviceId) {
        return userDeviceMapper.selectByUserId(userId).stream()
                .map(device -> toResponse(userId, currentDeviceId, device))
                .toList();
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void revokeDevice(Long userId, Long deviceId) {
        UserDevice device = userDeviceMapper.selectById(deviceId);
        if (device == null || !userId.equals(device.getUserId())) {
            auditService.record(userId, "AUTH", "DEVICE_REVOKE", "DEVICE", String.valueOf(deviceId), false);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        refreshTokenMapper.revokeActiveByUserAndDevice(userId, deviceId, "DEVICE_REVOKED");
        userDeviceMapper.markRevoked(userId, deviceId);
        auditService.record(userId, "AUTH", "DEVICE_REVOKE", "DEVICE", deviceId.toString(), true);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void revokeOtherDevices(Long userId, Long currentDeviceId) {
        UserDevice currentDevice = userDeviceMapper.selectById(currentDeviceId);
        if (currentDevice == null || !userId.equals(currentDevice.getUserId())) {
            auditService.record(userId, "AUTH", "OTHER_DEVICES_REVOKE", "DEVICE",
                    String.valueOf(currentDeviceId), false);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        refreshTokenMapper.revokeActiveByUserExceptDevice(userId, currentDeviceId, "OTHER_DEVICES_REVOKED");
        userDeviceMapper.markOtherDevicesRevoked(userId, currentDeviceId);
        auditService.record(userId, "AUTH", "OTHER_DEVICES_REVOKE", "DEVICE", currentDeviceId.toString(), true);
    }

    private DeviceSessionResponse toResponse(Long userId, Long currentDeviceId, UserDevice device) {
        LocalDateTime activeExpiry = refreshTokenMapper.selectLatestActiveExpiry(userId, device.getId());
        return new DeviceSessionResponse(
                device.getId(),
                device.getDeviceType(),
                device.getDeviceName(),
                device.getAppVersion(),
                maskIp(device.getLastIp()),
                device.getLastActiveAt(),
                device.getCreatedAt(),
                activeExpiry == null ? "REVOKED" : "ACTIVE",
                device.getId().equals(currentDeviceId),
                activeExpiry
        );
    }

    static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String[] ipv4Parts = ip.split("\\.", -1);
        if (ipv4Parts.length == 4) {
            return ipv4Parts[0] + "." + ipv4Parts[1] + ".*.*";
        }
        int separator = ip.indexOf(':');
        if (separator >= 0) {
            String firstSegment = ip.substring(0, separator);
            return firstSegment.isEmpty() ? "::****" : firstSegment + ":****";
        }
        return "****";
    }
}
