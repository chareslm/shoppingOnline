<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { readApiError } from '../services/http'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const form = reactive({ identifier: '', password: '' })
const rules: FormRules = {
  identifier: [{ required: true, message: '请输入用户名、邮箱或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await auth.login(form.identifier, form.password)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(readApiError(error, '登录失败，请检查账号和密码'))
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <p class="eyebrow">SHOPPING ONLINE</p>
      <h1>统一运营管理台</h1>
      <p>面向平台管理员、商家和客服人员的统一入口。</p>
    </section>
    <el-card class="login-card" shadow="never">
      <h2>账号登录</h2>
      <p class="hint">请使用已分配管理角色的账号登录。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="账号" prop="identifier">
          <el-input v-model="form.identifier" :prefix-icon="User" autocomplete="username" placeholder="用户名、邮箱或手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" :prefix-icon="Lock" autocomplete="current-password" show-password type="password" @keyup.enter="submit" />
        </el-form-item>
        <el-button :loading="auth.loading" native-type="submit" type="primary" class="login-button">登录</el-button>
      </el-form>
    </el-card>
  </main>
</template>
