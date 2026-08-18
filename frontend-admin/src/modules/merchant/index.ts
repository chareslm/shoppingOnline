import { Checked, Shop } from '@element-plus/icons-vue'
import MerchantReviewQueueView from './views/MerchantReviewQueueView.vue'
import type { AdminModuleContribution } from '../types'

export const merchantModule: AdminModuleContribution = {
  key: 'merchant',
  owner: '成员 2',
  routes: [
    {
      path: 'merchant/qualification-review',
      name: 'merchant-qualification-review',
      component: MerchantReviewQueueView,
      props: { stage: 'qualification' },
      meta: { permissions: ['merchant:qualification:audit'] },
    },
    {
      path: 'merchant/account-review',
      name: 'merchant-account-review',
      component: MerchantReviewQueueView,
      props: { stage: 'account' },
      meta: { permissions: ['merchant:qualification:audit'] },
    },
  ],
  menuItems: [
    {
      index: '/merchant/qualification-review',
      label: '商家资质审核',
      icon: Shop,
      permissions: ['merchant:qualification:audit'],
    },
    {
      index: '/merchant/account-review',
      label: '商家账号审核',
      icon: Checked,
      permissions: ['merchant:qualification:audit'],
    },
  ],
}
