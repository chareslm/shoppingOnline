import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../services/auth'
import { getDeviceId, getDeviceName } from '../utils/device'
import { clearSession, getSession, saveAuthenticatedUser, saveLoginSession, type SavedSession } from '../utils/session'

const MANAGEMENT_ROLES = new Set(['ADMIN', 'SUPER_ADMIN', 'MERCHANT_OWNER', 'MERCHANT_STAFF', 'CUSTOMER_SERVICE'])

export const useAuthStore = defineStore('auth', () => {
  const session = ref<SavedSession | null>(getSession())
  const loading = ref(false)
  const isAuthenticated = computed(() => Boolean(session.value?.accessToken && session.value?.refreshToken))

  async function login(identifier: string, password: string) {
    loading.value = true
    try {
      const loginResult = await authApi.login({
        identifier,
        password,
        deviceId: getDeviceId(),
        deviceType: 'ADMIN_WEB',
        deviceName: getDeviceName(),
      })

      if (!loginResult.roles.some((role) => MANAGEMENT_ROLES.has(role))) {
        throw new Error('该账号没有管理端访问权限')
      }

      session.value = saveLoginSession(loginResult)
    } finally {
      loading.value = false
    }
  }

  async function restoreCurrentUser() {
    if (!isAuthenticated.value) {
      return
    }

    const currentUser = await authApi.currentUser()
    saveAuthenticatedUser(currentUser)
    session.value = getSession()
  }

  async function logout() {
    try {
      if (isAuthenticated.value) {
        await authApi.logout(getDeviceId())
      }
    } finally {
      clearSession()
      session.value = null
    }
  }

  function clearLocalSession() {
    clearSession()
    session.value = null
  }

  return { session, loading, isAuthenticated, login, restoreCurrentUser, logout, clearLocalSession }
})
