import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/services/auth'
import { getDeviceId, getDeviceName } from '@/utils/device'
import { clearSession, getSession, saveAuthenticatedUser, saveLoginSession, type SavedSession } from '@/utils/session'
import { MERCHANT_PORTAL_ROLES, type PortalMode } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<SavedSession | null>(getSession())
  const loading = ref(false)
  const restored = ref(false)
  const isAuthenticated = computed(() => Boolean(session.value?.accessToken && session.value?.refreshToken))

  async function login(identifier: string, password: string, portalMode: PortalMode) {
    loading.value = true
    try {
      const result = await authApi.login({
        identifier,
        password,
        deviceId: getDeviceId(),
        deviceType: 'WEB',
        deviceName: getDeviceName(),
      })
      const allowed =
        portalMode === 'user'
          ? result.roles.includes('USER')
          : result.roles.some((role) => MERCHANT_PORTAL_ROLES.includes(role))
      if (!allowed) {
        throw new Error(portalMode === 'user' ? '该账号不具备用户身份' : '该账号不具备商家身份')
      }
      session.value = saveLoginSession(result, portalMode)
      restored.value = true
    } finally {
      loading.value = false
    }
  }

  async function restoreCurrentUser() {
    if (restored.value || !isAuthenticated.value) return
    const currentUser = await authApi.currentUser()
    const portalMode = session.value?.portalMode
    const allowed =
      portalMode === 'merchant'
        ? currentUser.roles.some((role) => MERCHANT_PORTAL_ROLES.includes(role))
        : currentUser.roles.includes('USER')
    if (!allowed) throw new Error(portalMode === 'merchant' ? '该账号不具备商家身份' : '该账号不具备用户身份')
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

  function clearLocalSession() {
    clearSession()
    session.value = null
    restored.value = false
  }

  return { session, loading, isAuthenticated, login, restoreCurrentUser, logout, clearLocalSession }
})
