import {
  hasUserRole,
  redirectToLogin,
  redirectToUnauthorized,
} from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'

Page({
  data: { username: '', loading: false, error: '' },

  async onShow() {
    await this.loadUser()
  },

  async onPullDownRefresh() {
    await this.loadUser()
    wx.stopPullDownRefresh()
  },

  async loadUser() {
    this.setData({ error: '' })
    try {
      const user = await authApi.me()
      if (!hasUserRole(user)) {
        redirectToUnauthorized()
        return
      }
      this.setData({ username: user.username })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  goCart() { wx.navigateTo({ url: '/pages/cart/index' }) },
  goOrders() { wx.navigateTo({ url: '/pages/orders/index' }) },
  goProfile() { wx.navigateTo({ url: '/pages/profile/index' }) },
  goAddresses() { wx.navigateTo({ url: '/pages/addresses/index' }) },
  goPreferences() { wx.navigateTo({ url: '/pages/preferences/index' }) },
  goPassword() { wx.navigateTo({ url: '/pages/change-password/index' }) },
  goDevices() { wx.navigateTo({ url: '/pages/devices/index' }) },

  async logout() {
    this.setData({ loading: true, error: '' })
    try {
      await authApi.logout()
    } catch (error) {
      wx.showToast({ title: errorMessage(error), icon: 'none' })
    } finally {
      this.setData({ loading: false })
      redirectToLogin()
    }
  },
})
