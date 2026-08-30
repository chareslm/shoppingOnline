import {
  enterAuthenticatedPortal,
  hasMerchantRole,
  hasUserRole,
  writePortalMode,
  type PortalMode,
} from '../../core/auth/access'
import { errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'

Page({
  data: {
    identifier: '',
    password: '',
    loading: false,
    error: '',
    portalMode: 'user' as PortalMode,
  },

  onLoad(options: Record<string, string | undefined>) {
    const next: Partial<{ identifier: string; portalMode: PortalMode }> = {}
    if (options.identifier) next.identifier = decodeURIComponent(options.identifier)
    if (options.portal === 'merchant') next.portalMode = 'merchant'
    if (Object.keys(next).length) this.setData(next)
  },

  onIdentifierInput(event: { detail: { value: string } }) {
    this.setData({ identifier: event.detail.value })
  },

  onPasswordInput(event: { detail: { value: string } }) {
    this.setData({ password: event.detail.value })
  },

  selectUser() {
    this.setData({ portalMode: 'user', error: '' })
  },

  selectMerchant() {
    this.setData({ portalMode: 'merchant', error: '' })
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
      const portalMode = this.data.portalMode
      const allowed = portalMode === 'user' ? hasUserRole(user) : hasMerchantRole(user)
      if (!allowed) {
        try {
          await authApi.logout()
        } catch {
          /* 本地仍需清除错误身份会话 */
        }
        this.setData({ error: portalMode === 'user' ? '该账号不具备用户身份' : '该账号不具备商家身份' })
        return
      }
      writePortalMode(portalMode)
      enterAuthenticatedPortal(user, portalMode)
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ loading: false })
    }
  },

  goRegister() {
    const query = this.data.portalMode === 'merchant' ? '?account=merchant' : ''
    wx.navigateTo({ url: `/pages/register/index${query}` })
  },
})
