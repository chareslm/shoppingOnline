import type { RouteRecordRaw } from 'vue-router'

export interface WebMenuItem {
  to: string
  label: string
}

export interface WebModuleContribution {
  key: 'account' | 'merchant' | 'product' | 'trade' | 'message'
  owner: string
  routes: RouteRecordRaw[]
  menuItems: WebMenuItem[]
}
