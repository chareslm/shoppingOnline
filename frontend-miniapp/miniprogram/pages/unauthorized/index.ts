import { clearSession } from '../../core/storage/session-storage'

Page({
  backToLogin() {
    clearSession()
    wx.reLaunch({ url: '/pages/login/index' })
  },
})
