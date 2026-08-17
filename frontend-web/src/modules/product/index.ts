import type { WebModuleContribution } from '../types'

export const productModule: WebModuleContribution = {
  key: 'product',
  owner: '成员 3',
  routes: [
    { path: 'products', name: 'product-list', component: () => import('./views/ProductListView.vue') },
    { path: 'products/:spuId', name: 'product-detail', component: () => import('./views/ProductDetailView.vue') },
    { path: 'merchant/products', name: 'product-manage', component: () => import('./views/ProductManageView.vue') },
  ],
  menuItems: [
    { to: '/products', label: '商品' },
    { to: '/merchant/products', label: '商品管理' },
  ],
}
