<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { readApiError } from '@/services/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const identifier = ref('')
const password = ref('')
const errorMessage = ref('')
const successMessage = ref('')

onMounted(() => {
  if (typeof route.query.identifier === 'string') identifier.value = route.query.identifier
  if (route.query.registered === '1') successMessage.value = '注册成功，请使用新账号登录'
  if (route.query.passwordChanged === '1') successMessage.value = '密码已修改，请使用新密码登录'
})

async function submit() {
  errorMessage.value = ''
  try {
    await auth.login(identifier.value.trim(), password.value)
    if (auth.session?.mustChangePassword) {
      await router.replace({ name: 'forced-password-change' })
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = readApiError(error, '登录失败，请检查账号和密码')
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <router-link class="brand brand-light" to="/">
        <span class="brand-mark">S</span>
        <span><strong>SHOP</strong><small>用户中心</small></span>
      </router-link>
      <div>
        <p class="eyebrow">ONE ACCOUNT, EVERYWHERE</p>
        <h1>把每一次购物<br />都留在同一个账号里。</h1>
        <p>统一管理个人资料、收货地址和消息偏好。商品、购物车与订单模块就绪后，将从这里自然接入。</p>
      </div>
      <div class="trust-row"><span>安全会话</span><span>设备管理</span><span>Token 自动续期</span></div>
    </section>
    <section class="login-panel">
      <form class="login-card" @submit.prevent="submit">
        <p class="eyebrow">WELCOME BACK</p>
        <h2>登录 SHOP</h2>
        <p class="muted">使用用户名、邮箱或手机号登录</p>
        <label>账号<input v-model="identifier" autocomplete="username" required placeholder="用户名 / 邮箱 / 手机号" /></label>
        <label>密码<input v-model="password" type="password" autocomplete="current-password" required placeholder="请输入密码" /></label>
        <p v-if="successMessage" class="notice success">{{ successMessage }}</p>
        <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
        <button class="primary-button wide" type="submit" :disabled="auth.loading">
          {{ auth.loading ? '正在登录…' : '登录' }}
        </button>
        <p class="login-switch">还没有账号？<router-link to="/register">立即注册</router-link></p>
        <p class="login-footnote">登录即表示该浏览器将作为一个独立设备保存会话。</p>
      </form>
    </section>
  </main>
</template>
