import { createRouter, createWebHistory } from 'vue-router'
import UserLayout from '@/layouts/UserLayout.vue'
import AddressView from '@/views/AddressView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import OverviewView from '@/views/OverviewView.vue'
import PreferenceView from '@/views/PreferenceView.vue'
import ProfileView from '@/views/ProfileView.vue'
import { useAuthStore } from '@/stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/register', name: 'register', component: RegisterView, meta: { public: true } },
    {
      path: '/',
      component: UserLayout,
      children: [
        { path: '', name: 'overview', component: OverviewView },
        { path: 'profile', name: 'profile', component: ProfileView },
        { path: 'addresses', name: 'addresses', component: AddressView },
        { path: 'preferences', name: 'preferences', component: PreferenceView },
      ],
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
    return true
  } catch {
    await auth.logout()
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})
