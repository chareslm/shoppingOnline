import type { AdminMode, AuthenticatedUser, LoginResponse } from '../types/auth'

const SESSION_KEY = 'shopping.admin.session'

export interface SavedSession extends AuthenticatedUser {
  accessToken: string
  refreshToken: string
  adminMode: AdminMode
}

export function getSession(): SavedSession | null {
  const rawSession = localStorage.getItem(SESSION_KEY)
  if (!rawSession) {
    return null
  }

  try {
    return JSON.parse(rawSession) as SavedSession
  } catch {
    clearSession()
    return null
  }
}

export function saveLoginSession(login: LoginResponse, adminMode: AdminMode) {
  const session: SavedSession = {
    userId: login.userId,
    username: login.username,
    roles: login.roles,
    permissions: login.permissions,
    mustChangePassword: login.mustChangePassword,
    accessToken: login.accessToken,
    refreshToken: login.refreshToken,
    adminMode,
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function saveAccessTokens(accessToken: string, refreshToken: string) {
  const session = getSession()
  if (!session) {
    return
  }

  localStorage.setItem(SESSION_KEY, JSON.stringify({ ...session, accessToken, refreshToken }))
}

export function saveAuthenticatedUser(user: AuthenticatedUser) {
  const session = getSession()
  if (!session) {
    return
  }

  localStorage.setItem(SESSION_KEY, JSON.stringify({ ...session, ...user }))
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}

export function hasPermission(permission: string) {
  return getSession()?.permissions.includes(permission) ?? false
}
