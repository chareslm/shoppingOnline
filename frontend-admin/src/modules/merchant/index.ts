import { Shop } from '@element-plus/icons-vue'
import MerchantReviewView from './views/MerchantReviewView.vue'
import type { AdminModuleContribution } from '../types'

const platformAdmin = { permissions: ['merchant:qualification:audit'], roles: ['ADMIN', 'SUPER_ADMIN'], adminModes: ['platform'] as const }

export const merchantModule: AdminModuleContribution = {
  key: 'merchant',
  owner: '成员 2',
  routes: [
    {
      path: 'merchant/review',
      name: 'merchant-review',
      component: MerchantReviewView,
      meta: platformAdmin,
    },
    {
      path: 'merchant/qualification-review',
      redirect: { name: 'merchant-review' },
    },
    {
      path: 'merchant/account-review',
      redirect: { name: 'merchant-review' },
    },
  ],
  menuItems: [
    {
      index: '/merchant/review',
      label: '商家审核',
      icon: Shop,
      permissions: ['merchant:qualification:audit'],
      roles: ['ADMIN', 'SUPER_ADMIN'],
      adminModes: ['platform'],
    },
  ],
}
