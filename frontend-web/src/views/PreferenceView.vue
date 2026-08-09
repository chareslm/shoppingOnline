<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { readApiError } from '@/services/http'
import { userApi } from '@/services/user'
import type { UpdateUserPreference } from '@/types/user'

const loading = ref(true)
const saving = ref(false)
const message = ref('')
const isError = ref(false)
const form = reactive<UpdateUserPreference>({ marketingEnabled: false, orderNotificationEnabled: true, systemNotificationEnabled: true, extraPreferences: {} })

onMounted(async () => {
  try {
    Object.assign(form, await userApi.preference())
  } catch (error) {
    message.value = readApiError(error, '偏好加载失败')
    isError.value = true
  } finally {
    loading.value = false
  }
})

async function save() {
  saving.value = true
  message.value = ''
  try {
    Object.assign(form, await userApi.updatePreference({ ...form }))
    message.value = '偏好设置已保存'
    isError.value = false
  } catch (error) {
    message.value = readApiError(error, '偏好保存失败')
    isError.value = true
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="page-stack narrow-page">
    <div class="page-heading"><div><p class="eyebrow">PREFERENCES</p><h1>通知偏好</h1><p>决定哪些类型的消息可以主动提醒你。</p></div></div>
    <div v-if="loading" class="loading-card">正在加载偏好设置…</div>
    <form v-else class="section-card preference-list" @submit.prevent="save">
      <label class="preference-item"><span><strong>订单通知</strong><small>订单状态、发货及售后进度</small></span><input v-model="form.orderNotificationEnabled" type="checkbox" role="switch" /></label>
      <label class="preference-item"><span><strong>系统通知</strong><small>账号安全和平台重要公告</small></span><input v-model="form.systemNotificationEnabled" type="checkbox" role="switch" /></label>
      <label class="preference-item"><span><strong>营销信息</strong><small>优惠活动、新品和个性化推荐</small></span><input v-model="form.marketingEnabled" type="checkbox" role="switch" /></label>
      <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>
      <div class="form-actions"><button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存偏好' }}</button></div>
    </form>
  </section>
</template>
