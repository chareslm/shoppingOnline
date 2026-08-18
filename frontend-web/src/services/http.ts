import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import type { LoginResponse } from '@/types/auth'
import { clearSession, getSession, saveAccessTokens } from '@/utils/session'
import { isBackendUnavailable, retryWhileBackendStarts } from '@/services/backendRetry'

const baseURL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

export const http = axios.create({ baseURL, timeout: 10_000 })

http.interceptors.request.use((config) => {
  const accessToken = getSession()?.accessToken
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

let refreshPromise: Promise<string> | null = null

async function refreshAccessToken() {
  const session = getSession()
  if (!session) throw new Error('登录会话不存在')

  const response = await axios.post<ApiResponse<LoginResponse>>(
    `${baseURL}/api/auth/refresh`,
    { refreshToken: session.refreshToken },
    { timeout: 10_000 },
  )
  if (response.data.code !== 0) throw new Error(response.data.message || '登录会话已失效')

  saveAccessTokens(response.data.data.accessToken, response.data.data.refreshToken)
  return response.data.data.accessToken
}

function redirectToLogin() {
  clearSession()
  if (window.location.pathname !== '/login') {
    const redirect = `${window.location.pathname}${window.location.search}`
    window.location.assign(`/login?redirect=${encodeURIComponent(redirect)}`)
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const request = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const isUnauthorized = error.response?.status === 401
    const isTokenIssuingRequest = /\/api\/auth\/(register|login\/password|refresh)(?:\?|$)/.test(request?.url ?? '')
    if (request && isUnauthorized && !isTokenIssuingRequest && !request._retried && getSession()?.refreshToken) {
      request._retried = true
      try {
        refreshPromise ??= refreshAccessToken()
        const accessToken = await refreshPromise
        request.headers.Authorization = `Bearer ${accessToken}`
        return http(request)
      } catch (refreshError) {
        redirectToLogin()
        return Promise.reject(refreshError)
      } finally {
        refreshPromise = null
      }
    }

    return retryWhileBackendStarts(error, (config) => http(config))
  },
)

export function unwrap<T>(response: ApiResponse<T>) {
  if (response.code !== 0) throw new Error(response.message || '请求失败')
  return response.data
}

export function readApiError(error: unknown, fallback = '请求失败，请稍后重试') {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    if (isBackendUnavailable(error)) return '后端正在启动或重启，请稍候再试（通常约一分钟）'
    return error.response?.data?.message || fallback
  }
  return error instanceof Error ? error.message : fallback
}
