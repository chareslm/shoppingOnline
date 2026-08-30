import { errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'
import { readPortalMode } from '../../core/auth/access'

Page({
  data: {
    currentPassword: '',
    newPassword: '',
    confirmation: '',
    loading: false,
    error: '',
    forced: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    this.setData({ forced: options.forced === '1' })
  },

  onCurrentInput(event: { detail: { value: string } }) { this.setData({ currentPassword: event.detail.value }) },
  onNewInput(event: { detail: { value: string } }) { this.setData({ newPassword: event.detail.value }) },
  onConfirmationInput(event: { detail: { value: string } }) { this.setData({ confirmation: event.detail.value }) },

  async submit() {
    const error = validatePasswordChange(
      this.data.currentPassword,
      this.data.newPassword,
      this.data.confirmation,
      this.data.forced,
    )
    if (error) {
      this.setData({ error })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      await authApi.changePassword(this.data.currentPassword, this.data.newPassword)
      wx.showToast({ title: '修改成功', icon: 'success' })
      const portal = readPortalMode() === 'merchant' ? '?portal=merchant' : ''
      setTimeout(() => wx.reLaunch({ url: `/pages/login/index${portal}` }), 600)
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ loading: false })
    }
  },
})

function validatePasswordChange(
  currentPassword: string,
  newPassword: string,
  confirmation: string,
  forced: boolean,
): string {
  if (!currentPassword) return forced ? '请输入邮件中的临时密码' : '请输入当前密码'
  if (newPassword.length < 12 || newPassword.length > 64) return '新密码长度须为 12–64 个字符'
  if (!/[a-z]/.test(newPassword) || !/[A-Z]/.test(newPassword) || !/\d/.test(newPassword) || !/[^A-Za-z0-9\s]/.test(newPassword)) {
    return '新密码须同时包含大写字母、小写字母、数字和特殊字符'
  }
  if (newPassword === currentPassword) return forced ? '新密码不能与临时密码相同' : '新密码不能与当前密码相同'
  if (newPassword !== confirmation) return '两次输入的新密码不一致'
  return ''
}
