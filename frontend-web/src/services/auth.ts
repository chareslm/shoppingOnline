import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type { AuthenticatedUser, LoginRequest, LoginResponse } from '@/types/auth'

export const authApi = {
  async login(payload: LoginRequest) {
    return unwrap((await http.post<ApiResponse<LoginResponse>>('/api/auth/login/password', payload)).data)
  },
  async logout(deviceId: string) {
    return unwrap((await http.post<ApiResponse<null>>('/api/auth/logout', { deviceId })).data)
  },
  async currentUser() {
    return unwrap((await http.get<ApiResponse<AuthenticatedUser>>('/api/auth/me')).data)
  },
}
