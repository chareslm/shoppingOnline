import { MERCHANT_PORTAL_ROLES } from '@/types/auth'
import type { WebModuleContribution } from '../types'

export const messageModule: WebModuleContribution = {
  key: 'message',
  owner: '成员 5',
  routes: [
    {
      path: 'merchant/inbox',
      name: 'merchant-inbox',
      component: () => import('./views/MerchantInboxView.vue'),
      meta: { portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES },
    },
  ],
  menuItems: [
    { to: '/merchant/inbox', label: '用户沟通', portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES, order: 40 },
  ],
}
