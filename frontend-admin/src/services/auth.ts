import type { ApiResponse, AuthenticatedUser, LoginRequest, LoginResponse, Permission, Role } from '../types/auth'
import { http } from './http'

function unwrap<T>(response: ApiResponse<T>) {
  if (response.code !== 0) {
    throw new Error(response.message || '请求失败')
  }
  return response.data
}

export const authApi = {
  async login(payload: LoginRequest) {
    const { data } = await http.post<ApiResponse<LoginResponse>>('/api/auth/login/password', payload)
    return unwrap(data)
  },

  async logout(deviceId: string) {
    const { data } = await http.post<ApiResponse<null>>('/api/auth/logout', { deviceId })
    return unwrap(data)
  },

  async currentUser() {
    const { data } = await http.get<ApiResponse<AuthenticatedUser>>('/api/auth/me')
    return unwrap(data)
  },

  async roles() {
    const { data } = await http.get<ApiResponse<Role[]>>('/api/admin/authorization/roles')
    return unwrap(data)
  },

  async permissions() {
    const { data } = await http.get<ApiResponse<Permission[]>>('/api/admin/authorization/permissions')
    return unwrap(data)
  },
}
