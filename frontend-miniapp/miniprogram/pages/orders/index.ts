import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { orderApi, paymentApi } from '../../features/trade/data/trade-api'
import { ORDER_STATUS_LABELS, orderStatusClass } from '../../features/trade/domain/trade-models'
import type { Order } from '../../features/trade/domain/trade-models'

interface OrderTab {
  key: string
  label: string
}

interface OrderView {
  orderId: string
  orderNo: string
  statusLabel: string
  statusClass: string
  summary: string
  itemCount: number
  amountText: string
  canPay: boolean
}

const TABS: OrderTab[] = [
  { key: 'all', label: '全部' },
  { key: '0', label: '待支付' },
  { key: '1', label: '已支付' },
  { key: '2', label: '已发货' },
  { key: '3', label: '已完成' },
  { key: 'refund', label: '退款' },
]

function toOrderView(order: Order): OrderView {
  const first = order.items[0]
  const itemCount = order.items.reduce((sum, item) => sum + item.quantity, 0)
  const summary = first
    ? `${first.skuName || `SKU ${first.skuId}`}${order.items.length > 1 ? ` 等 ${order.items.length} 种商品` : ''}`
    : '无商品明细'
  return {
    orderId: order.orderId,
    orderNo: order.orderNo,
    statusLabel: ORDER_STATUS_LABELS[order.status] || '未知状态',
    statusClass: orderStatusClass(order.status),
    summary,
    itemCount,
    amountText: order.payAmount.toFixed(2),
    canPay: order.status === 0,
  }
}

Page({
  data: {
    tabs: TABS,
    activeTab: 'all',
    orders: [] as OrderView[],
    loading: false,
    error: '',
  },

  rawOrders: [] as Order[],

  async onShow() {
    await this.loadOrders()
  },

  async onPullDownRefresh() {
    await this.loadOrders()
    wx.stopPullDownRefresh()
  },

  async loadOrders() {
    this.setData({ loading: true, error: '' })
    try {
      this.rawOrders = await orderApi.list()
      this.applyFilter(this.data.activeTab)
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  selectTab(event: { currentTarget: { dataset: { key?: string } } }) {
    const key = event.currentTarget.dataset.key
    if (!key) return
    this.setData({ activeTab: key })
    this.applyFilter(key)
  },

  applyFilter(key: string) {
    const filtered = this.rawOrders.filter((order) => {
      if (key === 'all') return true
      if (key === 'refund') return order.status === 6 || order.status === 7
      return order.status === Number(key)
    })
    this.setData({ orders: filtered.map(toOrderView) })
  },

  goDetail(event: { currentTarget: { dataset: { id?: string } } }) {
    const id = event.currentTarget.dataset.id
    if (id) wx.navigateTo({ url: `/pages/order-detail/index?id=${id}` })
  },

  async goPay(event: { currentTarget: { dataset: { id?: string } } }) {
    const orderId = event.currentTarget.dataset.id
    if (!orderId) return
    try {
      const payment = await paymentApi.create(orderId)
      wx.redirectTo({ url: `/pages/payment/index?id=${payment.paymentOrderId}` })
    } catch (error) {
      wx.showToast({ title: errorMessage(error), icon: 'none' })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  handleError(error: unknown) {
    this.setData({ error: errorMessage(error) })
    if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
  },
})
