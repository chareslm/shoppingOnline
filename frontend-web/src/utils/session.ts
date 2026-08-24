import type { AuthenticatedUser, LoginResponse, PortalMode } from '@/types/auth'

const SESSION_KEY = 'shopping.web.session'

export interface SavedSession extends AuthenticatedUser {
  accessToken: string
  refreshToken: string
  portalMode: PortalMode
}

export function getSession(): SavedSession | null {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    const session = JSON.parse(raw) as SavedSession
    // Migrate sessions created before the Web portal identity choice was introduced.
    if (!session.portalMode) {
      session.portalMode = session.roles.includes('USER') ? 'user' : 'merchant'
      localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    }
    return session
  } catch {
    clearSession()
    return null
  }
}

export function saveLoginSession(login: LoginResponse, portalMode: PortalMode) {
  const session: SavedSession = {
    userId: login.userId,
    username: login.username,
    roles: login.roles,
    permissions: login.permissions,
    mustChangePassword: login.mustChangePassword,
    accessToken: login.accessToken,
    refreshToken: login.refreshToken,
    portalMode,
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
