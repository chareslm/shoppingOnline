import { errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'

Page({
  data: {
    currentPassword: '',
    newPassword: '',
    confirmation: '',
    loading: false,
    error: '',
  },

  onCurrentInput(event: { detail: { value: string } }) { this.setData({ currentPassword: event.detail.value }) },
  onNewInput(event: { detail: { value: string } }) { this.setData({ newPassword: event.detail.value }) },
  onConfirmationInput(event: { detail: { value: string } }) { this.setData({ confirmation: event.detail.value }) },

  async submit() {
    if (!this.data.currentPassword || !this.data.newPassword) {
      this.setData({ error: '请完整填写密码' })
      return
    }
    if (this.data.newPassword !== this.data.confirmation) {
      this.setData({ error: '两次输入的新密码不一致' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      await authApi.changePassword(this.data.currentPassword, this.data.newPassword)
      wx.showToast({ title: '修改成功', icon: 'success' })
      setTimeout(() => wx.reLaunch({ url: '/pages/login/index' }), 600)
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ loading: false })
    }
  },
})
