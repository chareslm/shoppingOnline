import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { merchantApi } from '../../../features/merchant/data/merchant-api'
import { STAFF_STATUS_LABELS, type ShopStaffAccount } from '../../../features/merchant/domain/merchant-models'

Page({
  data: {
    shopName: '',
    items: [] as (ShopStaffAccount & { statusLabel: string })[],
    displayName: '',
    email: '',
    username: '',
    loading: false,
    submitting: false,
    error: '',
  },

  async onShow() {
    await this.load()
  },

  async onPullDownRefresh() {
    await this.load()
    wx.stopPullDownRefresh()
  },

  onDisplayNameInput(event: { detail: { value: string } }) { this.setData({ displayName: event.detail.value }) },
  onEmailInput(event: { detail: { value: string } }) { this.setData({ email: event.detail.value }) },
  onUsernameInput(event: { detail: { value: string } }) { this.setData({ username: event.detail.value }) },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [staff, shop] = await Promise.all([
        merchantApi.listStaff(),
        merchantApi.currentShop().catch(() => null),
      ])
      this.setData({
        shopName: shop?.name ?? '',
        items: staff.map((item) => ({ ...item, statusLabel: STAFF_STATUS_LABELS[item.status] })),
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ loading: false })
    }
  },

  async createStaff() {
    if (!this.data.displayName.trim() || !this.data.email.trim()) {
      this.setData({ error: '请填写客服显示名和邮箱' })
      return
    }
    this.setData({ submitting: true, error: '' })
    try {
      await merchantApi.createStaff({
        displayName: this.data.displayName.trim(),
        email: this.data.email.trim(),
        username: this.data.username.trim() || undefined,
      })
      this.setData({ displayName: '', email: '', username: '' })
      wx.showToast({ title: '已提交审核' })
      await this.load()
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ submitting: false })
    }
  },

  async retry(event: { currentTarget: { dataset: { id?: string } } }) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    try {
      await merchantApi.retryStaffEmail(id)
      wx.showToast({ title: '已重发邮件' })
      await this.load()
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },
})
