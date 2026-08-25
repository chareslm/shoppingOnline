import type { AppModuleContribution } from '../../app/module-types'

export const tradeModule: AppModuleContribution = {
  key: 'trade',
  owner: '成员 4',
  pages: [
    '/pages/cart/index',
    '/pages/checkout/index',
    '/pages/payment/index',
    '/pages/orders/index',
    '/pages/order-detail/index',
  ],
}

