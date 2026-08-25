import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { cartApi } from '../../features/trade/data/trade-api'
import type { CartGroup } from '../../features/trade/domain/trade-models'

Page({
  data: {
    groups: [] as CartGroup[],
    loading: false,
    error: '',
    checkedCount: 0,
    checkedAmount: '0.00',
    helperOpen: false,
    manual: { skuId: '', shopId: '', price: '', quantity: '1' },
    submitting: false,
  },

  async onShow() {
    await this.loadCart()
  },

  async onPullDownRefresh() {
    await this.loadCart()
    wx.stopPullDownRefresh()
  },

  async loadCart() {
    this.setData({ loading: true, error: '' })
    try {
      const cart = await cartApi.get()
      this.setData({ groups: cart.groups })
      this.refreshSummary()
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  refreshSummary() {
    let count = 0
    let amount = 0
    for (const group of this.data.groups) {
      for (const item of group.items) {
        if (item.checked === 1) {
          count += item.quantity
          amount += item.price * item.quantity
        }
      }
    }
    this.setData({ checkedCount: count, checkedAmount: amount.toFixed(2) })
  },

  async toggleChecked(event: { currentTarget: { dataset: { id?: string } }, detail: { value: boolean } }) {
    const itemId = event.currentTarget.dataset.id
    if (!itemId) return
    try {
      await cartApi.updateChecked(itemId, event.detail.value)
      await this.loadCart()
    } catch (error) {
      this.handleError(error)
    }
  },

  async changeQuantity(event: { currentTarget: { dataset: { id?: string, delta?: string } } }) {
    const itemId = event.currentTarget.dataset.id
    const delta = Number(event.currentTarget.dataset.delta)
    if (!itemId || !delta) return
    const item = this.findItem(itemId)
    if (!item) return
    const quantity = item.quantity + delta
    if (quantity < 1) return
    try {
      await cartApi.updateQuantity(itemId, quantity)
      await this.loadCart()
    } catch (error) {
      this.handleError(error)
    }
  },

  findItem(itemId: string) {
    for (const group of this.data.groups) {
      const item = group.items.find((candidate) => candidate.itemId === itemId)
      if (item) return item
    }
    return undefined
  },

  async removeItem(event: { currentTarget: { dataset: { id?: string } } }) {
    const itemId = event.currentTarget.dataset.id
    if (!itemId) return
    const result = await wx.showModal({ title: '删除商品', content: '确认把这个商品从购物车删除吗？' })
    if (!result.confirm) return
    try {
      await cartApi.removeItem(itemId)
      wx.showToast({ title: '已删除', icon: 'success' })
      await this.loadCart()
    } catch (error) {
      this.handleError(error)
    }
  },

  goCheckout() {
    if (this.data.checkedCount === 0) return
    wx.navigateTo({ url: '/pages/checkout/index' })
  },

  toggleHelper() {
    this.setData({ helperOpen: !this.data.helperOpen })
  },

  onManualInput(event: { currentTarget: { dataset: { field?: string } }, detail: { value: string } }) {
    const field = event.currentTarget.dataset.field
    const value = event.detail.value
    if (field === 'skuId') this.setData({ 'manual.skuId': value })
    else if (field === 'shopId') this.setData({ 'manual.shopId': value })
    else if (field === 'price') this.setData({ 'manual.price': value })
    else if (field === 'quantity') this.setData({ 'manual.quantity': value })
  },

  async submitManualAdd() {
    const skuId = this.data.manual.skuId.trim()
    const shopId = this.data.manual.shopId.trim()
    const price = Number(this.data.manual.price)
    const quantity = Number(this.data.manual.quantity)
    if (!skuId || !shopId || !Number.isFinite(price) || price <= 0 || !Number.isInteger(quantity) || quantity < 1) {
      wx.showToast({ title: '请填写完整且正确的商品信息', icon: 'none' })
      return
    }
    this.setData({ submitting: true, error: '' })
    try {
      await cartApi.addItem({ skuId, quantity, shopId, price })
      wx.showToast({ title: '已加入购物车', icon: 'success' })
      this.setData({ manual: { skuId: '', shopId: '', price: '', quantity: '1' } })
      await this.loadCart()
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ submitting: false })
    }
  },

  handleError(error: unknown) {
    this.setData({ error: errorMessage(error) })
    if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
  },
})
