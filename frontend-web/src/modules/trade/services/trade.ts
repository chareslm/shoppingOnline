import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type {
  AddCartItemRequest,
  Cart,
  CreateOrderRequest,
  CreateRefundRequest,
  Order,
  PaymentOrder,
  RefundOrder,
  UpdateCheckedRequest,
  UpdateQuantityRequest,
} from '../types'

export const cartApi = {
  async get() {
    return unwrap((await http.get<ApiResponse<Cart>>('/api/cart')).data)
  },
  async addItem(payload: AddCartItemRequest) {
    return unwrap((await http.post<ApiResponse<null>>('/api/cart/items', payload)).data)
  },
  async updateQuantity(itemId: string, payload: UpdateQuantityRequest) {
    return unwrap((await http.put<ApiResponse<null>>(`/api/cart/items/${itemId}/quantity`, payload)).data)
  },
  async updateChecked(itemId: string, payload: UpdateCheckedRequest) {
    return unwrap((await http.put<ApiResponse<null>>(`/api/cart/items/${itemId}/checked`, payload)).data)
  },
  async removeItem(itemId: string) {
    return unwrap((await http.delete<ApiResponse<null>>(`/api/cart/items/${itemId}`)).data)
  },
}

export const orderApi = {
  async create(payload: CreateOrderRequest) {
    return unwrap((await http.post<ApiResponse<Order[]>>('/api/orders', payload)).data)
  },
  async list() {
    return unwrap((await http.get<ApiResponse<Order[]>>('/api/orders')).data)
  },
  async detail(orderId: string) {
    return unwrap((await http.get<ApiResponse<Order>>(`/api/orders/${orderId}`)).data)
  },
  async cancel(orderId: string) {
    return unwrap((await http.put<ApiResponse<null>>(`/api/orders/${orderId}/cancel`)).data)
  },
  async confirm(orderId: string) {
    return unwrap((await http.put<ApiResponse<null>>(`/api/orders/${orderId}/confirm`)).data)
  },
}

export const paymentApi = {
  async create(orderId: string) {
    return unwrap((await http.post<ApiResponse<PaymentOrder>>('/api/payments', { orderId })).data)
  },
  async mockPay(paymentOrderId: string) {
    return unwrap((await http.post<ApiResponse<PaymentOrder>>(`/api/payments/${paymentOrderId}/mock-pay`)).data)
  },
  async detail(paymentOrderId: string) {
    return unwrap((await http.get<ApiResponse<PaymentOrder>>(`/api/payments/${paymentOrderId}`)).data)
  },
}

export const refundApi = {
  async create(payload: CreateRefundRequest) {
    return unwrap((await http.post<ApiResponse<null>>('/api/refunds', payload)).data)
  },
  async list() {
    return unwrap((await http.get<ApiResponse<RefundOrder[]>>('/api/refunds')).data)
  },
}