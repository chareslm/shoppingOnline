export interface SessionTokens {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

const SESSION_KEY = 'shopping.session.v1'

export function readSession(): SessionTokens | null {
  const value: unknown = wx.getStorageSync(SESSION_KEY)
  return normalizeSession(value)
}

export function writeSession(tokens: SessionTokens): void {
  const session = normalizeSession(tokens)
  if (!session) return
  wx.setStorageSync(SESSION_KEY, session)
}

export function clearSession(): void {
  wx.removeStorageSync(SESSION_KEY)
}

function normalizeSession(value: unknown): SessionTokens | null {
  if (!value || typeof value !== 'object') return null
  const candidate = value as Partial<SessionTokens>
  const expiresInSeconds = toExpiresInSeconds(candidate.expiresInSeconds)
  if (!candidate.accessToken || !candidate.refreshToken || expiresInSeconds == null) {
    return null
  }
  return {
    accessToken: candidate.accessToken,
    refreshToken: candidate.refreshToken,
    expiresInSeconds,
  }
}

/** 后端 Long 全局序列化为字符串，登录响应里的 expiresInSeconds 可能是 "1800"。 */
function toExpiresInSeconds(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return null
}

