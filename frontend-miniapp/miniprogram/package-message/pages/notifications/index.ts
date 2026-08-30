import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { notificationApi } from '../../../features/message/data/message-api'
import { NOTIFICATION_CATEGORY_LABELS, type NotificationItem } from '../../../features/message/domain/message-models'

Page({
  data: {
    items: [] as NotificationItem[],
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
      const items = await notificationApi.list()
      this.setData({ items })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ loading: false })
    }
  },

  categoryLabel(category: number): string {
    return NOTIFICATION_CATEGORY_LABELS[category] ?? '通知'
  },

  async markRead(event: { currentTarget: { dataset: { id?: string } } }) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    try {
      await notificationApi.markRead(id)
      this.setData({
        items: this.data.items.map((item) => (item.id === id ? { ...item, isRead: 1 } : item)),
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  async markAll() {
    try {
      await notificationApi.markAllRead()
      this.setData({ items: this.data.items.map((item) => ({ ...item, isRead: 1 })) })
      wx.showToast({ title: '已全部已读' })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },
})
