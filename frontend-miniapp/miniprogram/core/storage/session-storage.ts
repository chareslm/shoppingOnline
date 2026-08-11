export interface SessionTokens {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

const SESSION_KEY = 'shopping.session.v1'

export function readSession(): SessionTokens | null {
  const value: unknown = wx.getStorageSync(SESSION_KEY)
  if (!isSession(value)) {
    return null
  }
  return value
}

export function writeSession(tokens: SessionTokens): void {
  wx.setStorageSync(SESSION_KEY, tokens)
}

export function clearSession(): void {
  wx.removeStorageSync(SESSION_KEY)
}

function isSession(value: unknown): value is SessionTokens {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<SessionTokens>
  return Boolean(
    candidate.accessToken &&
      candidate.refreshToken &&
      typeof candidate.expiresInSeconds === 'number',
  )
}

