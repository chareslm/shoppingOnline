<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Monitor, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { readApiError } from '../services/http'
import ChangePasswordDialog from '../components/ChangePasswordDialog.vue'
import { adminModuleMenuItems } from '../modules/registry'
import { canAccessAdminMenu } from '../modules/types'

const router = useRouter()
const auth = useAuthStore()
const passwordDialogVisible = ref(false)
const visibleModuleMenuItems = computed(() =>
  adminModuleMenuItems.filter((item) => canAccessAdminMenu(item, auth.session?.permissions ?? [])),
)

onMounted(async () => {
  try {
    await auth.restoreCurrentUser()
    passwordDialogVisible.value = Boolean(auth.session?.mustChangePassword)
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

function handleCommand(command: string) {
  if (command === 'change-password') {
    passwordDialogVisible.value = true
    return
  }
  void handleLogout()
}

async function handlePasswordChanged() {
  auth.clearLocalSession()
  ElMessage.success('密码已修改，请使用新密码重新登录')
  await router.replace({ name: 'login' })
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
        <el-menu-item
          v-for="item in visibleModuleMenuItems"
          :key="item.index"
          :index="item.index"
          :disabled="item.disabled"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <span>统一身份与权限基础已接入</span>
        <el-dropdown @command="handleCommand">
          <span class="user-menu"><el-icon><UserFilled /></el-icon>{{ auth.session?.username }}<el-icon><ArrowDown /></el-icon></span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="change-password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
    <ChangePasswordDialog v-model="passwordDialogVisible" :forced="auth.session?.mustChangePassword" @changed="handlePasswordChanged" />
  </el-container>
</template>
