import {
  hasMerchantRole,
  hasUserRole,
  portalHomePath,
  readPortalMode,
  redirectToLogin,
  redirectToUnauthorized,
  writePortalMode,
} from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'

Page({
  data: { username: '', loading: false, error: '', canEnterMerchant: false, roles: [] as string[] },

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
      if (readPortalMode() === 'merchant' && hasMerchantRole(user)) {
        wx.reLaunch({ url: portalHomePath('merchant', user.roles) })
        return
      }
      if (!hasUserRole(user)) {
        redirectToUnauthorized()
        return
      }
      this.setData({
        username: user.username,
        canEnterMerchant: hasMerchantRole(user),
        roles: user.roles,
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  goMerchant() {
    writePortalMode('merchant')
    wx.reLaunch({ url: portalHomePath('merchant', this.data.roles) })
  },
  goProducts() { wx.navigateTo({ url: '/package-product/pages/list/index' }) },
  goChat() { wx.navigateTo({ url: '/package-message/pages/chat/index' }) },
  goNotifications() { wx.navigateTo({ url: '/package-message/pages/notifications/index' }) },
  goCart() { wx.navigateTo({ url: '/pages/cart/index' }) },
  goOrders() { wx.navigateTo({ url: '/pages/orders/index' }) },
  goProfile() { wx.navigateTo({ url: '/pages/profile/index' }) },
  goAddresses() { wx.navigateTo({ url: '/pages/addresses/index' }) },
  goPreferences() { wx.navigateTo({ url: '/pages/preferences/index' }) },
  goPassword() { wx.navigateTo({ url: '/pages/change-password/index' }) },
  goDevices() { wx.navigateTo({ url: '/pages/devices/index' }) },
  goStatistics() { wx.navigateTo({ url: '/pages/statistics/index' }) },

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
