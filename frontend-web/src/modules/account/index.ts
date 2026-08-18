import AddressView from '@/views/AddressView.vue'
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
  ],
  menuItems: [
    { to: '/', label: '概览', portalModes: ['user'], roles: ['USER'] },
    { to: '/profile', label: '个人资料', portalModes: ['user'], roles: ['USER'] },
    { to: '/addresses', label: '收货地址', portalModes: ['user'], roles: ['USER'] },
    { to: '/preferences', label: '偏好设置', portalModes: ['user'], roles: ['USER'] },
  ],
}
