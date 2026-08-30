import { isCustomerServiceOnly, redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { authApi } from '../../../features/account/data/auth-api'
import { chatSessionApi } from '../../../features/message/data/message-api'
import { SESSION_STATUS_LABELS, type ChatSession } from '../../../features/message/domain/message-models'

Page({
  data: {
    sessions: [] as (ChatSession & { statusLabel: string })[],
    forCs: false,
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
    this.setData({ loading: true, error: '' })
    try {
      const user = await authApi.me()
      const forCs = isCustomerServiceOnly(user.roles)
      const sessions = forCs ? await chatSessionApi.listCs() : await chatSessionApi.listMy()
      this.setData({
        forCs,
        sessions: sessions.map((item) => ({
          ...item,
          statusLabel: SESSION_STATUS_LABELS[item.status] ?? String(item.status),
        })),
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ loading: false })
    }
  },

  async assign(event: { currentTarget: { dataset: { id?: string } } }) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    try {
      await chatSessionApi.assign(id)
      wx.showToast({ title: '已领取' })
      await this.load()
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  openThread(event: { currentTarget: { dataset: { id?: string } } }) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/package-message/pages/thread/index?id=${id}` })
  },
})
