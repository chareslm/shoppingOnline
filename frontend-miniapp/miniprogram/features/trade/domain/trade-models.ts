// 交易模块类型，与 frontend-web/src/modules/trade/types.ts 及后端契约一致。
// 后端雪花 ID 以字符串形式返回（避免超出 JS Number 安全范围），ID 字段统一为 string。

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

// ---------- 订单 ----------

export interface OrderItem {
  itemId: string
  skuId: string
  skuName: string | null
  skuImage: string | null
  price: number
  quantity: number
  totalAmount: number
}

export interface Order {
  orderId: string
  orderNo: string
  /** 0 待支付 / 1 已支付 / 2 已发货 / 3 已完成 / 4 已取消 / 5 已关闭（超时） / 6 退款中 / 7 退款完成 */
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

// ---------- 状态展示辅助 ----------

export const ORDER_STATUS_LABELS: Record<number, string> = {
  0: '待支付',
  1: '已支付',
  2: '已发货',
  3: '已完成',
  4: '已取消',
  5: '已关闭',
  6: '退款中',
  7: '退款完成',
}

export const PAYMENT_STATUS_LABELS: Record<number, string> = {
  0: '待支付',
  1: '已支付',
}

/** 订单状态对应的徽章配色类名，供订单列表与详情共用 */
export function orderStatusClass(status: number): string {
  if (status === 0) return 'warning'
  if (status === 1 || status === 2) return 'info'
  if (status === 3) return 'success'
  if (status === 6 || status === 7) return 'refund'
  return 'closed'
}
