import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type { AuthenticatedUser, LoginRequest, LoginResponse, RegisteredUser, RegisterRequest } from '@/types/auth'

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
  async changePassword(currentPassword: string, newPassword: string) {
    return unwrap((await http.put<ApiResponse<null>>('/api/auth/password', { currentPassword, newPassword })).data)
  },
}
