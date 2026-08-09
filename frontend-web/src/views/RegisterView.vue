<script setup lang="ts">
import axios from 'axios'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/services/auth'
import { readApiError } from '@/services/http'
import type { ApiResponse } from '@/types/api'

const router = useRouter()
const saving = ref(false)
const errorMessage = ref('')
const form = reactive({ username: '', email: '', phone: '', password: '', confirmPassword: '' })

function validate() {
  const username = form.username.trim()
  const email = form.email.trim()
  const phone = form.phone.trim()
  if (!username && !email && !phone) return '用户名、邮箱和手机号至少填写一项'
  if (username && !/^[A-Za-z][A-Za-z0-9_]{2,63}$/.test(username)) return '用户名须以字母开头，长度 3–64，仅含字母、数字或下划线'
  if (phone && !/^1\d{10}$/.test(phone)) return '请输入正确的 11 位中国大陆手机号'
  if (form.password.length < 8 || form.password.length > 64) return '密码长度须为 8–64 个字符'
  if (new TextEncoder().encode(form.password).length > 72) return '密码的 UTF-8 编码不能超过 72 字节'
  if (form.password !== form.confirmPassword) return '两次输入的密码不一致'
  return ''
}

async function submit() {
  errorMessage.value = validate()
  if (errorMessage.value) return

  saving.value = true
  try {
    const username = form.username.trim()
    const email = form.email.trim()
    const phone = form.phone.trim()
    await authApi.register({
      username: username || undefined,
      email: email || undefined,
      phone: phone || undefined,
      password: form.password,
    })
    await router.replace({ name: 'login', query: { registered: '1', identifier: username || email || phone } })
  } catch (error) {
    if (axios.isAxiosError<ApiResponse<unknown>>(error) && error.response?.data?.code === 40901) {
      errorMessage.value = '用户名、邮箱或手机号已被注册'
    } else {
      errorMessage.value = readApiError(error, '注册失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <main class="login-page register-page">
    <section class="login-story">
      <router-link class="brand brand-light" to="/login">
        <span class="brand-mark">S</span>
        <span><strong>SHOP</strong><small>用户中心</small></span>
      </router-link>
      <div>
        <p class="eyebrow">CREATE YOUR ACCOUNT</p>
        <h1>一个账号，<br />开启完整购物旅程。</h1>
        <p>注册后将自动获得普通用户角色，并创建个人资料与偏好记录。账号身份范围始终由服务端控制。</p>
      </div>
      <div class="trust-row"><span>统一账号</span><span>资料隔离</span><span>多端共用</span></div>
    </section>
    <section class="login-panel register-panel">
      <form class="login-card register-card" @submit.prevent="submit">
        <p class="eyebrow">GET STARTED</p>
        <h2>注册 SHOP</h2>
        <p class="muted">用户名、邮箱和手机号至少填写一项</p>
        <div class="register-grid">
          <label>用户名（可选）<input v-model="form.username" autocomplete="username" maxlength="64" placeholder="字母开头，3–64 位" /></label>
          <label>手机号（可选）<input v-model="form.phone" inputmode="numeric" autocomplete="tel" maxlength="11" placeholder="11 位手机号" /></label>
          <label class="full">邮箱（可选）<input v-model="form.email" type="email" autocomplete="email" maxlength="254" placeholder="name@example.com" /></label>
          <label>密码<input v-model="form.password" type="password" autocomplete="new-password" required minlength="8" maxlength="64" placeholder="至少 8 个字符" /></label>
          <label>确认密码<input v-model="form.confirmPassword" type="password" autocomplete="new-password" required minlength="8" maxlength="64" placeholder="再次输入密码" /></label>
        </div>
        <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
        <button class="primary-button wide" type="submit" :disabled="saving">{{ saving ? '正在注册…' : '创建账号' }}</button>
        <p class="login-switch">已有账号？<router-link to="/login">返回登录</router-link></p>
      </form>
    </section>
  </main>
</template>
