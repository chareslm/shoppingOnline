<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, Setting, User, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { readApiError } from '../services/http'
import type { AdminMode } from '../types/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const form = reactive<{ identifier: string; password: string; adminMode: AdminMode }>({
  identifier: '',
  password: '',
  adminMode: 'platform',
})
const rules: FormRules = {
  adminMode: [{ required: true, message: '请选择管理员身份', trigger: 'change' }],
  identifier: [{ required: true, message: '请输入用户名、邮箱或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await auth.login(form.identifier, form.password, form.adminMode)
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
      <h1>运营管理台</h1>
      <p>系统管理员与平台管理员在此分开登录。商家请使用用户 Web 选择商家身份进入工作台。</p>
    </section>
    <el-card class="login-card" shadow="never">
      <h2>账号登录</h2>
      <p class="hint">请选择与账号角色匹配的管理入口。商家账号不能登录本端。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="管理员身份" prop="adminMode">
          <el-radio-group v-model="form.adminMode" class="admin-mode-picker">
            <el-radio-button value="system">
              <el-icon><Setting /></el-icon>
              <span>系统管理员</span>
              <small>需要 SUPER_ADMIN</small>
            </el-radio-button>
            <el-radio-button value="platform">
              <el-icon><UserFilled /></el-icon>
              <span>平台管理员</span>
              <small>需要 ADMIN</small>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
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
