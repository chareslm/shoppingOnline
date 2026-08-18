<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const roleText = computed(() => auth.session?.roles.join('、') || '未分配角色')
const modeName = computed(() => (auth.session?.adminMode === 'system' ? '系统管理员' : '平台管理员'))
const modeHint = computed(() =>
  auth.session?.adminMode === 'system'
    ? '当前可管理用户与角色、查看系统日志。商家请改用用户 Web 的商家身份登录。'
    : '当前可处理商家审核、商品审核、全部商品和消息发布。',
)
</script>

<template>
  <section>
    <el-card shadow="never" class="welcome-card">
      <p class="eyebrow">WORKSPACE</p>
      <h1>你好，{{ auth.session?.username }}</h1>
      <p>你已进入{{ modeName }}工作区。{{ modeHint }}</p>
    </el-card>
    <el-row :gutter="16" class="summary-grid">
      <el-col :xs="24" :sm="12" :lg="8"><el-card shadow="never"><span>当前身份</span><strong>{{ modeName }}</strong></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="8"><el-card shadow="never"><span>当前角色</span><strong>{{ roleText }}</strong></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="8"><el-card shadow="never"><span>已加载权限</span><strong>{{ auth.session?.permissions.length || 0 }} 项</strong></el-card></el-col>
    </el-row>
  </section>
</template>
