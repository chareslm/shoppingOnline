<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { readApiError } from '@/services/http'
import { paymentApi } from '../services/trade'
import { PAYMENT_STATUS_LABELS, type PaymentOrder } from '../types'

const route = useRoute()
const router = useRouter()
const payment = ref<PaymentOrder | null>(null)
const loading = ref(true)
const paying = ref(false)
const message = ref('')
const isError = ref(false)

const paymentOrderId = String(route.params.paymentOrderId)

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function loadPayment() {
  loading.value = true
  try {
    payment.value = await paymentApi.detail(paymentOrderId)
  } catch (error) {
    showMessage(readApiError(error, '支付单加载失败'), true)
  } finally {
    loading.value = false
  }
}

onMounted(loadPayment)

async function mockPay() {
  paying.value = true
  try {
    payment.value = await paymentApi.mockPay(paymentOrderId)
    showMessage('支付成功')
  } catch (error) {
    showMessage(readApiError(error, '支付失败'), true)
  } finally {
    paying.value = false
  }
}

function goOrder() {
  if (payment.value) void router.push({ name: 'trade-order-detail', params: { orderId: String(payment.value.orderId) } })
}

function formatMoney(value: number) {
  return `¥${value.toFixed(2)}`
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <section class="page-stack narrow-page">
    <div class="page-heading split-heading">
      <div>
        <p class="eyebrow">PAYMENT</p>
        <h1>收银台</h1>
        <p>当前为本地模拟支付渠道（MOCK_WECHAT）。</p>
      </div>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div v-if="loading" class="loading-card">正在加载支付单…</div>

    <template v-else-if="payment">
      <div class="section-card pay-card">
        <div class="pay-amount">
          <span class="muted">应付金额</span>
          <strong>{{ formatMoney(payment.amount) }}</strong>
        </div>
        <div class="pay-meta">
          <div><span class="muted">支付单号</span><span>{{ payment.paymentNo }}</span></div>
          <div><span class="muted">支付渠道</span><span>{{ payment.payChannel }}</span></div>
          <div><span class="muted">状态</span><span>{{ PAYMENT_STATUS_LABELS[payment.status] }}</span></div>
          <div><span class="muted">支付时间</span><span>{{ formatTime(payment.payTime) }}</span></div>
        </div>
        <button v-if="payment.status === 0" class="primary-button wide" type="button" :disabled="paying" @click="mockPay">
          {{ paying ? '支付中…' : '模拟支付' }}
        </button>
        <button v-else class="primary-button wide" type="button" @click="goOrder">查看订单</button>
      </div>
    </template>

    <div v-else class="section-card empty-state">
      <span>💳</span>
      <h2>支付单不存在</h2>
      <p>该支付单可能已失效或不属于当前账号。</p>
      <router-link class="primary-button" to="/orders">返回订单列表</router-link>
    </div>
  </section>
</template>

<style scoped>
.pay-card {
  display: grid;
  gap: 22px;
  text-align: center;
}

.pay-amount {
  display: grid;
  gap: 8px;
  padding: 18px 0 8px;
}

.pay-amount strong {
  font-size: 52px;
  letter-spacing: -.03em;
  color: var(--green-dark);
}

.pay-meta {
  display: grid;
  gap: 10px;
  padding: 18px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  text-align: left;
}

.pay-meta > div {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  font-size: 14px;
}
</style>