import { deviceName, stableDeviceId } from '../../../core/device/device'
import { apiRequest } from '../../../core/http/api-client'
import { clearSession, writeSession } from '../../../core/storage/session-storage'
import type {
  CurrentUser,
  DeviceSession,
  LoginResponse,
  RegisterResponse,
} from '../domain/auth-models'

export type { CurrentUser, LoginResponse, RegisterResponse } from '../domain/auth-models'

export const authApi = {
  register(input: {
    username: string
    password: string
    email?: string
    phone?: string
  }): Promise<RegisterResponse> {
    return apiRequest({
      path: '/api/auth/register',
      method: 'POST',
      data: input,
      authenticated: false,
    })
  },

  async login(identifier: string, password: string): Promise<LoginResponse> {
    const data = await apiRequest<LoginResponse>({
      path: '/api/auth/login/password',
      method: 'POST',
      data: {
        identifier,
        password,
        deviceId: stableDeviceId(),
        deviceType: 'MINIAPP',
        deviceName: deviceName(),
      },
      authenticated: false,
    })
    writeSession({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      expiresInSeconds: data.expiresInSeconds,
    })
    return data
  },

  me(): Promise<CurrentUser> {
    return apiRequest({ path: '/api/auth/me' })
  },

  devices(): Promise<DeviceSession[]> {
    return apiRequest({ path: '/api/auth/devices' })
  },

  async revokeDevice(deviceId: string, current: boolean): Promise<void> {
    await apiRequest<void>({
      path: `/api/auth/devices/${deviceId}/revoke`,
      method: 'POST',
    })
    if (current) clearSession()
  },

  async revokeOtherDevices(): Promise<void> {
    await apiRequest<void>({
      path: '/api/auth/devices/revoke-others',
      method: 'POST',
    })
  },

  async logout(): Promise<void> {
    try {
      await apiRequest<void>({
        path: '/api/auth/logout',
        method: 'POST',
        data: { deviceId: stableDeviceId() },
      })
    } finally {
      clearSession()
    }
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiRequest<void>({
      path: '/api/auth/password',
      method: 'PUT',
      data: { currentPassword, newPassword },
    })
    clearSession()
  },
}

