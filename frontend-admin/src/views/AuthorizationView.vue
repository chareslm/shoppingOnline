<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '../services/auth'
import { readApiError } from '../services/http'
import { useAuthStore } from '../stores/auth'
import type { Permission, Role } from '../types/auth'

const auth = useAuthStore()
const loading = ref(false)
const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])

async function loadAuthorization() {
  loading.value = true
  try {
    const requests: Promise<void>[] = []
    if (auth.session?.permissions.includes('system:role:view')) {
      requests.push(authApi.roles().then((roleList) => { roles.value = roleList }))
    }
    if (auth.session?.permissions.includes('system:permission:view')) {
      requests.push(authApi.permissions().then((permissionList) => { permissions.value = permissionList }))
    }
    await Promise.all(requests)
  } catch (error) {
    ElMessage.error(readApiError(error, '加载权限信息失败'))
  } finally {
    loading.value = false
  }
}

onMounted(loadAuthorization)
</script>

<template>
  <section>
    <div class="page-heading"><div><p class="eyebrow">AUTHORIZATION</p><h1>角色与权限</h1><p>该页仅展示后端授权数据；实际安全边界始终由服务端校验。</p></div><el-button :loading="loading" @click="loadAuthorization">刷新</el-button></div>
    <el-row :gutter="16">
      <el-col :xs="24" :lg="12"><el-card v-loading="loading" shadow="never" header="有效角色"><el-table :data="roles" empty-text="暂无可查看角色"><el-table-column prop="code" label="角色编码" /><el-table-column prop="name" label="名称" /><el-table-column prop="dataScope" label="数据范围" /></el-table></el-card></el-col>
      <el-col :xs="24" :lg="12"><el-card v-loading="loading" shadow="never" header="有效权限"><el-table :data="permissions" empty-text="暂无可查看权限"><el-table-column prop="code" label="权限编码" /><el-table-column prop="name" label="名称" /></el-table></el-card></el-col>
    </el-row>
  </section>
</template>
