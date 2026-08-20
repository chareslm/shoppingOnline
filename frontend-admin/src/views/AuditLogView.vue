<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { authApi } from '../services/auth'
import { readApiError } from '../services/http'
import type { AuditLog } from '../types/auth'

const loading = ref(false)
const logs = ref<AuditLog[]>([])
const total = ref(0)
const detailVisible = ref(false)
const selected = ref<AuditLog | null>(null)
const query = reactive({
  actorKeyword: '',
  module: '',
  actionCode: '',
  success: '' as '' | 'true' | 'false',
  timeRange: defaultTimeRange(),
  page: 1,
  pageSize: 20,
})

async function loadLogs() {
  loading.value = true
  try {
    const result = await authApi.auditLogs({
      actorKeyword: query.actorKeyword.trim() || undefined,
      module: query.module || undefined,
      actionCode: query.actionCode.trim() || undefined,
      success: query.success === '' ? undefined : query.success === 'true',
      startAt: query.timeRange?.[0],
      endAt: query.timeRange?.[1],
      page: query.page,
      pageSize: query.pageSize,
    })
    logs.value = result.items
    total.value = Number(result.total)
  } catch (error) {
    ElMessage.error(readApiError(error, '审计日志加载失败'))
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  void loadLogs()
}

function reset() {
  query.actorKeyword = ''
  query.module = ''
  query.actionCode = ''
  query.success = ''
  query.timeRange = defaultTimeRange()
  search()
}

function showDetail(log: AuditLog) {
  selected.value = log
  detailVisible.value = true
}

function defaultTimeRange(): [string, string] {
  const end = new Date()
  const start = new Date(end.getTime() - 7 * 24 * 60 * 60 * 1000)
  return [formatInputTime(start), formatInputTime(end)]
}

function formatInputTime(value: Date) {
  const offset = value.getTimezoneOffset() * 60_000
  return new Date(value.getTime() - offset).toISOString().slice(0, 19)
}

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

function formatDetail(value: unknown) {
  return value == null ? '无扩展详情' : JSON.stringify(value, null, 2)
}

onMounted(loadLogs)
</script>

<template>
  <section>
    <div class="page-heading">
      <div><p class="eyebrow">SECURITY AUDIT</p><h1>审计日志</h1><p>追踪登录、安全和高风险授权操作；客户端信息与扩展详情已按规则脱敏。</p></div>
    </div>

    <el-card shadow="never" class="filter-card audit-filter-card">
      <el-form inline @submit.prevent="search">
        <el-form-item label="操作者">
          <el-input v-model="query.actorKeyword" clearable placeholder="用户名 / 用户 ID" :prefix-icon="Search" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="query.module" clearable placeholder="全部模块" style="width: 150px">
            <el-option label="认证安全" value="AUTH" /><el-option label="授权管理" value="AUTHORIZATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作">
          <el-input v-model="query.actionCode" clearable placeholder="如 PASSWORD_LOGIN" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.success" placeholder="全部结果" style="width: 130px">
            <el-option label="全部结果" value="" /><el-option label="成功" value="true" /><el-option label="失败" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="query.timeRange" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" />
        </el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="logs" row-key="id" empty-text="暂无审计记录" @row-click="showDetail">
        <el-table-column label="时间" width="180"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作者" min-width="190">
          <template #default="{ row }"><strong>{{ row.actorUsername || '匿名/未知' }}</strong><div class="contact-line">ID：{{ row.actorUserId || '—' }}</div></template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="155" show-overflow-tooltip />
        <el-table-column prop="actionCode" label="动作" min-width="190" show-overflow-tooltip />
        <el-table-column label="结果" width="90"><template #default="{ row }"><el-tag :type="row.success ? 'success' : 'danger'" effect="plain">{{ row.success ? '成功' : '失败' }}</el-tag></template></el-table-column>
        <el-table-column label="目标" min-width="150"><template #default="{ row }">{{ row.targetType || '—' }}<span v-if="row.targetId"> / {{ row.targetId }}</span></template></el-table-column>
        <el-table-column prop="maskedClientIp" label="来源 IP" width="135" />
        <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="showDetail(row)">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" class="table-pagination" layout="total, sizes, prev, pager, next" :page-sizes="[20, 50, 100]" :total="total" @current-change="loadLogs" @size-change="search" />
    </el-card>

    <el-drawer v-model="detailVisible" title="审计详情" size="520px">
      <el-descriptions v-if="selected" :column="1" border>
        <el-descriptions-item label="日志 ID">{{ selected.id }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTime(selected.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="操作者">{{ selected.actorUsername || '匿名/未知' }}（{{ selected.actorUserId || '—' }}）</el-descriptions-item>
        <el-descriptions-item label="模块 / 动作">{{ selected.module }} / {{ selected.actionCode }}</el-descriptions-item>
        <el-descriptions-item label="执行结果"><el-tag :type="selected.success ? 'success' : 'danger'">{{ selected.success ? '成功' : '失败' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="操作目标">{{ selected.targetType || '—' }} / {{ selected.targetId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ selected.requestMethod || '—' }} {{ selected.requestPath || '—' }}</el-descriptions-item>
        <el-descriptions-item label="客户端">{{ selected.client || '—' }}</el-descriptions-item>
        <el-descriptions-item label="来源 IP">{{ selected.maskedClientIp || '—' }}</el-descriptions-item>
        <el-descriptions-item label="Trace ID">{{ selected.traceId || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="selected" class="audit-detail-block"><h3>扩展详情（已脱敏）</h3><pre>{{ formatDetail(selected.detail) }}</pre></div>
    </el-drawer>
  </section>
</template>

<style scoped>
.audit-filter-card :deep(.el-card__body) { padding-bottom: 2px; }
.audit-filter-card :deep(.el-form-item) { margin-bottom: 16px; }
.audit-detail-block { margin-top: 24px; }
.audit-detail-block pre { margin: 0; padding: 16px; overflow: auto; border-radius: 8px; color: #dbeafe; background: #172033; font: 13px/1.7 ui-monospace, SFMono-Regular, Consolas, monospace; white-space: pre-wrap; word-break: break-word; }
</style>
