import { apiRequest } from '../../../core/http/api-client'
import type {
  SaveUserAddress,
  UpdateUserPreference,
  UpdateUserProfile,
  UserAddress,
  UserPreference,
  UserProfile,
} from '../domain/user-models'

export const userApi = {
  profile(): Promise<UserProfile> {
    return apiRequest({ path: '/api/users/me/profile' })
  },

  updateProfile(input: UpdateUserProfile): Promise<UserProfile> {
    return apiRequest({ path: '/api/users/me/profile', method: 'PUT', data: input })
  },

  addresses(): Promise<UserAddress[]> {
    return apiRequest({ path: '/api/users/me/addresses' })
  },

  createAddress(input: SaveUserAddress): Promise<UserAddress> {
    return apiRequest({ path: '/api/users/me/addresses', method: 'POST', data: input })
  },

  updateAddress(addressId: number, input: SaveUserAddress): Promise<UserAddress> {
    return apiRequest({
      path: `/api/users/me/addresses/${addressId}`,
      method: 'PUT',
      data: input,
    })
  },

  setDefaultAddress(addressId: number): Promise<void> {
    return apiRequest({
      path: `/api/users/me/addresses/${addressId}/default`,
      method: 'PUT',
    })
  },

  deleteAddress(addressId: number): Promise<void> {
    return apiRequest({
      path: `/api/users/me/addresses/${addressId}`,
      method: 'DELETE',
    })
  },

  preference(): Promise<UserPreference> {
    return apiRequest({ path: '/api/users/me/preferences' })
  },

  updatePreference(input: UpdateUserPreference): Promise<UserPreference> {
    return apiRequest({ path: '/api/users/me/preferences', method: 'PUT', data: input })
  },
}

