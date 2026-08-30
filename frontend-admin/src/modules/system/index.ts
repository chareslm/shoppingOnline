import { DataAnalysis, Document, Message, UserFilled } from '@element-plus/icons-vue'
import SmtpSettingsView from '../../views/SmtpSettingsView.vue'
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
    {
      path: 'smtp',
      name: 'smtp-settings',
      component: SmtpSettingsView,
      meta: {
        permissions: ['system:smtp:view'],
        roles: ['SUPER_ADMIN'],
        adminModes: ['system'],
      },
    },
    {
      path: 'statistics/platform',
      name: 'statistics-platform',
      component: () => import('./views/PlatformStatisticsView.vue'),
      meta: {
        permissions: ['statistics:platform:view'],
        roles: ['SUPER_ADMIN'],
        adminModes: ['system'],
      },
    },
  ],
  menuItems: [
    {
      index: '/statistics/platform',
      label: '平台统计',
      icon: DataAnalysis,
      permissions: ['statistics:platform:view'],
      roles: ['SUPER_ADMIN'],
      adminModes: ['system'],
    },
    {
      index: '/users',
      label: '用户与角色',
      icon: UserFilled,
      permissions: ['system:user:view'],
      roles: ['SUPER_ADMIN'],
      adminModes: ['system'],
    },
    {
      index: '/smtp',
      label: 'SMTP 配置',
      icon: Message,
      permissions: ['system:smtp:view'],
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
    {
      index: '/audit-logs',
      label: '审计日志',
      icon: Document,
      permissions: ['system:audit:view'],
    },
  ],
}
