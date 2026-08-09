<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { userApi } from '@/services/user'
import { readApiError } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { UserAddress, UserPreference, UserProfile } from '@/types/user'

const auth = useAuthStore()
const profile = ref<UserProfile | null>(null)
const addresses = ref<UserAddress[]>([])
const preference = ref<UserPreference | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const greetingName = computed(() => profile.value?.nickname || auth.session?.username || '你好')

onMounted(async () => {
  try {
    ;[profile.value, addresses.value, preference.value] = await Promise.all([
      userApi.profile(),
      userApi.addresses(),
      userApi.preference(),
    ])
  } catch (error) {
    errorMessage.value = readApiError(error, '用户信息加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="page-stack">
    <div class="hero-card">
      <div>
        <p class="eyebrow">ACCOUNT HOME</p>
        <h1>你好，{{ greetingName }}</h1>
        <p>你的账号基础资料已经接入。现在可以完善个人信息、地址和通知偏好。</p>
      </div>
      <span class="hero-orb">S</span>
    </div>
    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
    <div v-if="loading" class="loading-card">正在整理你的账号信息…</div>
    <template v-else>
      <div class="stat-grid">
        <router-link class="stat-card" to="/profile"><span>资料完整度</span><strong>{{ profile?.nickname ? '已完善' : '待完善' }}</strong><small>编辑个人资料 →</small></router-link>
        <router-link class="stat-card" to="/addresses"><span>收货地址</span><strong>{{ addresses.length }} 个</strong><small>管理地址 →</small></router-link>
        <router-link class="stat-card" to="/preferences"><span>系统通知</span><strong>{{ preference?.systemNotificationEnabled ? '已开启' : '已关闭' }}</strong><small>修改偏好 →</small></router-link>
      </div>
      <div class="section-card next-card">
        <div><p class="eyebrow">COMING NEXT</p><h2>业务模块将在这里接入</h2><p class="muted">商品浏览、购物车和订单正在由对应模块推进；当前先保证账号和用户数据边界稳定。</p></div>
        <div class="tag-list"><span>商品</span><span>购物车</span><span>订单</span><span>消息</span></div>
      </div>
    </template>
  </section>
</template>
