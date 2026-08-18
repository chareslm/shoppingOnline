import { Box, Goods } from '@element-plus/icons-vue'
import PlaceholderView from '../../views/PlaceholderView.vue'
import type { AdminModuleContribution } from '../types'

const platformMeta = { roles: ['ADMIN', 'SUPER_ADMIN'] as const, adminModes: ['platform'] as const }

export const productModule: AdminModuleContribution = {
  key: 'product',
  owner: '成员 3',
  routes: [
    {
      path: 'product/audit',
      name: 'product-audit',
      component: PlaceholderView,
      props: {
        eyebrow: 'PRODUCT GOVERNANCE',
        title: '商品审核',
        description: '待审商品、审核结论与驳回原因将在此接入。',
      },
      meta: platformMeta,
    },
    {
      path: 'product/catalog',
      name: 'product-catalog',
      component: PlaceholderView,
      props: {
        eyebrow: 'PRODUCT GOVERNANCE',
        title: '全部商品',
        description: '全平台商品检索、上下架状态与类目查看将在此接入。',
      },
      meta: platformMeta,
    },
  ],
  menuItems: [
    { index: '/product/audit', label: '商品审核', icon: Goods, roles: ['ADMIN', 'SUPER_ADMIN'], adminModes: ['platform'] },
    { index: '/product/catalog', label: '全部商品', icon: Box, roles: ['ADMIN', 'SUPER_ADMIN'], adminModes: ['platform'] },
  ],
}
