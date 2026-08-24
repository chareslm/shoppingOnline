import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import LoginView from '../views/LoginView.vue'
import { adminModuleRoutes } from '../modules/registry'
import { getSession } from '../utils/session'
import type { AdminMode } from '../types/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    permissions?: readonly string[]
    roles?: readonly string[]
    adminModes?: readonly AdminMode[]
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: { roles: ['SUPER_ADMIN', 'ADMIN'], adminModes: ['system', 'platform'] },
      },
      ...adminModuleRoutes,
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const session = getSession()
  const modeRoleMatches = session?.adminMode === 'system'
    ? session.roles.includes('SUPER_ADMIN')
    : session?.adminMode === 'platform' && (session.roles.includes('ADMIN') || session.roles.includes('SUPER_ADMIN'))
  const isLoggedIn = Boolean(session?.accessToken && session.refreshToken && modeRoleMatches)

  if (to.name === 'login') {
    return isLoggedIn ? { name: 'dashboard' } : true
  }

  if (to.meta.requiresAuth && !isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.permissions && !to.meta.permissions.some((permission) => session?.permissions.includes(permission))) {
    return { name: 'dashboard' }
  }

  if (to.meta.roles && !to.meta.roles.some((role) => session?.roles.includes(role))) {
    return { name: 'dashboard' }
  }

  if (to.meta.adminModes && !to.meta.adminModes.some((mode) => mode === session?.adminMode)) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
