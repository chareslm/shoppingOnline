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
    { path: '', name: 'overview', component: OverviewView },
    { path: 'profile', name: 'profile', component: ProfileView },
    { path: 'addresses', name: 'addresses', component: AddressView },
    { path: 'preferences', name: 'preferences', component: PreferenceView },
    { path: 'devices', name: 'devices', component: DeviceSessionsView },
  ],
  menuItems: [
    { to: '/', label: '概览' },
    { to: '/profile', label: '个人资料' },
    { to: '/addresses', label: '收货地址' },
    { to: '/preferences', label: '偏好设置' },
    { to: '/devices', label: '登录设备' },
  ],
}
