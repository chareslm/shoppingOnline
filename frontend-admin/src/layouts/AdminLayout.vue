<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, DataAnalysis, Key, Monitor, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { readApiError } from '../services/http'

const router = useRouter()
const auth = useAuthStore()
const canViewAuthorization = computed(() =>
  auth.session?.permissions.some((permission) => ['system:role:view', 'system:permission:view'].includes(permission)),
)
const canViewUsers = computed(() => auth.session?.permissions.includes('system:user:view'))

onMounted(async () => {
  try {
    await auth.restoreCurrentUser()
  } catch (error) {
    ElMessage.error(readApiError(error, '登录状态已失效，请重新登录'))
    await auth.logout()
    await router.replace({ name: 'login' })
  }
})

async function handleLogout() {
  try {
    await auth.logout()
    ElMessage.success('已退出登录')
  } catch (error) {
    ElMessage.warning(readApiError(error, '会话已在本地清除'))
  } finally {
    await router.replace({ name: 'login' })
  }
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand"><span>SHOP</span> 运营管理台</div>
      <el-menu router :default-active="$route.path" background-color="transparent" text-color="#b8c4d9" active-text-color="#ffffff">
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item v-if="canViewAuthorization" index="/authorization">
          <el-icon><Key /></el-icon>
          <span>权限概览</span>
        </el-menu-item>
        <el-menu-item v-if="canViewUsers" index="/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户与角色</span>
        </el-menu-item>
        <el-menu-item index="pending-modules" disabled>
          <el-icon><DataAnalysis /></el-icon>
          <span>业务模块（待接入）</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <span>统一身份与权限基础已接入</span>
        <el-dropdown @command="handleLogout">
          <span class="user-menu"><el-icon><UserFilled /></el-icon>{{ auth.session?.username }}<el-icon><ArrowDown /></el-icon></span>
          <template #dropdown><el-dropdown-menu><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>
