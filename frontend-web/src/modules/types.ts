import type { RouteRecordRaw } from 'vue-router'
import type { PortalMode } from '@/types/auth'

export interface WebMenuItem {
  to: string
  label: string
  portalModes?: PortalMode[]
  roles?: string[]
  order?: number
}

export interface WebModuleContribution {
  key: 'account' | 'merchant' | 'product' | 'trade' | 'message'
  owner: string
  routes: RouteRecordRaw[]
  menuItems: WebMenuItem[]
}

export function canAccessWebMenu(item: WebMenuItem, portalMode?: PortalMode, roles: string[] = []) {
  const matchesMode = !item.portalModes?.length || Boolean(portalMode && item.portalModes.includes(portalMode))
  const matchesRole = !item.roles?.length || item.roles.some((role) => roles.includes(role))
  return matchesMode && matchesRole
}

export function portalHomePath(portalMode?: PortalMode) {
  return portalMode === 'merchant' ? '/merchant/add-product' : '/'
}

export function portalHomeName(portalMode?: PortalMode) {
  return portalMode === 'merchant' ? 'merchant-add-product' : 'overview'
}
