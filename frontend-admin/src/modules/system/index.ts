import { Document, UserFilled } from '@element-plus/icons-vue'
import SystemLogView from '../../views/SystemLogView.vue'
import UserManagementView from '../../views/UserManagementView.vue'
import type { AdminModuleContribution } from '../types'

export const systemModule: AdminModuleContribution = {
  key: 'system',
  owner: '项目管理员',
  routes: [
    {
      path: 'system-logs',
      name: 'system-logs',
      component: SystemLogView,
      meta: { roles: ['SUPER_ADMIN'], adminModes: ['system'] },
    },
    {
      path: 'users',
      name: 'users',
      component: UserManagementView,
      meta: {
        permissions: ['system:user:view'],
        roles: ['SUPER_ADMIN'],
        adminModes: ['system'],
      },
    },
  ],
  menuItems: [
    {
      index: '/users',
      label: '用户与角色',
      icon: UserFilled,
      permissions: ['system:user:view'],
      roles: ['SUPER_ADMIN'],
      adminModes: ['system'],
    },
    {
      index: '/system-logs',
      label: '系统日志',
      icon: Document,
      roles: ['SUPER_ADMIN'],
      adminModes: ['system'],
    },
  ],
}
