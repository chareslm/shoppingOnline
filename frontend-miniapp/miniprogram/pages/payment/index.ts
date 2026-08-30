import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { paymentApi } from '../../features/trade/data/trade-api'
import { PAYMENT_STATUS_LABELS } from '../../features/trade/domain/trade-models'
import type { PaymentOrder } from '../../features/trade/domain/trade-models'

Page({
  data: {
    payment: null as PaymentOrder | null,
    paymentNo: '',
    amountText: '0.00',
    statusLabel: '',
    paid: false,
    expireTime: '',
    payTime: '',
    paying: false,
    error: '',
  },

  paymentOrderId: '',

  onLoad(options: { id?: string }) {
    this.paymentOrderId = options.id || ''
  },

  async onShow() {
    await this.loadPayment()
  },

  async loadPayment() {
    if (!this.paymentOrderId) {
      this.setData({ error: '缺少支付单号' })
      return
    }
    try {
      const payment = await paymentApi.detail(this.paymentOrderId)
      this.setData({
        payment,
        paymentNo: payment.paymentNo,
        amountText: payment.amount.toFixed(2),
        statusLabel: PAYMENT_STATUS_LABELS[payment.status] || '未知状态',
        paid: payment.status === 1,
        expireTime: payment.expireTime || '',
        payTime: payment.payTime || '',
        error: '',
      })
    } catch (error) {
      this.handleError(error)
    }
  },

  async mockPay() {
    if (!this.paymentOrderId || this.data.paying) return
    const result = await wx.showModal({ title: '模拟支付', content: `确认支付 ¥${this.data.amountText} 吗？` })
    if (!result.confirm) return
    this.setData({ paying: true, error: '' })
    try {
      const payment = await paymentApi.mockPay(this.paymentOrderId)
      wx.showToast({ title: '支付成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: `/pages/order-detail/index?id=${payment.orderId}` })
      }, 800)
    } catch (error) {
      this.handleError(error)
      await this.loadPayment()
    } finally {
      this.setData({ paying: false })
    }
  },

  handleError(error: unknown) {
    this.setData({ error: errorMessage(error) })
    if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
  },
})
