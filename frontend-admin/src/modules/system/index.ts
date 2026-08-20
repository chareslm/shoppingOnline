import { Document, Key, UserFilled } from '@element-plus/icons-vue'
import AuthorizationView from '../../views/AuthorizationView.vue'
import UserManagementView from '../../views/UserManagementView.vue'
import type { AdminModuleContribution } from '../types'

export const systemModule: AdminModuleContribution = {
  key: 'system',
  owner: '项目管理员',
  routes: [
    {
      path: 'authorization',
      name: 'authorization',
      component: AuthorizationView,
      meta: { permissions: ['system:role:view', 'system:permission:view'] },
    },
    {
      path: 'users',
      name: 'users',
      component: UserManagementView,
      meta: { permissions: ['system:user:view'] },
    },
    {
      path: 'audit-logs',
      name: 'audit-logs',
      component: () => import('../../views/AuditLogView.vue'),
      meta: { permissions: ['system:audit:view'] },
    },
  ],
  menuItems: [
    {
      index: '/authorization',
      label: '权限概览',
      icon: Key,
      permissions: ['system:role:view', 'system:permission:view'],
    },
    {
      index: '/users',
      label: '用户与角色',
      icon: UserFilled,
      permissions: ['system:user:view'],
    },
    {
      index: '/audit-logs',
      label: '审计日志',
      icon: Document,
      permissions: ['system:audit:view'],
    },
  ],
}
