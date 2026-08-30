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
  userId: string
  username: string
  roles: string[]
  permissions: string[]
}

let refreshInFlight: Promise<void> | null = null

export async function apiRequest<T>(options: RequestOptions): Promise<T> {
  const authenticated = options.authenticated !== false
  const first = await rawRequest(options, authenticated ? readSession()?.accessToken : undefined)

  if (shouldRefreshAccessToken(first) && authenticated && readSession()?.refreshToken) {
    await refreshSession()
    const retried = await rawRequest(options, readSession()?.accessToken)
    return unwrap<T>(retried)
  }

  return unwrap<T>(first)
}

function shouldRefreshAccessToken(response: RawResponse): boolean {
  if (response.statusCode !== 401) return false
  const code = (response.data as Partial<ApiEnvelope<unknown>> | null)?.code
  // 40101 才是 Access Token 失效。改密当前密码错误是 40102，不能走刷新。
  return code == null || code === 40101
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
  const method = options.method ?? 'GET'

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${apiBaseUrl()}${options.path}`,
      method,
      data: serializeRequestData(method, options.data),
      header,
      success: (response) => resolve({ statusCode: response.statusCode, data: response.data }),
      fail: () => reject(new ApiError('无法连接服务器，请检查网络和 API 地址', 0)),
    })
  })
}

function serializeRequestData(
  method: HttpMethod,
  data?: WechatMiniprogram.IAnyObject,
): string | WechatMiniprogram.IAnyObject | undefined {
  if (data == null) return undefined
  // 微信只对 POST + application/json 自动 JSON 序列化；PUT/DELETE 传入对象会变成 query，改密等接口体为空。
  if (method === 'GET') return data
  return JSON.stringify(data)
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
  const message = friendlyMessage(envelope?.code, envelope?.message, response.statusCode)
  throw new ApiError(message, response.statusCode, envelope?.code)
}

function friendlyMessage(code: number | undefined, message: string | undefined, statusCode: number): string {
  if (code === 40102) return '当前密码不正确'
  if (code === 40303) return '请先修改临时密码后再使用其他功能'
  if (code === 40001 && message && /password must contain/i.test(message)) {
    return '新密码须同时包含大写字母、小写字母、数字和特殊字符，长度为 12–64 位'
  }
  return message || httpMessage(statusCode)
}

function httpMessage(statusCode: number): string {
  if (statusCode === 401) return '登录状态已失效，请重新登录'
  if (statusCode === 403) return '当前账号没有访问权限'
  if (statusCode === 404) return '请求的数据不存在'
  return statusCode > 0 ? `请求失败（HTTP ${statusCode}）` : '请求失败'
}
