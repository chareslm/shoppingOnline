import { apiBaseUrl } from '../../config/environment'
import { ApiError, type ApiEnvelope } from '../models/api'
import {
  clearSession,
  readSession,
  writeSession,
  type SessionTokens,
} from '../storage/session-storage'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

interface RequestOptions {
  path: string
  method?: HttpMethod
  data?: WechatMiniprogram.IAnyObject
  authenticated?: boolean
}

interface RawResponse {
  statusCode: number
  data: unknown
}

interface RefreshResponse extends SessionTokens {
  userId: number
  username: string
  roles: string[]
  permissions: string[]
}

let refreshInFlight: Promise<void> | null = null

export async function apiRequest<T>(options: RequestOptions): Promise<T> {
  const authenticated = options.authenticated !== false
  const first = await rawRequest(options, authenticated ? readSession()?.accessToken : undefined)

  if (first.statusCode === 401 && authenticated && readSession()?.refreshToken) {
    await refreshSession()
    const retried = await rawRequest(options, readSession()?.accessToken)
    return unwrap<T>(retried)
  }

  return unwrap<T>(first)
}

async function refreshSession(): Promise<void> {
  if (!refreshInFlight) {
    refreshInFlight = performRefresh().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

async function performRefresh(): Promise<void> {
  const refreshToken = readSession()?.refreshToken
  if (!refreshToken) {
    clearSession()
    throw new ApiError('登录状态已失效，请重新登录', 401)
  }

  try {
    const response = await rawRequest(
      {
        path: '/api/auth/refresh',
        method: 'POST',
        data: { refreshToken },
        authenticated: false,
      },
      undefined,
    )
    const data = unwrap<RefreshResponse>(response)
    writeSession({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      expiresInSeconds: data.expiresInSeconds,
    })
  } catch (error) {
    clearSession()
    throw error instanceof ApiError
      ? error
      : new ApiError('登录状态已失效，请重新登录', 401)
  }
}

function rawRequest(options: RequestOptions, accessToken?: string): Promise<RawResponse> {
  const header: Record<string, string> = { 'content-type': 'application/json' }
  if (accessToken) {
    header.Authorization = `Bearer ${accessToken}`
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${apiBaseUrl()}${options.path}`,
      method: options.method ?? 'GET',
      data: options.data,
      header,
      success: (response) => resolve({ statusCode: response.statusCode, data: response.data }),
      fail: () => reject(new ApiError('无法连接服务器，请检查网络和 API 地址', 0)),
    })
  })
}

function unwrap<T>(response: RawResponse): T {
  const envelope = response.data as Partial<ApiEnvelope<T>> | null
  if (
    response.statusCode >= 200 &&
    response.statusCode < 300 &&
    envelope &&
    envelope.code === 0
  ) {
    return envelope.data as T
  }
  const message = envelope?.message || httpMessage(response.statusCode)
  throw new ApiError(message, response.statusCode, envelope?.code)
}

function httpMessage(statusCode: number): string {
  if (statusCode === 401) return '登录状态已失效，请重新登录'
  if (statusCode === 403) return '当前账号没有访问权限'
  if (statusCode === 404) return '请求的数据不存在'
  return statusCode > 0 ? `请求失败（HTTP ${statusCode}）` : '请求失败'
}
