export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  items: T[]
  total: string
  page: number
  pageSize: number
}

export interface LoginRequest {
  identifier: string
  password: string
  deviceId: string
  deviceType: 'ADMIN_WEB'
  deviceName: string
}

export interface AuthenticatedUser {
  userId: string
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
  id: string
  code: string
  name: string
  dataScope?: string
}

export interface Permission {
  id: string
  code: string
  name: string
}

export interface AdminUser {
  userId: string
  username?: string | null
  maskedEmail?: string | null
  maskedPhone?: string | null
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED' | 'PENDING_VERIFICATION'
  roles: Role[]
  createdAt: string
  lastLoginAt?: string | null
}

export interface AuditLog {
  id: string
  actorUserId?: string | null
  actorUsername?: string | null
  module: string
  actionCode: string
  targetType?: string | null
  targetId?: string | null
  success: boolean
  traceId?: string | null
  requestMethod?: string | null
  requestPath?: string | null
  maskedClientIp?: string | null
  client?: string | null
  detail?: unknown
  createdAt: string
}
