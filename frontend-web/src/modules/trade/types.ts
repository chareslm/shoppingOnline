// 交易模块前端类型，与 docs/api/cart.md、docs/api/trade.md、docs/api/payment.md 契约一致。
// 注意：后端雪花 ID 以字符串形式返回（避免超出 JS Number 安全范围），ID 字段统一为 string。

// ---------- 购物车 ----------

export interface CartItem {
  itemId: string
  skuId: string
  skuName: string | null
  skuImage: string | null
  price: number
  quantity: number
  /** 0 未勾选 / 1 已勾选 */
  checked: number
  groupId: string
}

export interface CartGroup {
  groupId: string
  shopId: string
  shopName: string | null
  items: CartItem[]
}

export interface Cart {
  cartId: string
  groups: CartGroup[]
}

export interface AddCartItemRequest {
  skuId: string
  quantity: number
  shopId: string
  price: number
}

export interface UpdateQuantityRequest {
  quantity: number
}

export interface UpdateCheckedRequest {
  checked: boolean
}

// ---------- 订单 ----------

export interface OrderItem {
  itemId: string
  skuId: string
  skuName: string | null
  skuImage: string | null
  price: number
  quantity: number
  status: number
  totalAmount: number
}

export interface Order {
  orderId: string
  orderNo: string
  /** 0 待支付 / 1 已支付 / 2 已发货 / 3 已完成 / 4 已取消 / 6 退款中 */
  status: number
  totalAmount: number
  discountAmount: number
  freightAmount: number
  payAmount: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark: string | null
  closeTime: string | null
  payTime: string | null
  finishTime: string | null
  items: OrderItem[]
}

export interface CreateOrderRequest {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
}

// ---------- 支付 ----------

export interface PaymentOrder {
  paymentOrderId: string
  paymentNo: string
  orderId: string
  userId: string
  amount: number
  payChannel: string
  /** 0 待支付 / 1 已支付 */
  status: number
  payTime: string | null
  expireTime: string | null
}

// ---------- 退款 ----------

export interface RefundOrder {
  refundId: string
  refundNo: string
  orderId: string
  paymentOrderId: string
  amount: number
  reason: string | null
  /** 0 已提交 / 1 退款成功 */
  status: number
  refundTime: string | null
  createdAt: string
}

export interface CreateRefundRequest {
  orderId: string
  amount?: number
  reason?: string
}

// ---------- 状态展示辅助 ----------

export const ORDER_STATUS_LABELS: Record<number, string> = {
  0: '待支付',
  1: '已支付',
  2: '已发货',
  3: '已完成',
  4: '已取消',
  6: '退款中',
}

export const PAYMENT_STATUS_LABELS: Record<number, string> = {
  0: '待支付',
  1: '已支付',
}

export const REFUND_STATUS_LABELS: Record<number, string> = {
  0: '处理中',
  1: '退款成功',
}