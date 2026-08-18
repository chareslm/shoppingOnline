import PlaceholderPage from '@/components/PlaceholderPage.vue'
import { MERCHANT_PORTAL_ROLES } from '@/types/auth'
import type { WebModuleContribution } from '../types'

const merchantMeta = { portalModes: ['merchant'] as const, roles: MERCHANT_PORTAL_ROLES }

export const merchantModule: WebModuleContribution = {
  key: 'merchant',
  owner: '成员 2',
  routes: [
    {
      path: 'merchant/service-accounts',
      name: 'merchant-service-accounts',
      component: PlaceholderPage,
      props: {
        eyebrow: 'MERCHANT SERVICE',
        title: '客服账号',
        description: '店铺客服账号、在线状态和技能组将在此接入。',
      },
      meta: merchantMeta,
    },
  ],
  menuItems: [
    { to: '/merchant/service-accounts', label: '客服账号', portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES, order: 30 },
  ],
}
