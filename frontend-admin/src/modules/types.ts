import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

export interface AdminMenuItem {
  index: string
  label: string
  icon: Component
  permissions?: string[]
  disabled?: boolean
}

export interface AdminModuleContribution {
  key: 'system' | 'merchant' | 'product' | 'trade' | 'message'
  owner: string
  routes: RouteRecordRaw[]
  menuItems: AdminMenuItem[]
}

export function canAccessAdminMenu(item: AdminMenuItem, permissions: string[]) {
  return !item.permissions?.length || item.permissions.some((permission) => permissions.includes(permission))
}
