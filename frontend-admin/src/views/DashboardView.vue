<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const roleText = computed(() => auth.session?.roles.join('、') || '未分配角色')
</script>

<template>
  <section>
    <el-card shadow="never" class="welcome-card">
      <p class="eyebrow">WORKSPACE</p>
      <h1>你好，{{ auth.session?.username }}</h1>
      <p>管理端认证、会话续期、路由守卫与权限菜单已就绪。后续业务模块可在此统一接入。</p>
    </el-card>
    <el-row :gutter="16" class="summary-grid">
      <el-col :xs="24" :sm="12" :lg="8"><el-card shadow="never"><span>当前角色</span><strong>{{ roleText }}</strong></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="8"><el-card shadow="never"><span>已加载权限</span><strong>{{ auth.session?.permissions.length || 0 }} 项</strong></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="8"><el-card shadow="never"><span>接口认证</span><strong>Bearer Token</strong></el-card></el-col>
    </el-row>
  </section>
</template>
