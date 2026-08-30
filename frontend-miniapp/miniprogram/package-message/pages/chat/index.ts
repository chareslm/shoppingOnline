import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { chatSessionApi } from '../../../features/message/data/message-api'
import { SESSION_STATUS_LABELS, type ChatSession } from '../../../features/message/domain/message-models'

Page({
  data: {
    sessions: [] as (ChatSession & { statusLabel: string })[],
    firstMessage: '',
    shopId: '',
    loading: false,
    creating: false,
    error: '',
  },

  async onShow() {
    await this.load()
  },

  async onPullDownRefresh() {
    await this.load()
    wx.stopPullDownRefresh()
  },

  onFirstMessageInput(event: { detail: { value: string } }) { this.setData({ firstMessage: event.detail.value }) },
  onShopIdInput(event: { detail: { value: string } }) { this.setData({ shopId: event.detail.value }) },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const sessions = await chatSessionApi.listMy()
      this.setData({
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

  async createSession() {
    if (!this.data.firstMessage.trim()) {
      this.setData({ error: '请输入首条咨询内容' })
      return
    }
    this.setData({ creating: true, error: '' })
    try {
      const session = await chatSessionApi.create({
        firstMessage: this.data.firstMessage.trim(),
        shopId: this.data.shopId.trim() || undefined,
        subject: '客服咨询',
      })
      this.setData({ firstMessage: '', shopId: '' })
      wx.navigateTo({ url: `/package-message/pages/thread/index?id=${session.sessionId}` })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ creating: false })
    }
  },

  openThread(event: { currentTarget: { dataset: { id?: string } } }) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/package-message/pages/thread/index?id=${id}` })
  },
})
