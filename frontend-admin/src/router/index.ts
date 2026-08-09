import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import AuthorizationView from '../views/AuthorizationView.vue'
import LoginView from '../views/LoginView.vue'
import { getSession } from '../utils/session'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    permissions?: string[]
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
      { path: 'dashboard', name: 'dashboard', component: DashboardView },
      {
        path: 'authorization',
        name: 'authorization',
        component: AuthorizationView,
        meta: { permissions: ['system:role:view', 'system:permission:view'] },
      },
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
  const isLoggedIn = Boolean(session?.accessToken && session.refreshToken)

  if (to.name === 'login') {
    return isLoggedIn ? { name: 'dashboard' } : true
  }

  if (to.meta.requiresAuth && !isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.permissions && !to.meta.permissions.some((permission) => session?.permissions.includes(permission))) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
