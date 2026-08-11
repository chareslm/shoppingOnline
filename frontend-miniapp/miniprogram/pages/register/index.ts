import { errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'

Page({
  data: {
    username: '',
    password: '',
    confirmation: '',
    loading: false,
    error: '',
  },

  onUsernameInput(event: { detail: { value: string } }) {
    this.setData({ username: event.detail.value })
  },
  onPasswordInput(event: { detail: { value: string } }) {
    this.setData({ password: event.detail.value })
  },
  onConfirmationInput(event: { detail: { value: string } }) {
    this.setData({ confirmation: event.detail.value })
  },

  async submit() {
    const username = this.data.username.trim()
    if (!username || !this.data.password) {
      this.setData({ error: '请输入用户名和密码' })
      return
    }
    if (this.data.password !== this.data.confirmation) {
      this.setData({ error: '两次输入的密码不一致' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      await authApi.register({ username, password: this.data.password })
      wx.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: `/pages/login/index?identifier=${encodeURIComponent(username)}` })
      }, 500)
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ loading: false })
    }
  },
})

