import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type {
  SaveUserAddress,
  UpdateUserPreference,
  UpdateUserProfile,
  UserAddress,
  UserPreference,
  UserProfile,
} from '@/types/user'

export const userApi = {
  async profile() {
    return unwrap((await http.get<ApiResponse<UserProfile>>('/api/users/me/profile')).data)
  },
  async updateProfile(payload: UpdateUserProfile) {
    return unwrap((await http.put<ApiResponse<UserProfile>>('/api/users/me/profile', payload)).data)
  },
  async addresses() {
    return unwrap((await http.get<ApiResponse<UserAddress[]>>('/api/users/me/addresses')).data)
  },
  async createAddress(payload: SaveUserAddress) {
    return unwrap((await http.post<ApiResponse<UserAddress>>('/api/users/me/addresses', payload)).data)
  },
  async updateAddress(addressId: string, payload: SaveUserAddress) {
    return unwrap((await http.put<ApiResponse<UserAddress>>(`/api/users/me/addresses/${addressId}`, payload)).data)
  },
  async setDefaultAddress(addressId: string) {
    return unwrap((await http.put<ApiResponse<null>>(`/api/users/me/addresses/${addressId}/default`)).data)
  },
  async deleteAddress(addressId: string) {
    return unwrap((await http.delete<ApiResponse<null>>(`/api/users/me/addresses/${addressId}`)).data)
  },
  async preference() {
    return unwrap((await http.get<ApiResponse<UserPreference>>('/api/users/me/preferences')).data)
  },
  async updatePreference(payload: UpdateUserPreference) {
    return unwrap((await http.put<ApiResponse<UserPreference>>('/api/users/me/preferences', payload)).data)
  },
}
