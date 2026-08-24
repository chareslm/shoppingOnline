<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/services/auth'
import { readApiError } from '@/services/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const saving = ref(false)
const errorMessage = ref('')
const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

function validate() {
  if (!form.currentPassword) return '请输入邮件中的临时密码'
  if (form.newPassword.length < 12 || form.newPassword.length > 64) return '新密码长度须为 12–64 个字符'
  if (new TextEncoder().encode(form.newPassword).length > 72) return '新密码的 UTF-8 编码不能超过 72 字节'
  if (!/[a-z]/.test(form.newPassword) || !/[A-Z]/.test(form.newPassword) || !/\d/.test(form.newPassword) || !/[^A-Za-z0-9\s]/.test(form.newPassword)) {
    return '新密码须同时包含大写字母、小写字母、数字和特殊字符'
  }
  if (form.newPassword === form.currentPassword) return '新密码不能与临时密码相同'
  if (form.newPassword !== form.confirmPassword) return '两次输入的新密码不一致'
  return ''
}

async function submit() {
  errorMessage.value = validate()
  if (errorMessage.value) return
  saving.value = true
  try {
    const portal = auth.session?.portalMode
    await authApi.changePassword(form.currentPassword, form.newPassword)
    await auth.logout()
    await router.replace({ name: 'login', query: { passwordChanged: '1', ...(portal === 'merchant' ? { portal: 'merchant' } : {}) } })
  } catch (error) {
    errorMessage.value = readApiError(error, '密码修改失败，请检查临时密码')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <router-link class="brand brand-light" to="/login">
        <span class="brand-mark">S</span>
        <span><strong>SHOP</strong><small>商家账号保护</small></span>
      </router-link>
      <div>
        <p class="eyebrow">FIRST SIGN-IN</p>
        <h1>临时密码，<br />只使用这一次。</h1>
        <p>平台审核通过后发送的密码仅用于首次登录。修改成功会撤销当前会话，请使用新密码重新登录。</p>
      </div>
      <div class="trust-row"><span>强密码</span><span>会话撤销</span><span>审计留痕</span></div>
    </section>
    <section class="login-panel">
      <form class="login-card" @submit.prevent="submit">
        <p class="eyebrow">SECURE YOUR ACCOUNT</p>
        <h2>设置正式密码</h2>
        <p class="muted">至少 12 位，并包含大小写字母、数字和特殊字符</p>
        <label>当前临时密码<input v-model="form.currentPassword" type="password" autocomplete="current-password" required /></label>
        <label>新密码<input v-model="form.newPassword" type="password" autocomplete="new-password" required /></label>
        <label>确认新密码<input v-model="form.confirmPassword" type="password" autocomplete="new-password" required /></label>
        <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
        <button class="primary-button wide" type="submit" :disabled="saving">{{ saving ? '正在修改…' : '保存新密码' }}</button>
      </form>
    </section>
  </main>
</template>
