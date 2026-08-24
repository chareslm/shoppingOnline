export interface CurrentUser {
  userId: string
  username: string
  roles: string[]
  permissions: string[]
}

export interface LoginResponse extends CurrentUser {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

export interface RegisterResponse {
  userId: string
  username: string
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

