import { MERCHANT_OPERATOR_ROLES } from '@/types/auth'
import type { WebModuleContribution } from '../types'

const merchantOperatorMeta = { portalModes: ['merchant'] as const, roles: MERCHANT_OPERATOR_ROLES }

export const productModule: WebModuleContribution = {
  key: 'product',
  owner: '成员 3',
  routes: [
    { path: 'products', name: 'product-list', component: () => import('./views/ProductListView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'products/:spuId', name: 'product-detail', component: () => import('./views/ProductDetailView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    {
      path: 'merchant/add-product',
      name: 'merchant-add-product',
      component: () => import('./views/MerchantProductCreateView.vue'),
      meta: merchantOperatorMeta,
    },
    {
      path: 'merchant/products',
      name: 'merchant-product-browse',
      component: () => import('./views/MerchantProductListView.vue'),
      meta: merchantOperatorMeta,
    },
  ],
  menuItems: [
    { to: '/products', label: '商品', portalModes: ['user'], roles: ['USER'] },
    { to: '/merchant/add-product', label: '添加商品', portalModes: ['merchant'], roles: MERCHANT_OPERATOR_ROLES, order: 10 },
    { to: '/merchant/products', label: '商品浏览', portalModes: ['merchant'], roles: MERCHANT_OPERATOR_ROLES, order: 20 },
  ],
}
