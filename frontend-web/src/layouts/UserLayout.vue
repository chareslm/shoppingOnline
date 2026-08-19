<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { canAccessWebMenu, portalHomePath } from '@/modules/types'
import { webModuleMenuItems } from '@/modules/registry'
import { useAuthStore } from '@/stores/auth'
import { isCustomerServiceOnly } from '@/types/auth'

const auth = useAuthStore()
const router = useRouter()
const menuOpen = ref(false)
const displayName = computed(() => auth.session?.username || '用户')
const initial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const isMerchantPortal = computed(() => auth.session?.portalMode === 'merchant')
const homePath = computed(() => portalHomePath(auth.session?.portalMode, auth.session?.roles ?? []))
const isCustomerService = computed(() => isCustomerServiceOnly(auth.session?.roles ?? []))
const visibleMenuItems = computed(() =>
  webModuleMenuItems
    .filter((item) => canAccessWebMenu(item, auth.session?.portalMode, auth.session?.roles ?? []))
    .sort((left, right) => (left.order ?? 100) - (right.order ?? 100)),
)

async function logout() {
  await auth.logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <router-link class="brand" :to="homePath" @click="menuOpen = false">
        <span class="brand-mark">S</span>
        <span><strong>SHOP</strong><small>{{ isCustomerService ? '客服工作台' : isMerchantPortal ? '商家工作台' : '用户中心' }}</small></span>
      </router-link>
      <nav :class="['main-nav', { open: menuOpen }]">
        <router-link v-for="item in visibleMenuItems" :key="item.to" :to="item.to" @click="menuOpen = false">
          {{ item.label }}
        </router-link>
      </nav>
      <div class="user-actions">
        <button class="menu-toggle" type="button" aria-label="展开导航" @click="menuOpen = !menuOpen">☰</button>
        <span class="avatar">{{ initial }}</span>
        <span class="user-name">{{ displayName }}</span>
        <button class="text-button" type="button" @click="logout">退出</button>
      </div>
    </header>
    <main class="page-container"><router-view /></main>
    <footer>{{ isCustomerService ? 'SHOP · 客服工作台' : isMerchantPortal ? 'SHOP · 商家工作台' : 'SHOP · 统一账号与用户中心基础能力' }}</footer>
  </div>
</template>
