import {
  enterAuthenticatedPortal,
  hasMerchantRole,
  hasUserRole,
  readPortalMode,
  redirectToLogin,
  redirectToUnauthorized,
  restoreUser,
} from '../../core/auth/access'

Page({
  data: { message: '正在恢复登录状态…' },

  async onLoad() {
    const user = await restoreUser()
    if (!user) {
      redirectToLogin()
      return
    }
    const mode = readPortalMode()
    const allowed = mode === 'merchant' ? hasMerchantRole(user) : hasUserRole(user)
    if (!allowed) {
      redirectToUnauthorized()
      return
    }
    enterAuthenticatedPortal(user, mode)
  },
})
