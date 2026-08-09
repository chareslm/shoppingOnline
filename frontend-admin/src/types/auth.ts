export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface LoginRequest {
  identifier: string
  password: string
  deviceId: string
  deviceType: 'ADMIN_WEB'
  deviceName: string
}

export interface AuthenticatedUser {
  userId: number
  username: string
  roles: string[]
  permissions: string[]
}

export interface LoginResponse extends AuthenticatedUser {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

export interface Role {
  id: number
  code: string
  name: string
  dataScope?: string
}

export interface Permission {
  id: number
  code: string
  name: string
}
