import {
  hasUserRole,
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
    if (!hasUserRole(user)) {
      redirectToUnauthorized()
      return
    }
    wx.reLaunch({ url: '/pages/home/index' })
  },
})

