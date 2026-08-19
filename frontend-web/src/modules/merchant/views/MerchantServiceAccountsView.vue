<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { readApiError } from '@/services/http'
import { merchantApi } from '../services/merchant'
import type { ShopStaffAccount } from '../types'

const items = ref<ShopStaffAccount[]>([])
const loading = ref(false)
const submitting = ref(false)
const message = ref('')
const isError = ref(false)
const shopName = ref('')
const form = reactive({ displayName: '', email: '', username: '' })

function show(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function load() {
  loading.value = true
  try {
    const [staff, shop] = await Promise.all([
      merchantApi.listStaff(),
      merchantApi.currentShop().catch(() => null),
    ])
    items.value = staff
    if (shop?.name) shopName.value = shop.name
  } catch (error) {
    show(readApiError(error, '客服账号加载失败'), true)
  } finally {
    loading.value = false
  }
}

function staffStatusLabel(status: ShopStaffAccount['status']) {
  return {
    PENDING_AUDIT: '待审核',
    ACTIVE: '已开通',
    REJECTED: '已驳回',
    REVOKED: '已撤销',
    DISABLED: '已停用',
  }[status]
}

onMounted(load)

async function createStaff() {
  if (!form.displayName.trim() || !form.email.trim()) {
    show('请填写客服显示名和邮箱', true)
    return
  }
  submitting.value = true
  try {
    await merchantApi.createStaff({
      displayName: form.displayName.trim(),
      email: form.email.trim(),
      username: form.username.trim() || undefined,
    })
    form.displayName = ''
    form.email = ''
    form.username = ''
    show('已提交平台审核，通过后客服才能登录')
    await load()
  } catch (error) {
    show(readApiError(error, '创建失败'), true)
  } finally {
    submitting.value = false
  }
}

async function retry(item: ShopStaffAccount) {
  try {
    const result = await merchantApi.retryStaffEmail(item.id)
    show(result.emailDeliveryStatus === 'SENT' ? '临时密码已重发' : '邮件仍然发送失败，请检查 SMTP')
    await load()
  } catch (error) {
    show(readApiError(error, '重发失败'), true)
  }
}
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">MERCHANT SERVICE</p>
        <h1>客服账号</h1>
        <p>提交客服申请后由平台审核。通过后客服才能在用户 Web 以商家身份登录，并且只能进入用户沟通页。{{ shopName ? `当前店铺：${shopName}` : '' }}</p>
      </div>
    </div>
    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>
    <div class="section-card form-card">
      <div class="form-grid">
        <label>显示名<input v-model="form.displayName" maxlength="64" placeholder="客服昵称" /></label>
        <label>邮箱<input v-model="form.email" type="email" maxlength="254" placeholder="用于登录和接收临时密码" /></label>
        <label class="full">用户名（可选）<input v-model="form.username" maxlength="64" placeholder="字母开头，3–64 位" /></label>
      </div>
      <div class="form-actions">
        <button class="primary-button" type="button" :disabled="submitting" @click="createStaff">提交审核</button>
      </div>
    </div>
    <div v-if="loading" class="loading-card">正在加载客服账号…</div>
    <div v-else-if="!items.length" class="section-card empty-state">
      <h2>还没有客服账号</h2>
      <p>提交后等待平台审核，通过后对方才能登录。</p>
    </div>
    <div v-else class="section-card manage-table">
      <table>
        <thead>
          <tr><th>客服</th><th>店铺</th><th>登录标识</th><th>状态</th><th>审核意见</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>
              <strong>{{ item.displayName }}</strong>
              <small class="muted block">{{ item.maskedEmail || '无邮箱' }}</small>
            </td>
            <td>{{ item.shopName || shopName || '本店' }}</td>
            <td>{{ item.username || item.maskedEmail }}</td>
            <td>
              <span class="status-pill">{{ staffStatusLabel(item.status) }}</span>
              <span v-if="item.mustChangePassword && item.status === 'ACTIVE'" class="status-pill">待改密</span>
            </td>
            <td>{{ item.auditRemark || '—' }}</td>
            <td>
              <div class="action-group">
                <button v-if="item.status === 'ACTIVE' && item.mustChangePassword" class="text-button" type="button" @click="retry(item)">重发邮件</button>
                <span v-else class="muted">等待平台处理</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.block { display: block; margin-top: 4px; }
.manage-table { padding: 0; overflow: auto; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 16px 20px; text-align: left; border-bottom: 1px solid var(--line); }
.status-pill { margin-right: 6px; padding: 4px 10px; border-radius: 999px; background: #edf2ed; font-size: 12px; font-weight: 700; }
.action-group { display: flex; gap: 8px; }
</style>
