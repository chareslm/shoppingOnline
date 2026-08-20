import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type { AuthenticatedUser, DeviceSession, LoginRequest, LoginResponse, RegisteredUser, RegisterRequest } from '@/types/auth'

export const authApi = {
  async register(payload: RegisterRequest) {
    return unwrap((await http.post<ApiResponse<RegisteredUser>>('/api/auth/register', payload)).data)
  },
  async login(payload: LoginRequest) {
    return unwrap((await http.post<ApiResponse<LoginResponse>>('/api/auth/login/password', payload)).data)
  },
  async logout(deviceId: string) {
    return unwrap((await http.post<ApiResponse<null>>('/api/auth/logout', { deviceId })).data)
  },
  async currentUser() {
    return unwrap((await http.get<ApiResponse<AuthenticatedUser>>('/api/auth/me')).data)
  },
  async devices() {
    return unwrap((await http.get<ApiResponse<DeviceSession[]>>('/api/auth/devices')).data)
  },
  async revokeDevice(deviceId: string) {
    return unwrap((await http.post<ApiResponse<null>>(`/api/auth/devices/${deviceId}/revoke`)).data)
  },
  async revokeOtherDevices() {
    return unwrap((await http.post<ApiResponse<null>>('/api/auth/devices/revoke-others')).data)
  },
}
