<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { webModuleMenuItems } from '@/modules/registry'

const auth = useAuthStore()
const router = useRouter()
const menuOpen = ref(false)
const displayName = computed(() => auth.session?.username || '用户')
const initial = computed(() => displayName.value.slice(0, 1).toUpperCase())

async function logout() {
  await auth.logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <router-link class="brand" to="/" @click="menuOpen = false">
        <span class="brand-mark">S</span>
        <span><strong>SHOP</strong><small>用户中心</small></span>
      </router-link>
      <nav :class="['main-nav', { open: menuOpen }]">
        <router-link v-for="item in webModuleMenuItems" :key="item.to" :to="item.to" @click="menuOpen = false">
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
    <footer>SHOP · 统一账号与用户中心基础能力</footer>
  </div>
</template>
