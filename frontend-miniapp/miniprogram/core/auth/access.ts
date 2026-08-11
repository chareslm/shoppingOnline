import { authApi, type CurrentUser } from '../../features/account/data/auth-api'
import { clearSession, readSession } from '../storage/session-storage'

export async function restoreUser(): Promise<CurrentUser | null> {
  if (!readSession()) return null
  try {
    return await authApi.me()
  } catch {
    clearSession()
    return null
  }
}

export function hasUserRole(user: CurrentUser): boolean {
  return user.roles.includes('USER')
}

export function redirectToLogin(): void {
  wx.reLaunch({ url: '/pages/login/index' })
}

export function redirectToUnauthorized(): void {
  wx.reLaunch({ url: '/pages/unauthorized/index' })
}

