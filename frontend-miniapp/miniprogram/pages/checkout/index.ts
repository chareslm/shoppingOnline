import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import type { UserAddress } from '../../features/account/domain/user-models'
import { userApi } from '../../features/account/data/user-api'
import { cartApi, orderApi, paymentApi } from '../../features/trade/data/trade-api'
import type { CartItem } from '../../features/trade/domain/trade-models'

interface CheckoutItemView {
  itemId: string
  name: string
  quantity: number
  subtotalText: string
}

Page({
  data: {
    addresses: [] as UserAddress[],
    selectedAddressId: '',
    items: [] as CheckoutItemView[],
    itemCount: 0,
    totalAmount: '0.00',
    remark: '',
    loading: false,
    submitting: false,
    error: '',
  },

  async onShow() {
    await this.loadCheckout()
  },

  async loadCheckout() {
    this.setData({ loading: true, error: '' })
    try {
      const [cart, addresses] = await Promise.all([cartApi.get(), userApi.addresses()])
      const checked: CartItem[] = []
      for (const group of cart.groups) {
        for (const item of group.items) {
          if (item.checked === 1) checked.push(item)
        }
      }
      const defaultAddress = addresses.find((address) => address.isDefault) || addresses[0]
      this.setData({
        addresses,
        selectedAddressId: defaultAddress ? defaultAddress.id : '',
        items: checked.map((item) => ({
          itemId: item.itemId,
          name: item.skuName || `SKU ${item.skuId}`,
          quantity: item.quantity,
          subtotalText: (item.price * item.quantity).toFixed(2),
        })),
        itemCount: checked.reduce((sum, item) => sum + item.quantity, 0),
        totalAmount: checked.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2),
      })
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  selectAddress(event: { detail: { value: string } }) {
    this.setData({ selectedAddressId: event.detail.value })
  },

  onRemarkInput(event: { detail: { value: string } }) {
    this.setData({ remark: event.detail.value })
  },

  goAddresses() {
    wx.navigateTo({ url: '/pages/addresses/index' })
  },

  async submit() {
    const address = this.data.addresses.find((candidate) => candidate.id === this.data.selectedAddressId)
    if (!address) {
      wx.showToast({ title: '请先选择收货地址', icon: 'none' })
      return
    }
    if (this.data.submitting) return
    this.setData({ submitting: true, error: '' })
    try {
      const orders = await orderApi.create({
        receiverName: address.recipientName,
        receiverPhone: address.recipientPhone,
        receiverAddress: `${address.provinceName}${address.cityName}${address.districtName}${address.detailAddress}`,
        remark: this.data.remark.trim() || undefined,
      })
      const payment = await paymentApi.create(orders[0].orderId)
      wx.redirectTo({ url: `/pages/payment/index?id=${payment.paymentOrderId}` })
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
