import type { AppModuleContribution } from '../../app/module-types'

export const merchantModule: AppModuleContribution = {
  key: 'merchant',
  owner: '成员 2',
  pages: [
    '/package-merchant/pages/home/index',
    '/package-merchant/pages/products/index',
    '/package-merchant/pages/add-product/index',
    '/package-merchant/pages/staff/index',
    '/package-merchant/pages/stats/index',
    '/package-merchant/pages/inbox/index',
  ],
}
