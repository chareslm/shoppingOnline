import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/services/auth'
import { getDeviceId, getDeviceName } from '@/utils/device'
import { clearSession, getSession, saveAuthenticatedUser, saveLoginSession, type SavedSession } from '@/utils/session'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<SavedSession | null>(getSession())
  const loading = ref(false)
  const restored = ref(false)
  const isAuthenticated = computed(() => Boolean(session.value?.accessToken && session.value?.refreshToken))

  async function login(identifier: string, password: string) {
    loading.value = true
    try {
      const result = await authApi.login({
        identifier,
        password,
        deviceId: getDeviceId(),
        deviceType: 'WEB',
        deviceName: getDeviceName(),
      })
      session.value = saveLoginSession(result)
      restored.value = true
    } finally {
      loading.value = false
    }
  }

  async function restoreCurrentUser() {
    if (restored.value || !isAuthenticated.value) return
    const currentUser = await authApi.currentUser()
    saveAuthenticatedUser(currentUser)
    session.value = getSession()
    restored.value = true
  }

  async function logout() {
    try {
      if (isAuthenticated.value) await authApi.logout(getDeviceId())
    } catch {
      // Local logout must still finish when the backend is unavailable.
    } finally {
      clearSession()
      session.value = null
      restored.value = false
    }
  }

  return { session, loading, isAuthenticated, login, restoreCurrentUser, logout }
})
