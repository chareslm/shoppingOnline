import { authApi, type CurrentUser } from '../../features/account/data/auth-api'
import { clearSession, readSession } from '../storage/session-storage'

export type PortalMode = 'user' | 'merchant'

export const MERCHANT_PORTAL_ROLES = ['MERCHANT_OWNER', 'MERCHANT_STAFF', 'CUSTOMER_SERVICE'] as const
export const PORTAL_MODE_STORAGE_KEY = 'shopping.portal-mode.v1'

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

export function hasMerchantRole(user: CurrentUser): boolean {
  return user.roles.some((role) => MERCHANT_PORTAL_ROLES.includes(role as (typeof MERCHANT_PORTAL_ROLES)[number]))
}

export function isCustomerServiceOnly(roles: string[] = []): boolean {
  return (
    roles.includes('CUSTOMER_SERVICE') &&
    !roles.includes('MERCHANT_OWNER') &&
    !roles.includes('MERCHANT_STAFF')
  )
}

export function readPortalMode(): PortalMode {
  try {
    const value = wx.getStorageSync(PORTAL_MODE_STORAGE_KEY)
    if (value === 'merchant' || value === 'user') return value
  } catch {
    // Storage may be unavailable in isolated unit tests.
  }
  return 'user'
}

export function writePortalMode(mode: PortalMode): void {
  wx.setStorageSync(PORTAL_MODE_STORAGE_KEY, mode)
}

export function portalHomePath(mode: PortalMode, roles: string[] = []): string {
  if (mode === 'merchant') {
    return isCustomerServiceOnly(roles)
      ? '/package-merchant/pages/inbox/index'
      : '/package-merchant/pages/home/index'
  }
  return '/pages/home/index'
}

export function enterAuthenticatedPortal(user: CurrentUser, mode: PortalMode = readPortalMode()): void {
  if (user.mustChangePassword) {
    wx.reLaunch({ url: '/pages/change-password/index?forced=1' })
    return
  }
  wx.reLaunch({ url: portalHomePath(mode, user.roles) })
}

export function redirectToLogin(): void {
  wx.reLaunch({ url: '/pages/login/index' })
}

export function redirectToUnauthorized(): void {
  wx.reLaunch({ url: '/pages/unauthorized/index' })
}
