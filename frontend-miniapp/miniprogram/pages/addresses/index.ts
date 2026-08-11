import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { userApi } from '../../features/account/data/user-api'
import type { UserAddress } from '../../features/account/domain/user-models'

Page({
  data: {
    addresses: [] as UserAddress[],
    loading: false,
    error: '',
  },

  async onShow() {
    await this.loadAddresses()
  },

  async onPullDownRefresh() {
    await this.loadAddresses()
    wx.stopPullDownRefresh()
  },

  async loadAddresses() {
    this.setData({ loading: true, error: '' })
    try {
      this.setData({ addresses: await userApi.addresses() })
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  createAddress() {
    wx.navigateTo({ url: '/pages/address-edit/index' })
  },

  editAddress(event: { currentTarget: { dataset: { id?: number } } }) {
    const id = event.currentTarget.dataset.id
    if (id) wx.navigateTo({ url: `/pages/address-edit/index?id=${id}` })
  },

  async setDefault(event: { currentTarget: { dataset: { id?: number } } }) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    try {
      await userApi.setDefaultAddress(id)
      await this.loadAddresses()
    } catch (error) {
      this.handleError(error)
    }
  },

  async deleteAddress(event: { currentTarget: { dataset: { id?: number } } }) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    const result = await wx.showModal({ title: '删除地址', content: '确认删除这条收货地址吗？' })
    if (!result.confirm) return
    try {
      await userApi.deleteAddress(id)
      wx.showToast({ title: '已删除', icon: 'success' })
      await this.loadAddresses()
    } catch (error) {
      this.handleError(error)
    }
  },

  handleError(error: unknown) {
    this.setData({ error: errorMessage(error) })
    if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
  },
})

