export interface AuthenticatedUser {
  userId: string
  username: string
  roles: string[]
  permissions: string[]
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
  userId: string
  username: string | null
  status: string
}

export interface DeviceSession {
  id: string
  deviceType: 'WEB' | 'ANDROID' | 'MINIAPP' | 'ADMIN_WEB'
  deviceName: string | null
  appVersion: string | null
  maskedIp: string | null
  lastActiveAt: string
  createdAt: string
  status: 'ACTIVE' | 'REVOKED'
  current: boolean
  sessionExpiresAt: string | null
}
