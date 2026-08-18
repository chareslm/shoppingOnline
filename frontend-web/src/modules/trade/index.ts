import PlaceholderPage from '@/components/PlaceholderPage.vue'
import { MERCHANT_PORTAL_ROLES } from '@/types/auth'
import type { WebModuleContribution } from '../types'

export const tradeModule: WebModuleContribution = {
  key: 'trade',
  owner: '成员 4',
  routes: [
    { path: 'cart', name: 'trade-cart', component: () => import('./views/CartView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'checkout', name: 'trade-checkout', component: () => import('./views/CheckoutView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'orders', name: 'trade-orders', component: () => import('./views/OrderListView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'orders/:orderId', name: 'trade-order-detail', component: () => import('./views/OrderDetailView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'pay/:paymentOrderId', name: 'trade-payment', component: () => import('./views/PaymentView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    {
      path: 'merchant/orders',
      name: 'merchant-orders',
      component: PlaceholderPage,
      props: {
        eyebrow: 'MERCHANT TRADE',
        title: '订单',
        description: '本店订单、发货、售后与退款处理将在此接入。',
      },
      meta: { portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES },
    },
  ],
  menuItems: [
    { to: '/cart', label: '购物车', portalModes: ['user'], roles: ['USER'] },
    { to: '/orders', label: '我的订单', portalModes: ['user'], roles: ['USER'] },
    { to: '/merchant/orders', label: '订单', portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES, order: 50 },
  ],
}
