import { Goods } from '@element-plus/icons-vue'
import type { AdminModuleContribution } from '../types'

export const productModule: AdminModuleContribution = {
  key: 'product',
  owner: '成员 3',
  routes: [],
  menuItems: [
    { index: 'product-pending', label: '商品模块（待接入）', icon: Goods, disabled: true },
  ],
}
