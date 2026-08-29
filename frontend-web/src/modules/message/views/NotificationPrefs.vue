<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { readApiError } from '@/services/http'
import { notificationApi } from '../services/message'
import type { NotificationPreference } from '../types'

const prefs = ref<NotificationPreference | null>(null)
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const isError = ref(false)

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function load() {
  loading.value = true
  message.value = ''
  try {
    prefs.value = await notificationApi.getPreference()
  } catch (error) {
    showMessage(readApiError(error, '偏好加载失败'), true)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!prefs.value || saving.value) return
  saving.value = true
  message.value = ''
  try {
    prefs.value = await notificationApi.updatePreference({
      systemEnabled: prefs.value.systemEnabled,
      orderEnabled: prefs.value.orderEnabled,
      marketingEnabled: prefs.value.marketingEnabled,
      serviceEnabled: prefs.value.serviceEnabled,
    })
    showMessage('偏好设置已保存')
  } catch (error) {
    showMessage(readApiError(error, '保存失败'), true)
  } finally {
    saving.value = false
  }
}

function toggle(field: 'systemEnabled' | 'orderEnabled' | 'marketingEnabled' | 'serviceEnabled') {
  if (!prefs.value) return
  prefs.value[field] = prefs.value[field] === 1 ? 0 : 1
}

onMounted(load)
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">PREFERENCES</p>
        <h1>通知偏好</h1>
        <p>选择你希望接收哪些类型的通知。</p>
      </div>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div v-if="loading" class="loading-card">加载设置…</div>

    <div v-else-if="prefs" class="prefs-list">
      <article class="section-card pref-item" @click="toggle('systemEnabled')">
        <div class="pref-icon">⚙️</div>
        <div class="pref-info">
          <strong>系统通知</strong>
          <p class="muted">平台公告、账号安全提醒等重要信息</p>
        </div>
        <div :class="['pref-switch', { on: prefs.systemEnabled === 1 }]">
          <div class="switch-thumb"></div>
        </div>
      </article>

      <article class="section-card pref-item" @click="toggle('orderEnabled')">
        <div class="pref-icon">📦</div>
        <div class="pref-info">
          <strong>订单通知</strong>
          <p class="muted">订单状态变更、支付确认、发货提醒等</p>
        </div>
        <div :class="['pref-switch', { on: prefs.orderEnabled === 1 }]">
          <div class="switch-thumb"></div>
        </div>
      </article>

      <article class="section-card pref-item" @click="toggle('marketingEnabled')">
        <div class="pref-icon">🎁</div>
        <div class="pref-info">
          <strong>营销通知</strong>
          <p class="muted">促销活动、优惠券、新品推荐等</p>
        </div>
        <div :class="['pref-switch', { on: prefs.marketingEnabled === 1 }]">
          <div class="switch-thumb"></div>
        </div>
      </article>

      <article class="section-card pref-item" @click="toggle('serviceEnabled')">
        <div class="pref-icon">💬</div>
        <div class="pref-info">
          <strong>客服消息</strong>
          <p class="muted">客服回复、会话提醒等</p>
        </div>
        <div :class="['pref-switch', { on: prefs.serviceEnabled === 1 }]">
          <div class="switch-thumb"></div>
        </div>
      </article>

      <div class="pref-actions">
        <button class="primary-button" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存设置' }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.prefs-list {
  display: grid;
  gap: 12px;
  max-width: 640px;
}

.pref-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  cursor: pointer;
  transition: background .15s;
}

.pref-item:hover {
  background: #f6f8f6;
}

.pref-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #e9f5ef;
  font-size: 22px;
  flex-shrink: 0;
}

.pref-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pref-info p {
  margin: 0;
  font-size: 13px;
}

.pref-switch {
  width: 48px;
  height: 28px;
  border-radius: 999px;
  background: #dee2e6;
  padding: 3px;
  transition: background .2s;
  flex-shrink: 0;
}

.pref-switch.on {
  background: var(--green);
}

.switch-thumb {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: white;
  transition: transform .2s;
}

.pref-switch.on .switch-thumb {
  transform: translateX(20px);
}

.pref-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}
</style>
