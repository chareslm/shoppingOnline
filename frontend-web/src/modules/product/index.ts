import PlaceholderPage from '@/components/PlaceholderPage.vue'
import { MERCHANT_PORTAL_ROLES } from '@/types/auth'
import type { WebModuleContribution } from '../types'

const merchantMeta = { portalModes: ['merchant'] as const, roles: MERCHANT_PORTAL_ROLES }

export const productModule: WebModuleContribution = {
  key: 'product',
  owner: '成员 3',
  routes: [
    { path: 'products', name: 'product-list', component: () => import('./views/ProductListView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'products/:spuId', name: 'product-detail', component: () => import('./views/ProductDetailView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    {
      path: 'merchant/add-product',
      name: 'merchant-add-product',
      component: PlaceholderPage,
      props: {
        eyebrow: 'MERCHANT CATALOG',
        title: '添加商品',
        description: '创建类目、SPU、SKU 与库存的商家工作台将在此接入。',
      },
      meta: merchantMeta,
    },
    {
      path: 'merchant/products',
      name: 'merchant-product-browse',
      component: PlaceholderPage,
      props: {
        eyebrow: 'MERCHANT CATALOG',
        title: '商品浏览',
        description: '本店商品列表、上下架和库存查看将在此接入。',
      },
      meta: merchantMeta,
    },
  ],
  menuItems: [
    { to: '/products', label: '商品', portalModes: ['user'], roles: ['USER'] },
    { to: '/merchant/add-product', label: '添加商品', portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES, order: 10 },
    { to: '/merchant/products', label: '商品浏览', portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES, order: 20 },
  ],
}
