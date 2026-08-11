export interface CurrentUser {
  userId: number
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
  userId: number
  username: string
  status: string
}

