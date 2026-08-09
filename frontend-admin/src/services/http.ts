import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse, LoginResponse } from '../types/auth'
import { clearSession, getSession, saveAccessTokens } from '../utils/session'

const baseURL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

export const http = axios.create({
  baseURL,
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  const accessToken = getSession()?.accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

let refreshPromise: Promise<string> | null = null

async function refreshAccessToken() {
  const session = getSession()
  if (!session) {
    throw new Error('登录会话不存在')
  }

  const response = await axios.post<ApiResponse<LoginResponse>>(
    `${baseURL}/api/auth/refresh`,
    { refreshToken: session.refreshToken },
    { timeout: 10_000 },
  )

  if (response.data.code !== 0) {
    throw new Error(response.data.message || '登录会话已失效')
  }

  saveAccessTokens(response.data.data.accessToken, response.data.data.refreshToken)
  return response.data.data.accessToken
}

function redirectToLogin() {
  clearSession()
  if (window.location.pathname !== '/login') {
    window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const request = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const isUnauthorized = error.response?.status === 401
    const isTokenIssuingRequest = /\/api\/auth\/(register|login\/password|refresh)(?:\?|$)/.test(request?.url ?? '')

    if (!request || !isUnauthorized || isTokenIssuingRequest || request._retried || !getSession()?.refreshToken) {
      return Promise.reject(error)
    }

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
  },
)

export function readApiError(error: unknown, fallback = '请求失败，请稍后重试') {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    return error.response?.data?.message || fallback
  }
  return error instanceof Error ? error.message : fallback
}
