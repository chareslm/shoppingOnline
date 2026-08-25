import { apiRequest } from '../../../core/http/api-client'
import type {
  AddCartItemRequest,
  Cart,
  CreateOrderRequest,
  Order,
  PaymentOrder,
} from '../domain/trade-models'

export const cartApi = {
  get(): Promise<Cart> {
    return apiRequest({ path: '/api/cart' })
  },

  addItem(input: AddCartItemRequest): Promise<void> {
    return apiRequest({ path: '/api/cart/items', method: 'POST', data: input })
  },

  updateQuantity(itemId: string, quantity: number): Promise<void> {
    return apiRequest({
      path: `/api/cart/items/${itemId}/quantity`,
      method: 'PUT',
      data: { quantity },
    })
  },

  updateChecked(itemId: string, checked: boolean): Promise<void> {
    return apiRequest({
      path: `/api/cart/items/${itemId}/checked`,
      method: 'PUT',
      // 后端契约：checked 为 0/1 整数，直接发布尔会反序列化失败
      data: { checked: checked ? 1 : 0 },
    })
  },

  removeItem(itemId: string): Promise<void> {
    return apiRequest({ path: `/api/cart/items/${itemId}`, method: 'DELETE' })
  },
}

export const orderApi = {
  create(input: CreateOrderRequest): Promise<Order[]> {
    return apiRequest({ path: '/api/orders', method: 'POST', data: input })
  },

  list(): Promise<Order[]> {
    return apiRequest({ path: '/api/orders' })
  },

  detail(orderId: string): Promise<Order> {
    return apiRequest({ path: `/api/orders/${orderId}` })
  },

  cancel(orderId: string): Promise<void> {
    return apiRequest({ path: `/api/orders/${orderId}/cancel`, method: 'PUT' })
  },

  confirm(orderId: string): Promise<void> {
    return apiRequest({ path: `/api/orders/${orderId}/confirm`, method: 'PUT' })
  },
}

export const paymentApi = {
  create(orderId: string): Promise<PaymentOrder> {
    return apiRequest({ path: '/api/payments', method: 'POST', data: { orderId } })
  },

  mockPay(paymentOrderId: string): Promise<PaymentOrder> {
    return apiRequest({ path: `/api/payments/${paymentOrderId}/mock-pay`, method: 'POST' })
  },

  detail(paymentOrderId: string): Promise<PaymentOrder> {
    return apiRequest({ path: `/api/payments/${paymentOrderId}` })
  },
}

export const refundApi = {
  create(orderId: string, reason?: string): Promise<void> {
    const data: Record<string, string> = { orderId }
    if (reason) data.reason = reason
    return apiRequest({ path: '/api/refunds', method: 'POST', data })
  },
}
