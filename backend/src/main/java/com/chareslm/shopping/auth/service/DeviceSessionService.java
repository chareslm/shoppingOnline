package com.chareslm.shopping.auth.service;

import com.chareslm.shopping.auth.dto.response.DeviceSessionResponse;

import java.util.List;

public interface DeviceSessionService {
    List<DeviceSessionResponse> listDevices(Long userId, Long currentDeviceId);

    void revokeDevice(Long userId, Long deviceId);

    void revokeOtherDevices(Long userId, Long currentDeviceId);
}
