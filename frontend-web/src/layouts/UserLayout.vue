<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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
        <router-link to="/" @click="menuOpen = false">概览</router-link>
        <router-link to="/profile" @click="menuOpen = false">个人资料</router-link>
        <router-link to="/addresses" @click="menuOpen = false">收货地址</router-link>
        <router-link to="/preferences" @click="menuOpen = false">偏好设置</router-link>
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
