import { Shop } from '@element-plus/icons-vue'
import type { AdminModuleContribution } from '../types'

export const merchantModule: AdminModuleContribution = {
  key: 'merchant',
  owner: '成员 2',
  routes: [],
  menuItems: [
    { index: 'merchant-pending', label: '商家模块（待接入）', icon: Shop, disabled: true },
  ],
}
