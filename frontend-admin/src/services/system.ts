import type { ApiResponse } from '../types/auth'
import { http } from './http'

export interface SmtpSetting {
  enabled: boolean
  host?: string | null
  port: number
  username?: string | null
  fromAddress?: string | null
  smtpAuth: boolean
  starttlsEnabled: boolean
  passwordConfigured: boolean
  usingEnvironmentFallback: boolean
}

function unwrap<T>(response: ApiResponse<T>) {
  if (response.code !== 0) {
    throw new Error(response.message || '请求失败')
  }
  return response.data
}

export const systemApi = {
  async smtp() {
    const { data } = await http.get<ApiResponse<SmtpSetting>>('/api/admin/system/smtp')
    return unwrap(data)
  },

  async updateSmtp(payload: {
    enabled: boolean
    host?: string
    port: number
    username?: string
    password?: string
    fromAddress?: string
    smtpAuth: boolean
    starttlsEnabled: boolean
    currentPassword: string
  }) {
    const { data } = await http.put<ApiResponse<SmtpSetting>>('/api/admin/system/smtp', payload)
    return unwrap(data)
  },

  async testSmtp(payload: { to?: string; currentPassword: string }) {
    const { data } = await http.post<ApiResponse<null>>('/api/admin/system/smtp/test', payload, { timeout: 20_000 })
    return unwrap(data)
  },
}
