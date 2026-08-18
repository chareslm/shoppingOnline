import PlaceholderPage from '@/components/PlaceholderPage.vue'
import { MERCHANT_PORTAL_ROLES } from '@/types/auth'
import type { WebModuleContribution } from '../types'

export const messageModule: WebModuleContribution = {
  key: 'message',
  owner: '成员 5',
  routes: [
    {
      path: 'merchant/inbox',
      name: 'merchant-inbox',
      component: PlaceholderPage,
      props: {
        eyebrow: 'MERCHANT MESSAGE',
        title: '用户沟通',
        description: '与用户的客服会话、转接和处理记录将在此接入。',
      },
      meta: { portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES },
    },
  ],
  menuItems: [
    { to: '/merchant/inbox', label: '用户沟通', portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES, order: 40 },
  ],
}
