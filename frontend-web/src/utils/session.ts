import type { AuthenticatedUser, LoginResponse } from '@/types/auth'

const SESSION_KEY = 'shopping.web.session'

export interface SavedSession extends AuthenticatedUser {
  accessToken: string
  refreshToken: string
}

export function getSession(): SavedSession | null {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SavedSession
  } catch {
    clearSession()
    return null
  }
}

export function saveLoginSession(login: LoginResponse) {
  const session: SavedSession = {
    userId: login.userId,
    username: login.username,
    roles: login.roles,
    permissions: login.permissions,
    accessToken: login.accessToken,
    refreshToken: login.refreshToken,
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function saveAccessTokens(accessToken: string, refreshToken: string) {
  const session = getSession()
  if (session) localStorage.setItem(SESSION_KEY, JSON.stringify({ ...session, accessToken, refreshToken }))
}

export function saveAuthenticatedUser(user: AuthenticatedUser) {
  const session = getSession()
  if (session) localStorage.setItem(SESSION_KEY, JSON.stringify({ ...session, ...user }))
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}
