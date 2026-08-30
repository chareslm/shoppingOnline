import {
  hasUserRole,
  isCustomerServiceOnly,
  portalHomePath,
  redirectToLogin,
  writePortalMode,
} from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { authApi } from '../../../features/account/data/auth-api'
import { merchantApi } from '../../../features/merchant/data/merchant-api'

Page({
  data: {
    shopName: '',
    isOwner: false,
    csOnly: false,
    canEnterUser: false,
    loading: false,
    error: '',
  },

  async onShow() {
    await this.load()
  },

  async onPullDownRefresh() {
    await this.load()
    wx.stopPullDownRefresh()
  },

  async load() {
    this.setData({ error: '' })
    try {
      const user = await authApi.me()
      this.setData({
        isOwner: user.roles.includes('MERCHANT_OWNER'),
        csOnly: isCustomerServiceOnly(user.roles),
        canEnterUser: hasUserRole(user),
      })
      try {
        const shop = await merchantApi.currentShop()
        this.setData({ shopName: shop.name })
      } catch {
        this.setData({ shopName: '' })
      }
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  goAddProduct() { wx.navigateTo({ url: '/package-merchant/pages/add-product/index' }) },
  goProducts() { wx.navigateTo({ url: '/package-merchant/pages/products/index' }) },
  goStaff() { wx.navigateTo({ url: '/package-merchant/pages/staff/index' }) },
  goStats() { wx.navigateTo({ url: '/package-merchant/pages/stats/index' }) },
  goInbox() { wx.navigateTo({ url: '/package-merchant/pages/inbox/index' }) },
  goUser() {
    writePortalMode('user')
    wx.reLaunch({ url: portalHomePath('user') })
  },

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
