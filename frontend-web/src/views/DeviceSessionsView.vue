<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/services/auth'
import { readApiError } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { DeviceSession } from '@/types/auth'

const router = useRouter()
const auth = useAuthStore()
const devices = ref<DeviceSession[]>([])
const loading = ref(true)
const actingId = ref('')
const message = ref('')
const isError = ref(false)

onMounted(loadDevices)

async function loadDevices() {
  loading.value = true
  message.value = ''
  try {
    devices.value = await authApi.devices()
  } catch (error) {
    message.value = readApiError(error, '登录设备加载失败')
    isError.value = true
  } finally {
    loading.value = false
  }
}

async function revoke(device: DeviceSession) {
  const label = device.current ? '当前设备' : device.deviceName || deviceLabel(device.deviceType)
  if (!window.confirm(`确定要退出${label}吗？该设备将无法继续刷新登录状态。`)) return
  actingId.value = device.id
  message.value = ''
  try {
    await authApi.revokeDevice(device.id)
    if (device.current) {
      auth.clearLocalSession()
      await router.replace({ name: 'login' })
      return
    }
    await loadDevices()
    message.value = '指定设备已退出'
    isError.value = false
  } catch (error) {
    message.value = readApiError(error, '设备退出失败')
    isError.value = true
  } finally {
    actingId.value = ''
  }
}

async function revokeOthers() {
  if (!window.confirm('确定退出除当前设备以外的全部设备吗？')) return
  actingId.value = 'others'
  message.value = ''
  try {
    await authApi.revokeOtherDevices()
    await loadDevices()
    message.value = '其他设备已全部退出'
    isError.value = false
  } catch (error) {
    message.value = readApiError(error, '其他设备退出失败')
    isError.value = true
  } finally {
    actingId.value = ''
  }
}

function deviceLabel(type: DeviceSession['deviceType']) {
  return ({ WEB: '网页浏览器', ANDROID: 'Android App', MINIAPP: '微信小程序', ADMIN_WEB: '管理端' })[type]
}

function formatTime(value: string | null) {
  if (!value) return '无有效会话'
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<template>
  <section class="page-stack narrow-page">
    <div class="page-heading split-heading">
      <div>
        <p class="eyebrow">SECURITY</p>
        <h1>登录设备</h1>
        <p>查看使用过本账号的设备，并撤销不再使用的登录会话。</p>
      </div>
      <button class="secondary-button" type="button" :disabled="Boolean(actingId)" @click="revokeOthers">
        {{ actingId === 'others' ? '正在退出…' : '退出其他设备' }}
      </button>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>
    <div v-if="loading" class="loading-card">正在加载登录设备…</div>
    <div v-else-if="devices.length === 0" class="section-card empty-state">
      <span>⌁</span><h2>暂无设备记录</h2><p>完成一次登录后，设备会显示在这里。</p>
    </div>
    <div v-else class="device-list">
      <article v-for="device in devices" :key="device.id" :class="['section-card', 'device-card', { current: device.current }]">
        <div class="device-icon">{{ device.deviceType === 'ANDROID' ? 'A' : device.deviceType === 'MINIAPP' ? '微' : 'W' }}</div>
        <div class="device-detail">
          <div class="device-title">
            <strong>{{ device.deviceName || deviceLabel(device.deviceType) }}</strong>
            <span v-if="device.current" class="current-badge">当前设备</span>
            <span :class="['status-badge', device.status.toLowerCase()]">{{ device.status === 'ACTIVE' ? '会话有效' : '已退出' }}</span>
          </div>
          <p>{{ deviceLabel(device.deviceType) }}<template v-if="device.appVersion"> · {{ device.appVersion }}</template></p>
          <small>最近活跃：{{ formatTime(device.lastActiveAt) }} · IP：{{ device.maskedIp || '未知' }}</small>
          <small>会话有效期：{{ formatTime(device.sessionExpiresAt) }}</small>
        </div>
        <button class="text-button danger" type="button" :disabled="Boolean(actingId)" @click="revoke(device)">
          {{ actingId === device.id ? '退出中…' : device.current ? '退出当前设备' : '退出设备' }}
        </button>
      </article>
    </div>

    <p class="muted session-note">退出后，该设备的 Refresh Token 会立即失效；已签发的 Access Token 最长仍可能使用 30 分钟。</p>
  </section>
</template>

<style scoped>
.device-list { display: grid; gap: 14px; }
.device-card { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 18px; padding: 22px 24px; }
.device-card.current { border-color: #79a995; box-shadow: inset 4px 0 var(--green); }
.device-icon { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 15px; color: var(--green-dark); background: var(--lime); font-weight: 800; }
.device-title { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.device-title strong { font-size: 17px; }
.current-badge, .status-badge { padding: 4px 8px; border-radius: 99px; font-size: 11px; font-weight: 800; }
.current-badge { color: var(--green-dark); background: var(--lime); }
.status-badge.active { color: #15553f; background: #e9f5ef; }
.status-badge.revoked { color: #68756e; background: #edf2ed; }
.device-detail p { margin: 7px 0; color: var(--muted); }
.device-detail small { display: block; margin-top: 4px; color: #7a867f; }
.session-note { margin: 0; font-size: 13px; line-height: 1.7; }
button:disabled { opacity: .55; cursor: wait; }
@media (max-width: 640px) {
  .device-card { grid-template-columns: auto 1fr; }
  .device-card > button { grid-column: 1 / -1; width: 100%; padding: 10px; }
}
</style>
