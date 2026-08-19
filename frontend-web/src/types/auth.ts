export interface AuthenticatedUser {
  userId: number
  username: string
  roles: string[]
  permissions: string[]
  mustChangePassword: boolean
}

export type PortalMode = 'user' | 'merchant'

export const MERCHANT_PORTAL_ROLES = ['MERCHANT_OWNER', 'MERCHANT_STAFF', 'CUSTOMER_SERVICE']
export const MERCHANT_OPERATOR_ROLES = ['MERCHANT_OWNER', 'MERCHANT_STAFF']
export const MERCHANT_OWNER_ROLE = 'MERCHANT_OWNER'

export function isCustomerServiceOnly(roles: string[] = []) {
  return roles.includes('CUSTOMER_SERVICE')
    && !roles.includes('MERCHANT_OWNER')
    && !roles.includes('MERCHANT_STAFF')
}

export interface LoginRequest {
  identifier: string
  password: string
  deviceId: string
  deviceType: 'WEB'
  deviceName: string
}

export interface LoginResponse extends AuthenticatedUser {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

export interface RegisterRequest {
  username?: string
  email?: string
  phone?: string
  password: string
}

export interface RegisteredUser {
  userId: number
  username: string | null
  status: string
}
