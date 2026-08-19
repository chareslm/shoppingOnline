<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { readApiError } from '../../../services/http'
import { merchantAdminApi } from '../services/merchant'
import type { AdminShopStaff } from '../types'

const props = defineProps<{ status: string; emptyText: string }>()
const loading = ref(false)
const items = ref<AdminShopStaff[]>([])

const statusLabel: Record<string, string> = {
  PENDING_AUDIT: '待审核',
  ACTIVE: '已通过',
  REJECTED: '已驳回',
  REVOKED: '已撤销',
  DISABLED: '已停用',
}

async function load() {
  loading.value = true
  try {
    items.value = await merchantAdminApi.staff(props.status || undefined)
  } catch (error) {
    ElMessage.error(readApiError(error, '客服列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function audit(row: AdminShopStaff, result: 'APPROVE' | 'REJECT') {
  let remark: string | undefined
  try {
    if (result === 'REJECT') {
      const prompted = await ElMessageBox.prompt(`驳回「${row.displayName}」？`, '客服审核', {
        inputPlaceholder: '原因（可选）',
        type: 'warning',
      })
      remark = String(prompted.value ?? '').trim() || undefined
    } else {
      await ElMessageBox.confirm(`通过「${row.displayName}」并开通登录？通过后才会发放密码。`, '客服审核', { type: 'success' })
    }
  } catch {
    return
  }
  try {
    await merchantAdminApi.auditStaff(row.id, result, remark)
    ElMessage.success(result === 'APPROVE' ? '已通过并开通客服账号' : '已驳回')
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error, '审核失败'))
  }
}

async function revoke(row: AdminShopStaff) {
  try {
    await ElMessageBox.confirm(`撤销「${row.displayName}」的客服权限？对方将无法登录。`, '撤销客服', { type: 'warning' })
  } catch (error) {
    return
  }
  try {
    await merchantAdminApi.revokeStaff(row.id)
    ElMessage.success('已撤销')
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error, '撤销失败'))
  }
}

async function restore(row: AdminShopStaff) {
  try {
    await ElMessageBox.confirm(`重新授予「${row.displayName}」客服权限？将重新发放登录密码。`, '恢复客服', { type: 'success' })
  } catch {
    return
  }
  try {
    await merchantAdminApi.restoreStaff(row.id)
    ElMessage.success('已恢复')
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error, '恢复失败'))
  }
}

async function retryEmail(row: AdminShopStaff) {
  try {
    await merchantAdminApi.retryStaffEmail(row.id)
    ElMessage.success('已重发登录邮件')
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error, '重发失败'))
  }
}

watch(() => props.status, load)
onMounted(load)
</script>

<template>
  <el-card shadow="never">
    <el-table v-loading="loading" :data="items" :empty-text="emptyText">
      <el-table-column prop="displayName" label="客服" min-width="160">
        <template #default="{ row }">
          <strong>{{ row.displayName }}</strong>
          <div class="muted">{{ row.maskedEmail || '无邮箱' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="shopName" label="店铺" min-width="160">
        <template #default="{ row }">{{ row.shopName || (row.shopId ? `店铺 #${row.shopId}` : '未知店铺') }}</template>
      </el-table-column>
      <el-table-column label="登录标识" min-width="140">
        <template #default="{ row }">{{ row.username || row.maskedEmail }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag effect="plain">{{ statusLabel[row.status] ?? row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditRemark" label="审核意见" min-width="160">
        <template #default="{ row }">{{ row.auditRemark || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING_AUDIT'" link type="success" @click="audit(row, 'APPROVE')">通过</el-button>
          <el-button v-if="row.status === 'PENDING_AUDIT'" link type="danger" @click="audit(row, 'REJECT')">驳回</el-button>
          <el-button v-if="row.status === 'ACTIVE' || row.status === 'DISABLED'" link type="warning" @click="revoke(row)">撤销</el-button>
          <el-button v-if="row.status === 'REVOKED' || row.status === 'REJECTED'" link type="success" @click="restore(row)">重新授予</el-button>
          <el-button v-if="row.status === 'ACTIVE' && row.mustChangePassword" link @click="retryEmail(row)">重发邮件</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.muted { color: var(--el-text-color-secondary); font-size: 12px; margin-top: 4px; }
</style>
