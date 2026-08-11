import { hasUserRole, redirectToUnauthorized } from '../../core/auth/access'
import { errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'

Page({
  data: {
    identifier: '',
    password: '',
    loading: false,
    error: '',
  },

  onLoad(options: Record<string, string | undefined>) {
    if (options.identifier) {
      this.setData({ identifier: decodeURIComponent(options.identifier) })
    }
  },

  onIdentifierInput(event: { detail: { value: string } }) {
    this.setData({ identifier: event.detail.value })
  },

  onPasswordInput(event: { detail: { value: string } }) {
    this.setData({ password: event.detail.value })
  },

  async submit() {
    const identifier = this.data.identifier.trim()
    if (!identifier || !this.data.password) {
      this.setData({ error: '请输入登录标识和密码' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const user = await authApi.login(identifier, this.data.password)
      if (!hasUserRole(user)) {
        redirectToUnauthorized()
        return
      }
      wx.reLaunch({ url: '/pages/home/index' })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ loading: false })
    }
  },

  goRegister() {
    wx.navigateTo({ url: '/pages/register/index' })
  },
})

