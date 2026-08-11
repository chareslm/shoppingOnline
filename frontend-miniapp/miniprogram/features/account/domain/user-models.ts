export type Gender = 'UNKNOWN' | 'MALE' | 'FEMALE'

export interface UserProfile {
  userId: number
  nickname?: string | null
  avatarUrl?: string | null
  realName?: string | null
  gender: Gender
  birthday?: string | null
  bio?: string | null
}

export interface UpdateUserProfile {
  nickname: string
  avatarUrl: string
  realName: string
  gender: Gender
  birthday: string | null
  bio: string
}

export interface UserAddress {
  id: number
  recipientName: string
  recipientPhone: string
  provinceCode?: string | null
  provinceName: string
  cityCode?: string | null
  cityName: string
  districtCode?: string | null
  districtName: string
  detailAddress: string
  postalCode?: string | null
  isDefault: boolean
}

export interface SaveUserAddress {
  recipientName: string
  recipientPhone: string
  provinceCode: string
  provinceName: string
  cityCode: string
  cityName: string
  districtCode: string
  districtName: string
  detailAddress: string
  postalCode: string
  isDefault: boolean
}

export interface UserPreference {
  userId: number
  marketingEnabled: boolean
  orderNotificationEnabled: boolean
  systemNotificationEnabled: boolean
  extraPreferences: Record<string, unknown>
}

export interface UpdateUserPreference {
  marketingEnabled: boolean
  orderNotificationEnabled: boolean
  systemNotificationEnabled: boolean
  extraPreferences: Record<string, unknown>
}

