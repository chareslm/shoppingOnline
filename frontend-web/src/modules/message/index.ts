import { MERCHANT_PORTAL_ROLES } from '@/types/auth'
import type { WebModuleContribution } from '../types'

const USER_ROLES: string[] = ['USER']

export const messageModule: WebModuleContribution = {
  key: 'message',
  owner: '成员 5',
  routes: [
    // 买家端路由
    {
      path: 'chat',
      name: 'chat',
      component: () => import('./views/ChatView.vue'),
      meta: { portalModes: ['user'], roles: USER_ROLES },
    },
    {
      path: 'notifications',
      name: 'notifications',
      component: () => import('./views/NotificationCenter.vue'),
      meta: { portalModes: ['user'], roles: USER_ROLES },
    },
    {
      path: 'notifications/preference',
      name: 'notification-preferences',
      component: () => import('./views/NotificationPrefs.vue'),
      meta: { portalModes: ['user'], roles: USER_ROLES },
    },
    // 商家端路由
    {
      path: 'merchant/inbox',
      name: 'merchant-inbox',
      component: () => import('./views/MerchantInboxView.vue'),
      meta: { portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES },
    },
  ],
  menuItems: [
    // 买家端菜单
    { to: '/chat', label: '在线客服', portalModes: ['user'], roles: USER_ROLES, order: 50 },
    { to: '/notifications', label: '消息通知', portalModes: ['user'], roles: USER_ROLES, order: 51 },
    { to: '/notifications/preference', label: '通知设置', portalModes: ['user'], roles: USER_ROLES, order: 52 },
    // 商家端菜单
    { to: '/merchant/inbox', label: '用户沟通', portalModes: ['merchant'], roles: MERCHANT_PORTAL_ROLES, order: 40 },
  ],
}