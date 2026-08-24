import { MERCHANT_OWNER_ROLE } from '@/types/auth'
import type { WebModuleContribution } from '../types'

export const merchantModule: WebModuleContribution = {
  key: 'merchant',
  owner: '成员 2',
  routes: [
    {
      path: 'merchant/service-accounts',
      name: 'merchant-service-accounts',
      component: () => import('./views/MerchantServiceAccountsView.vue'),
      meta: { portalModes: ['merchant'], roles: [MERCHANT_OWNER_ROLE] },
    },
  ],
  menuItems: [
    { to: '/merchant/service-accounts', label: '客服账号', portalModes: ['merchant'], roles: [MERCHANT_OWNER_ROLE], order: 30 },
  ],
}
