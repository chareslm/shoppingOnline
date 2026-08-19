import { createRouter, createWebHistory } from 'vue-router'
import UserLayout from '@/layouts/UserLayout.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ForcedPasswordChangeView from '@/views/ForcedPasswordChangeView.vue'
import { portalHomeName } from '@/modules/types'
import { webModuleRoutes } from '@/modules/registry'
import { useAuthStore } from '@/stores/auth'
import type { PortalMode } from '@/types/auth'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    portalModes?: readonly PortalMode[]
    roles?: readonly string[]
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/register', name: 'register', component: RegisterView, meta: { public: true } },
    { path: '/change-password', name: 'forced-password-change', component: ForcedPasswordChangeView },
    {
      path: '/',
      component: UserLayout,
      children: webModuleRoutes,
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const homeName = portalHomeName(auth.session?.portalMode, auth.session?.roles ?? [])

  if (to.meta.public) return auth.isAuthenticated ? { name: homeName } : true
  if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  try {
    await auth.restoreCurrentUser()
    if (auth.session?.mustChangePassword && to.name !== 'forced-password-change') return { name: 'forced-password-change' }
    if (!auth.session?.mustChangePassword && to.name === 'forced-password-change') return { name: homeName }
    if (to.meta.portalModes && !to.meta.portalModes.includes(auth.session?.portalMode ?? 'user')) return { name: homeName }
    if (to.meta.roles && !to.meta.roles.some((role) => auth.session?.roles.includes(role))) return { name: homeName }
    return true
  } catch {
    await auth.logout()
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})
