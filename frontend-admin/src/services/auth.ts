import type { AdminUser, ApiResponse, AuditLog, AuthenticatedUser, LoginRequest, LoginResponse, PageResponse, Permission, Role } from '../types/auth'
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

  async changePassword(currentPassword: string, newPassword: string) {
    const { data } = await http.put<ApiResponse<null>>('/api/auth/password', { currentPassword, newPassword })
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

  async users(params: { keyword?: string; status?: string; page: number; pageSize: number }) {
    const { data } = await http.get<ApiResponse<PageResponse<AdminUser>>>('/api/admin/authorization/users', { params })
    return unwrap(data)
  },

  async replaceUserRoles(userId: string, roleIds: string[], currentPassword: string) {
    const { data } = await http.put<ApiResponse<null>>(`/api/admin/authorization/users/${userId}/roles`, {
      roleIds,
      currentPassword,
    })
    return unwrap(data)
  },

  async auditLogs(params: {
    actorKeyword?: string
    module?: string
    actionCode?: string
    success?: boolean
    startAt?: string
    endAt?: string
    page: number
    pageSize: number
  }) {
    const { data } = await http.get<ApiResponse<PageResponse<AuditLog>>>('/api/admin/audit-logs', { params })
    return unwrap(data)
  },
}
