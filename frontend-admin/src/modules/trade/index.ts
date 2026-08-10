import { ShoppingCart } from '@element-plus/icons-vue'
import type { AdminModuleContribution } from '../types'

export const tradeModule: AdminModuleContribution = {
  key: 'trade',
  owner: '成员 4',
  routes: [],
  menuItems: [
    { index: 'trade-pending', label: '交易模块（待接入）', icon: ShoppingCart, disabled: true },
  ],
}
