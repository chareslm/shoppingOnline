import { Box, FolderOpened, Goods } from '@element-plus/icons-vue'
import type { AdminModuleContribution } from '../types'

const platformMeta = { roles: ['ADMIN', 'SUPER_ADMIN'] as const, adminModes: ['platform'] as const }

export const productModule: AdminModuleContribution = {
  key: 'product',
  owner: '成员 3',
  routes: [
    {
      path: 'product/categories',
      name: 'product-categories',
      component: () => import('./views/CategoryManageView.vue'),
      meta: { ...platformMeta, permissions: ['category:manage'] },
    },
    {
      path: 'product/audit',
      name: 'product-audit',
      component: () => import('./views/ProductAuditView.vue'),
      meta: { ...platformMeta, permissions: ['product:audit'] },
    },
    {
      path: 'product/catalog',
      name: 'product-catalog',
      component: () => import('./views/ProductCatalogView.vue'),
      meta: { ...platformMeta, permissions: ['product:audit'] },
    },
  ],
  menuItems: [
    { index: '/product/categories', label: '商品类目', icon: FolderOpened, roles: ['ADMIN', 'SUPER_ADMIN'], adminModes: ['platform'], permissions: ['category:manage'] },
    { index: '/product/audit', label: '商品审核', icon: Goods, roles: ['ADMIN', 'SUPER_ADMIN'], adminModes: ['platform'], permissions: ['product:audit'] },
    { index: '/product/catalog', label: '全部商品', icon: Box, roles: ['ADMIN', 'SUPER_ADMIN'], adminModes: ['platform'], permissions: ['product:audit'] },
  ],
}
