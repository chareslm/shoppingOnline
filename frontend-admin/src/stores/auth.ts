import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../services/auth'
import type { AdminMode } from '../types/auth'
import { getDeviceId, getDeviceName } from '../utils/device'
import { clearSession, getSession, saveAuthenticatedUser, saveLoginSession, type SavedSession } from '../utils/session'

const REQUIRED_ROLES: Record<AdminMode, string[]> = {
  system: ['SUPER_ADMIN'],
  platform: ['ADMIN', 'SUPER_ADMIN'],
}

const MODE_NAME: Record<AdminMode, string> = {
  system: '系统管理员',
  platform: '平台管理员',
}

function assertModeRole(adminMode: AdminMode, roles: string[]) {
  const requiredRoles = REQUIRED_ROLES[adminMode]
  if (!requiredRoles.some((role) => roles.includes(role))) {
    throw new Error(`所选“${MODE_NAME[adminMode]}”身份要求账号具备 ${requiredRoles.join(' 或 ')} 角色`)
  }
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<SavedSession | null>(getSession())
  const loading = ref(false)
  const isAuthenticated = computed(() =>
    Boolean(session.value?.accessToken && session.value?.refreshToken && session.value.adminMode),
  )

  async function login(identifier: string, password: string, adminMode: AdminMode) {
    loading.value = true
    try {
      const loginResult = await authApi.login({
        identifier,
        password,
        deviceId: getDeviceId(),
        deviceType: 'ADMIN_WEB',
        deviceName: getDeviceName(),
      })

      // 管理端只接受两类平台角色；商家与客服角色即使认证成功也不能建立管理端会话。
      assertModeRole(adminMode, loginResult.roles)
      session.value = saveLoginSession(loginResult, adminMode)
    } finally {
      loading.value = false
    }
  }

  async function restoreCurrentUser() {
    if (!isAuthenticated.value) {
      return
    }

    const currentUser = await authApi.currentUser()
    assertModeRole(session.value!.adminMode, currentUser.roles)
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
