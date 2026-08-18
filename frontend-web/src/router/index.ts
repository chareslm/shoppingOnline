import { createRouter, createWebHistory } from 'vue-router'
import UserLayout from '@/layouts/UserLayout.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ForcedPasswordChangeView from '@/views/ForcedPasswordChangeView.vue'
import { webModuleRoutes } from '@/modules/registry'
import { useAuthStore } from '@/stores/auth'

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
  if (to.meta.public) return auth.isAuthenticated ? { name: 'overview' } : true
  if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  try {
    await auth.restoreCurrentUser()
    if (auth.session?.mustChangePassword && to.name !== 'forced-password-change') return { name: 'forced-password-change' }
    if (!auth.session?.mustChangePassword && to.name === 'forced-password-change') return { name: 'overview' }
    return true
  } catch {
    await auth.logout()
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})
