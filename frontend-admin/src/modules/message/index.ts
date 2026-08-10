import { ChatLineRound } from '@element-plus/icons-vue'
import type { AdminModuleContribution } from '../types'

export const messageModule: AdminModuleContribution = {
  key: 'message',
  owner: '成员 5',
  routes: [],
  menuItems: [
    { index: 'message-pending', label: '消息模块（待接入）', icon: ChatLineRound, disabled: true },
  ],
}
