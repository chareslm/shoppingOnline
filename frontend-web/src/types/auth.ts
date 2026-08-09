export interface AuthenticatedUser {
  userId: number
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
