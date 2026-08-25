import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { orderApi, paymentApi, refundApi } from '../../features/trade/data/trade-api'
import { ORDER_STATUS_LABELS, orderStatusClass } from '../../features/trade/domain/trade-models'
import type { Order } from '../../features/trade/domain/trade-models'

interface DetailItemView {
  itemId: string
  name: string
  image: string
  priceText: string
  quantity: number
  subtotalText: string
}

interface KeyValueRow {
  label: string
  value: string
}

Page({
  data: {
    order: null as Order | null,
    orderNo: '',
    statusLabel: '',
    statusClass: '',
    receiverLine: '',
    receiverAddress: '',
    remark: '',
    items: [] as DetailItemView[],
    amounts: [] as KeyValueRow[],
    times: [] as KeyValueRow[],
    canPay: false,
    canCancel: false,
    canConfirm: false,
    canRefund: false,
    loading: false,
    acting: false,
    error: '',
  },

  orderId: '',

  onLoad(options: { id?: string }) {
    this.orderId = options.id || ''
  },

  async onShow() {
    await this.loadDetail()
  },

  async loadDetail() {
    if (!this.orderId) {
      this.setData({ error: '缺少订单号' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const order = await orderApi.detail(this.orderId)
      const times: KeyValueRow[] = []
      if (order.payTime) times.push({ label: '支付时间', value: order.payTime })
      if (order.finishTime) times.push({ label: '完成时间', value: order.finishTime })
      if (order.closeTime) times.push({ label: '关闭时间', value: order.closeTime })
      this.setData({
        order,
        orderNo: order.orderNo,
        statusLabel: ORDER_STATUS_LABELS[order.status] || '未知状态',
        statusClass: orderStatusClass(order.status),
        receiverLine: `${order.receiverName}　${order.receiverPhone}`,
        receiverAddress: order.receiverAddress,
        remark: order.remark || '',
        items: order.items.map((item) => ({
          itemId: item.itemId,
          name: item.skuName || `SKU ${item.skuId}`,
          image: item.skuImage || '',
          priceText: item.price.toFixed(2),
          quantity: item.quantity,
          subtotalText: item.totalAmount.toFixed(2),
        })),
        amounts: [
          { label: '商品总额', value: `¥${order.totalAmount.toFixed(2)}` },
          { label: '优惠', value: `-¥${order.discountAmount.toFixed(2)}` },
          { label: '运费', value: `¥${order.freightAmount.toFixed(2)}` },
          { label: '实付', value: `¥${order.payAmount.toFixed(2)}` },
        ],
        times,
        canPay: order.status === 0,
        canCancel: order.status === 0,
        canConfirm: order.status === 2,
        canRefund: order.status === 1 || order.status === 2,
      })
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  async payNow() {
    if (!this.orderId || this.data.acting) return
    this.setData({ acting: true, error: '' })
    try {
      const payment = await paymentApi.create(this.orderId)
      wx.redirectTo({ url: `/pages/payment/index?id=${payment.paymentOrderId}` })
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ acting: false })
    }
  },

  async cancelOrder() {
    if (!this.orderId || this.data.acting) return
    const result = await wx.showModal({ title: '取消订单', content: '确认取消这个待支付订单吗？' })
    if (!result.confirm) return
    this.setData({ acting: true, error: '' })
    try {
      await orderApi.cancel(this.orderId)
      wx.showToast({ title: '已取消', icon: 'success' })
      await this.loadDetail()
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ acting: false })
    }
  },

  async confirmReceipt() {
    if (!this.orderId || this.data.acting) return
    const result = await wx.showModal({ title: '确认收货', content: '请确认已收到全部商品。' })
    if (!result.confirm) return
    this.setData({ acting: true, error: '' })
    try {
      await orderApi.confirm(this.orderId)
      wx.showToast({ title: '已确认收货', icon: 'success' })
      await this.loadDetail()
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ acting: false })
    }
  },

  async applyRefund() {
    if (!this.orderId || this.data.acting) return
    const result = await wx.showModal({
      title: '申请退款',
      content: '将为整笔订单提交退款申请。',
      editable: true,
      placeholderText: '请输入退款原因（可选）',
    })
    if (!result.confirm) return
    this.setData({ acting: true, error: '' })
    try {
      await refundApi.create(this.orderId, result.content?.trim() || undefined)
      wx.showToast({ title: '退款申请已提交', icon: 'success' })
      await this.loadDetail()
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ acting: false })
    }
  },

  handleError(error: unknown) {
    this.setData({ error: errorMessage(error) })
    if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
  },
})
