import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { userApi } from '../../features/account/data/user-api'

Page({
  data: {
    marketingEnabled: false,
    orderNotificationEnabled: true,
    systemNotificationEnabled: true,
    extraPreferences: {} as Record<string, unknown>,
    loading: false,
    error: '',
  },

  async onLoad() {
    this.setData({ loading: true, error: '' })
    try {
      const preference = await userApi.preference()
      this.setData({
        marketingEnabled: preference.marketingEnabled,
        orderNotificationEnabled: preference.orderNotificationEnabled,
        systemNotificationEnabled: preference.systemNotificationEnabled,
        extraPreferences: preference.extraPreferences ?? {},
      })
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  onMarketingChange(event: { detail: { value: boolean } }) { this.setData({ marketingEnabled: event.detail.value }) },
  onOrderChange(event: { detail: { value: boolean } }) { this.setData({ orderNotificationEnabled: event.detail.value }) },
  onSystemChange(event: { detail: { value: boolean } }) { this.setData({ systemNotificationEnabled: event.detail.value }) },

  async save() {
    this.setData({ loading: true, error: '' })
    try {
      await userApi.updatePreference({
        marketingEnabled: this.data.marketingEnabled,
        orderNotificationEnabled: this.data.orderNotificationEnabled,
        systemNotificationEnabled: this.data.systemNotificationEnabled,
        extraPreferences: this.data.extraPreferences,
      })
      wx.showToast({ title: '保存成功', icon: 'success' })
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  handleError(error: unknown) {
    this.setData({ error: errorMessage(error) })
    if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
  },
})

