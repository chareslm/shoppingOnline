import { MERCHANT_PORTAL_ROLES } from '@/types/auth'
import type { WebModuleContribution } from '../types'

export const messageModule: WebModuleContribution = {
  key: 'message',
  owner: '成员 5',
  routes: [
    {
      path: 'chat',
      name: 'message-chat',
      component: () => import('./views/ChatView.vue'),
      meta: { portalModes: ['user'], roles: ['USER'] },
    },
    {
      path: 'notifications',
      name: 'message-notifications',
      component: () => import('./views/NotificationCenter.vue'),
      meta: { portalModes: ['user'], roles: ['USER'] },
    },
    {
      path: 'notifications/preference',
      name: 'message-notification-prefs',
      component: () => import('./views/NotificationPrefs.vue'),
      meta: { portalModes: ['user'], roles: ['USER'] },
    },
    {
      path: 'merchant/inbox',
      name: 'merchant-inbox',
      component: () => import('./views/MerchantInboxView.vue'),
      meta: { portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES },
    },
  ],
  menuItems: [
    { to: '/chat', label: '客服聊天', portalModes: ['user'], roles: ['USER'] },
    { to: '/notifications', label: '消息通知', portalModes: ['user'], roles: ['USER'] },
    { to: '/merchant/inbox', label: '用户沟通', portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES, order: 40 },
  ],
}
