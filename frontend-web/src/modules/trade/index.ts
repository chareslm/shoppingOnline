import type { WebModuleContribution } from '../types'

export const tradeModule: WebModuleContribution = {
  key: 'trade',
  owner: '成员 4',
  routes: [
    { path: 'cart', name: 'trade-cart', component: () => import('./views/CartView.vue') },
    { path: 'checkout', name: 'trade-checkout', component: () => import('./views/CheckoutView.vue') },
    { path: 'orders', name: 'trade-orders', component: () => import('./views/OrderListView.vue') },
    { path: 'orders/:orderId', name: 'trade-order-detail', component: () => import('./views/OrderDetailView.vue') },
    { path: 'pay/:paymentOrderId', name: 'trade-payment', component: () => import('./views/PaymentView.vue') },
  ],
  menuItems: [
    { to: '/cart', label: '购物车' },
    { to: '/orders', label: '我的订单' },
  ],
}