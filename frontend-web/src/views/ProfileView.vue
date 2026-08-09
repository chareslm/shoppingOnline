<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { readApiError } from '@/services/http'
import { userApi } from '@/services/user'
import type { UpdateUserProfile } from '@/types/user'

const loading = ref(true)
const saving = ref(false)
const message = ref('')
const isError = ref(false)
const form = reactive<UpdateUserProfile>({ nickname: '', avatarUrl: '', realName: '', gender: 'UNKNOWN', birthday: null, bio: '' })

onMounted(async () => {
  try {
    const profile = await userApi.profile()
    Object.assign(form, {
      nickname: profile.nickname || '', avatarUrl: profile.avatarUrl || '', realName: profile.realName || '',
      gender: profile.gender || 'UNKNOWN', birthday: profile.birthday, bio: profile.bio || '',
    })
  } catch (error) {
    showMessage(readApiError(error, '资料加载失败'), true)
  } finally {
    loading.value = false
  }
})

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function save() {
  saving.value = true
  message.value = ''
  try {
    const updated = await userApi.updateProfile({ ...form, birthday: form.birthday || null })
    Object.assign(form, { ...updated, nickname: updated.nickname || '', avatarUrl: updated.avatarUrl || '', realName: updated.realName || '', bio: updated.bio || '' })
    showMessage('个人资料已保存')
  } catch (error) {
    showMessage(readApiError(error, '资料保存失败'), true)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="page-stack narrow-page">
    <div class="page-heading"><div><p class="eyebrow">PROFILE</p><h1>个人资料</h1><p>这些信息只属于当前登录账号。</p></div></div>
    <div v-if="loading" class="loading-card">正在加载个人资料…</div>
    <form v-else class="section-card form-card" @submit.prevent="save">
      <div class="avatar-preview"><span>{{ form.nickname?.slice(0, 1) || 'U' }}</span><div><strong>账号头像</strong><small>填写网络图片地址后将在后续页面展示</small></div></div>
      <div class="form-grid">
        <label>昵称<input v-model="form.nickname" maxlength="64" placeholder="希望大家如何称呼你" /></label>
        <label>真实姓名<input v-model="form.realName" maxlength="64" placeholder="可选" /></label>
        <label>性别<select v-model="form.gender"><option value="UNKNOWN">不设置</option><option value="MALE">男</option><option value="FEMALE">女</option></select></label>
        <label>生日<input v-model="form.birthday" type="date" /></label>
        <label class="full">头像 URL<input v-model="form.avatarUrl" type="url" maxlength="512" placeholder="https://example.com/avatar.png" /></label>
        <label class="full">个人简介<textarea v-model="form.bio" maxlength="500" rows="5" placeholder="介绍一下自己"></textarea><small>{{ form.bio.length }}/500</small></label>
      </div>
      <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>
      <div class="form-actions"><button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存资料' }}</button></div>
    </form>
  </section>
</template>
