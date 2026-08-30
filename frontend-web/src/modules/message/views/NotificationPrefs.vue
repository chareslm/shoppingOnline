<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">PREFERENCES</p>
        <h1>通知偏好设置</h1>
        <p class="subtle">管理您希望接收的通知类型</p>
      </div>
    </div>

    <div class="prefs-container">
      <div v-if="loading" class="loading">加载中...</div>

      <template v-else-if="preference">
        <div class="prefs-section">
          <h2>通知类型</h2>
          <p class="section-desc">开启后，您将通过站内信接收到对应类型的通知</p>

          <div class="prefs-list">
            <div class="pref-item">
              <div class="pref-info">
                <div class="pref-icon">⚙️</div>
                <div>
                  <div class="pref-title">系统通知</div>
                  <div class="pref-desc">账号安全、系统维护等重要通知</div>
                </div>
              </div>
              <label class="switch">
                <input type="checkbox" :checked="toBool(preference.systemEnabled)" :disabled="saving" @change="updateField('systemEnabled', !toBool(preference.systemEnabled))" />
                <span class="slider"></span>
              </label>
            </div>

            <div class="pref-item">
              <div class="pref-info">
                <div class="pref-icon">📦</div>
                <div>
                  <div class="pref-title">订单通知</div>
                  <div class="pref-desc">下单、发货、配送、退款等订单状态变更</div>
                </div>
              </div>
              <label class="switch">
                <input type="checkbox" :checked="toBool(preference.orderEnabled)" :disabled="saving" @change="updateField('orderEnabled', !toBool(preference.orderEnabled))" />
                <span class="slider"></span>
              </label>
            </div>

            <div class="pref-item">
              <div class="pref-info">
                <div class="pref-icon">🎉</div>
                <div>
                  <div class="pref-title">营销通知</div>
                  <div class="pref-desc">优惠活动、促销信息、会员权益等</div>
                </div>
              </div>
              <label class="switch">
                <input type="checkbox" :checked="toBool(preference.marketingEnabled)" :disabled="saving" @change="updateField('marketingEnabled', !toBool(preference.marketingEnabled))" />
                <span class="slider"></span>
              </label>
            </div>

            <div class="pref-item">
              <div class="pref-info">
                <div class="pref-icon">💬</div>
                <div>
                  <div class="pref-title">客服消息</div>
                  <div class="pref-desc">在线客服回复、会话状态变更通知</div>
                </div>
              </div>
              <label class="switch">
                <input type="checkbox" :checked="toBool(preference.serviceEnabled)" :disabled="saving" @change="updateField('serviceEnabled', !toBool(preference.serviceEnabled))" />
                <span class="slider"></span>
              </label>
            </div>
          </div>
        </div>

        <div class="prefs-section">
          <h2>推送渠道</h2>
          <p class="section-desc">当前已启用以下推送渠道</p>
          <div class="channel-list">
            <div class="channel-item">
              <span class="channel-tag active">站内信</span>
              <span class="channel-desc">已启用 · 实时推送</span>
            </div>
            <div class="channel-item disabled">
              <span class="channel-tag">邮件</span>
              <span class="channel-desc">基础版预留接口 · 待扩展</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { notificationApi } from '@/modules/message/services/message'
import type { NotificationPreference } from '@/modules/message/types'
import { readApiError } from '@/services/http'

const preference = ref<NotificationPreference | null>(null)
const loading = ref(false)
const saving = ref(false)

function toBool(value: number) {
  return value === 1
}

function toInt(value: boolean) {
  return value ? 1 : 0
}

async function loadData() {
  loading.value = true
  try {
    preference.value = await notificationApi.getPreference()
  } catch (err) {
    console.error('加载偏好失败:', readApiError(err))
  } finally {
    loading.value = false
  }
}

async function updateField(field: keyof NotificationPreference, value: boolean) {
  if (!preference.value) return
  saving.value = true
  const patch: Partial<NotificationPreference> = {
    systemEnabled: preference.value.systemEnabled,
    orderEnabled: preference.value.orderEnabled,
    marketingEnabled: preference.value.marketingEnabled,
    serviceEnabled: preference.value.serviceEnabled,
  }
  ;(patch as any)[field] = toInt(value)
  try {
    const updated = await notificationApi.updatePreference(patch)
    preference.value = updated
  } catch (err) {
    alert('保存失败：' + readApiError(err))
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.prefs-container { max-width: 720px; }
.prefs-section { background: var(--paper, #fff); border: 1px solid var(--line, #e8e8e8); border-radius: 12px; padding: 24px; margin-bottom: 20px; }
.prefs-section h2 { margin: 0 0 4px; font-size: 16px; }
.section-desc { margin: 0 0 16px; font-size: 13px; color: #888; }
.prefs-list { display: flex; flex-direction: column; gap: 4px; }
.pref-item { display: flex; align-items: center; justify-content: space-between; padding: 14px 0; border-bottom: 1px solid var(--line); }
.pref-item:last-child { border-bottom: none; }
.pref-info { display: flex; gap: 12px; align-items: flex-start; }
.pref-icon { font-size: 24px; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 10px; }
.pref-title { font-weight: 500; font-size: 14px; }
.pref-desc { font-size: 12px; color: #888; margin-top: 2px; }
.switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; inset: 0; background: #ccc; border-radius: 24px; transition: 0.3s; }
.slider::before { position: absolute; content: ''; height: 18px; width: 18px; left: 3px; bottom: 3px; background: #fff; border-radius: 50%; transition: 0.3s; }
input:checked + .slider { background: var(--green, #00843d); }
input:checked + .slider::before { transform: translateX(20px); }
input:disabled + .slider { opacity: 0.5; cursor: not-allowed; }
.channel-list { display: flex; flex-direction: column; gap: 12px; }
.channel-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: #f9f9f9; border-radius: 8px; }
.channel-tag { padding: 4px 12px; border-radius: 6px; font-size: 13px; font-weight: 500; background: #e8e8e8; color: #666; }
.channel-tag.active { background: var(--green); color: #fff; }
.channel-desc { font-size: 13px; color: #888; }
.loading { text-align: center; padding: 40px; color: #999; }
</style>