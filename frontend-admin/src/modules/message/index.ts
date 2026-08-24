import { Promotion } from '@element-plus/icons-vue'
import PlaceholderView from '../../views/PlaceholderView.vue'
import type { AdminModuleContribution } from '../types'

export const messageModule: AdminModuleContribution = {
  key: 'message',
  owner: '成员 5',
  routes: [
    {
      path: 'message/publish',
      name: 'message-publish',
      component: PlaceholderView,
      props: {
        eyebrow: 'PLATFORM MESSAGE',
        title: '消息发布',
        description: '平台公告、运营通知与消息投放将在此接入。',
      },
      meta: { roles: ['ADMIN', 'SUPER_ADMIN'], adminModes: ['platform'] },
    },
  ],
  menuItems: [
    {
      index: '/message/publish',
      label: '消息发布',
      icon: Promotion,
      roles: ['ADMIN', 'SUPER_ADMIN'],
      adminModes: ['platform'],
    },
  ],
}
