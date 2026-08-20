<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { authApi } from '../services/auth'
import { readApiError } from '../services/http'
import type { AdminUser, Role } from '../types/auth'

const PLATFORM_ROLE_CODES = new Set(['USER', 'ADMIN', 'SUPER_ADMIN'])
const loading = ref(false)
const assigning = ref(false)
const dialogVisible = ref(false)
const users = ref<AdminUser[]>([])
const roles = ref<Role[]>([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', page: 1, pageSize: 20 })
const assignment = reactive<{ user: AdminUser | null; roleIds: string[]; currentPassword: string }>({
  user: null,
  roleIds: [],
  currentPassword: '',
})
const platformRoles = computed(() => roles.value.filter((role) => PLATFORM_ROLE_CODES.has(role.code)))

async function loadUsers() {
  loading.value = true
  try {
    const result = await authApi.users({
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined,
      page: query.page,
      pageSize: query.pageSize,
    })
    users.value = result.items
    total.value = Number(result.total)
  } catch (error) {
    ElMessage.error(readApiError(error, '用户列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function initialize() {
  try {
    roles.value = await authApi.roles()
    await loadUsers()
  } catch (error) {
    ElMessage.error(readApiError(error, '用户管理初始化失败'))
  }
}

function search() {
  query.page = 1
  void loadUsers()
}

function openAssignment(user: AdminUser) {
  assignment.user = user
  assignment.roleIds = user.roles.filter((role) => PLATFORM_ROLE_CODES.has(role.code)).map((role) => role.id)
  assignment.currentPassword = ''
  dialogVisible.value = true
}

async function saveRoles() {
  if (!assignment.user || !assignment.roleIds.length) {
    ElMessage.warning('至少保留一个平台角色')
    return
  }
  if (!assignment.currentPassword) {
    ElMessage.warning('请输入当前管理员密码进行二次确认')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定更新账号 ${assignment.user.username || assignment.user.userId} 的平台角色吗？目标账号的登录会话将被撤销。`,
      '确认角色变更',
      { type: 'warning', confirmButtonText: '确认变更', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  assigning.value = true
  try {
    await authApi.replaceUserRoles(assignment.user.userId, assignment.roleIds, assignment.currentPassword)
    ElMessage.success('角色已更新，目标账号需重新登录')
    dialogVisible.value = false
    await loadUsers()
  } catch (error) {
    const message = readApiError(error, '角色更新失败')
    ElMessage.error(message === 'invalid credentials' ? '当前管理员密码错误' : message)
  } finally {
    assigning.value = false
    assignment.currentPassword = ''
  }
}

function roleTagType(code: string) {
  if (code === 'SUPER_ADMIN') return 'danger'
  if (code === 'ADMIN') return 'warning'
  if (code === 'USER') return 'success'
  return 'info'
}

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '从未登录'
}

onMounted(initialize)
</script>

<template>
  <section>
    <div class="page-heading">
      <div><p class="eyebrow">USER ACCESS</p><h1>用户与角色</h1><p>查询平台账号并管理平台角色；商家与客服角色由对应业务模块维护。</p></div>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent="search">
        <el-form-item label="账号">
          <el-input v-model="query.keyword" clearable placeholder="用户名 / 邮箱 / 手机号" :prefix-icon="Search" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 170px">
            <el-option label="正常" value="ACTIVE" /><el-option label="禁用" value="DISABLED" />
            <el-option label="锁定" value="LOCKED" /><el-option label="待验证" value="PENDING_VERIFICATION" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="query.keyword = ''; query.status = ''; search()">重置</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="users" row-key="userId" empty-text="暂无用户">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column label="账号" min-width="190">
          <template #default="{ row }"><strong>{{ row.username || '未设置用户名' }}</strong><div class="contact-line">{{ row.maskedEmail || row.maskedPhone || '无补充标识' }}</div></template>
        </el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column label="当前角色" min-width="260">
          <template #default="{ row }"><div class="role-tags"><el-tag v-for="role in row.roles" :key="role.id" :type="roleTagType(role.code)" effect="light">{{ role.code }}</el-tag></div></template>
        </el-table-column>
        <el-table-column label="创建时间" width="180"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="最后登录" width="180"><template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template></el-table-column>
        <el-table-column label="操作" width="110" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openAssignment(row)">分配角色</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" class="table-pagination" layout="total, prev, pager, next" :total="total" @current-change="loadUsers" />
    </el-card>

    <el-dialog v-model="dialogVisible" title="分配平台角色" width="520px" destroy-on-close>
      <el-alert title="高风险操作" description="仅修改 USER、ADMIN、SUPER_ADMIN；商家与客服角色会保留。保存后目标账号的 Refresh Token 将被撤销。" type="warning" :closable="false" show-icon />
      <el-form label-position="top" class="assignment-form">
        <el-form-item label="目标账号"><el-input :model-value="assignment.user?.username || String(assignment.user?.userId || '')" disabled /></el-form-item>
        <el-form-item label="平台角色">
          <el-checkbox-group v-model="assignment.roleIds"><el-checkbox v-for="role in platformRoles" :key="role.id" :value="role.id">{{ role.code }}（{{ role.name }}）</el-checkbox></el-checkbox-group>
        </el-form-item>
        <el-form-item label="当前管理员密码"><el-input v-model="assignment.currentPassword" type="password" show-password autocomplete="current-password" placeholder="用于二次确认" @keyup.enter="saveRoles" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="assigning" @click="saveRoles">保存角色</el-button></template>
    </el-dialog>
  </section>
</template>
