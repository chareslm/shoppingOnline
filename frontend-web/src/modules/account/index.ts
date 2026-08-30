import AddressView from '@/views/AddressView.vue'
import DeviceSessionsView from '@/views/DeviceSessionsView.vue'
import OverviewView from '@/views/OverviewView.vue'
import PreferenceView from '@/views/PreferenceView.vue'
import ProfileView from '@/views/ProfileView.vue'
import type { WebModuleContribution } from '../types'

export const accountModule: WebModuleContribution = {
  key: 'account',
  owner: '项目管理员',
  routes: [
    { path: '', name: 'overview', component: OverviewView, meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'profile', name: 'profile', component: ProfileView, meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'addresses', name: 'addresses', component: AddressView, meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'preferences', name: 'preferences', component: PreferenceView, meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'devices', name: 'devices', component: DeviceSessionsView, meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'statistics', name: 'self-statistics', component: () => import('./views/UserStatisticsView.vue'), meta: { portalModes: ['user'], roles: ['USER'] } },
    { path: 'merchant/statistics', name: 'merchant-statistics', component: () => import('./views/MerchantStatisticsView.vue'), meta: { portalModes: ['merchant'], roles: ['MERCHANT_OWNER'] } },
  ],
  menuItems: [
    { to: '/', label: '概览', portalModes: ['user'], roles: ['USER'] },
    { to: '/profile', label: '个人资料', portalModes: ['user'], roles: ['USER'] },
    { to: '/addresses', label: '收货地址', portalModes: ['user'], roles: ['USER'] },
    { to: '/preferences', label: '偏好设置', portalModes: ['user'], roles: ['USER'] },
    { to: '/devices', label: '登录设备', portalModes: ['user'], roles: ['USER'] },
    { to: '/statistics', label: '消费统计', portalModes: ['user'], roles: ['USER'], order: 45 },
    { to: '/merchant/statistics', label: '经营统计', portalModes: ['merchant'], roles: ['MERCHANT_OWNER'], order: 35 },
  ],
}
