import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import type { AdminMode } from '../types/auth'

export interface AdminMenuItem {
  index: string
  label: string
  icon: Component
  permissions?: string[]
  roles?: string[]
  adminModes?: AdminMode[]
  disabled?: boolean
}

export interface AdminModuleContribution {
  key: 'system' | 'merchant' | 'product' | 'trade' | 'message'
  owner: string
  routes: RouteRecordRaw[]
  menuItems: AdminMenuItem[]
}

export function canAccessAdminMenu(
  item: AdminMenuItem,
  permissions: string[],
  roles: string[],
  adminMode?: AdminMode,
) {
  const hasPermission = !item.permissions?.length || item.permissions.some((permission) => permissions.includes(permission))
  const hasRole = !item.roles?.length || item.roles.some((role) => roles.includes(role))
  const matchesMode = !item.adminModes?.length || Boolean(adminMode && item.adminModes.includes(adminMode))
  return hasPermission && hasRole && matchesMode
}
