<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { readApiError } from '@/services/http'
import { orderApi, paymentApi, refundApi } from '../services/trade'
import { ORDER_STATUS_LABELS, REFUND_STATUS_LABELS, type Order, type RefundOrder } from '../types'

const route = useRoute()
const router = useRouter()
const order = ref<Order | null>(null)
const refunds = ref<RefundOrder[]>([])
const loading = ref(true)
const acting = ref(false)
const message = ref('')
const isError = ref(false)

const refundModalOpen = ref(false)
const refundAmount = ref('')
const refundReason = ref('')
const refundSubmitting = ref(false)

const orderId = String(route.params.orderId)

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function loadDetail() {
  loading.value = true
  try {
    order.value = await orderApi.detail(orderId)
    refunds.value = (await refundApi.list()).filter((refund) => refund.orderId === orderId)
  } catch (error) {
    showMessage(readApiError(error, '订单加载失败'), true)
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)

async function withActing(action: () => Promise<unknown>) {
  if (acting.value) return
  acting.value = true
  try {
    await action()
    await loadDetail()
  } catch (error) {
    showMessage(readApiError(error, '操作失败'), true)
  } finally {
    acting.value = false
  }
}

function cancelOrder() {
  if (!order.value || !window.confirm(`确定取消订单 ${order.value.orderNo} 吗？`)) return
  void withActing(() => orderApi.cancel(order.value!.orderId))
}

function confirmOrder() {
  if (!window.confirm('确认已收到商品吗？')) return
  void withActing(() => orderApi.confirm(order.value!.orderId))
}

async function goPay() {
  if (!order.value) return
  try {
    const payment = await paymentApi.create(order.value.orderId)
    await router.push({ name: 'trade-payment', params: { paymentOrderId: String(payment.paymentOrderId) } })
  } catch (error) {
    showMessage(readApiError(error, '创建支付单失败'), true)
  }
}

function openRefundModal() {
  if (!order.value) return
  refundAmount.value = String(order.value.payAmount)
  refundReason.value = ''
  refundModalOpen.value = true
}

async function submitRefund() {
  if (!order.value) return
  const amount = Number(refundAmount.value)
  if (!Number.isFinite(amount) || amount <= 0 || amount > order.value.payAmount) {
    showMessage(`退款金额需在 0.01 ~ ${order.value.payAmount.toFixed(2)} 之间`, true)
    return
  }
  refundSubmitting.value = true
  try {
    await refundApi.create({
      orderId: order.value.orderId,
      amount,
      reason: refundReason.value.trim() || undefined,
    })
    refundModalOpen.value = false
    showMessage('退款申请已提交')
    await loadDetail()
  } catch (error) {
    showMessage(readApiError(error, '退款申请失败'), true)
  } finally {
    refundSubmitting.value = false
  }
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
        <p class="eyebrow">ORDER DETAIL</p>
        <h1>订单详情</h1>
        <p v-if="order">订单号 {{ order.orderNo }}</p>
      </div>
      <button class="secondary-button" type="button" @click="router.push({ name: 'trade-orders' })">← 返回列表</button>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div v-if="loading" class="loading-card">正在加载订单详情…</div>

    <template v-else-if="order">
      <div class="section-card detail-status">
        <div>
          <p class="eyebrow">STATUS</p>
          <h2 :class="`status-text status-${order.status}`">{{ ORDER_STATUS_LABELS[order.status] }}</h2>
        </div>
        <div class="detail-actions">
          <button v-if="order.status === 0" class="secondary-button" type="button" :disabled="acting" @click="cancelOrder">取消订单</button>
          <button v-if="order.status === 0" class="primary-button" type="button" :disabled="acting" @click="goPay">去支付</button>
          <button v-if="order.status === 2" class="primary-button" type="button" :disabled="acting" @click="confirmOrder">确认收货</button>
          <button v-if="order.status === 1" class="secondary-button" type="button" @click="openRefundModal">申请退款</button>
        </div>
      </div>

      <div class="section-card">
        <p class="eyebrow">GOODS</p>
        <div class="detail-items">
          <div v-for="item in order.items" :key="item.itemId" class="detail-item">
            <div class="cart-thumb">{{ item.skuImage || '📦' }}</div>
            <div class="detail-item-info">
              <strong>{{ item.skuName || `SKU #${item.skuId}` }}</strong>
              <small class="muted">{{ formatMoney(item.price) }} × {{ item.quantity }}</small>
            </div>
            <strong>{{ formatMoney(item.totalAmount) }}</strong>
          </div>
        </div>
      </div>

      <div class="section-card">
        <p class="eyebrow">AMOUNT</p>
        <div class="amount-lines">
          <div><span class="muted">商品总额</span><span>{{ formatMoney(order.totalAmount) }}</span></div>
          <div><span class="muted">优惠金额</span><span>− {{ formatMoney(order.discountAmount) }}</span></div>
          <div><span class="muted">运费</span><span>{{ formatMoney(order.freightAmount) }}</span></div>
          <div class="amount-total"><span>实付金额</span><strong>{{ formatMoney(order.payAmount) }}</strong></div>
        </div>
      </div>

      <div class="section-card">
        <p class="eyebrow">DELIVERY</p>
        <div class="info-lines">
          <div><span class="muted">收货人</span><span>{{ order.receiverName }} {{ order.receiverPhone }}</span></div>
          <div><span class="muted">收货地址</span><span>{{ order.receiverAddress }}</span></div>
          <div v-if="order.remark"><span class="muted">备注</span><span>{{ order.remark }}</span></div>
        </div>
      </div>

      <div class="section-card">
        <p class="eyebrow">TIMELINE</p>
        <div class="info-lines">
          <div><span class="muted">订单关闭时间</span><span>{{ formatTime(order.closeTime) }}</span></div>
          <div><span class="muted">支付时间</span><span>{{ formatTime(order.payTime) }}</span></div>
          <div><span class="muted">完成时间</span><span>{{ formatTime(order.finishTime) }}</span></div>
        </div>
      </div>

      <div v-if="refunds.length" class="section-card">
        <p class="eyebrow">REFUNDS</p>
        <div class="info-lines">
          <div v-for="refund in refunds" :key="refund.refundId">
            <span class="muted">{{ refund.refundNo }}</span>
            <span>{{ REFUND_STATUS_LABELS[refund.status] }} · {{ formatMoney(refund.amount) }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="section-card empty-state">
      <span>📋</span>
      <h2>订单不存在</h2>
      <p>该订单可能已被删除或不属于当前账号。</p>
      <router-link class="primary-button" to="/orders">返回订单列表</router-link>
    </div>

    <div v-if="refundModalOpen" class="modal-backdrop" @mousedown.self="refundModalOpen = false">
      <form class="modal-card" @submit.prevent="submitRefund">
        <div class="modal-head">
          <div><p class="eyebrow">REFUND</p><h2>申请退款</h2></div>
          <button class="icon-button" type="button" aria-label="关闭" @click="refundModalOpen = false">×</button>
        </div>
        <div class="form-grid compact">
          <label class="full">退款金额（元）<input v-model="refundAmount" type="number" min="0.01" :max="order?.payAmount" step="0.01" required /></label>
          <label class="full">退款原因<textarea v-model="refundReason" rows="3" maxlength="255" placeholder="选填，例如：七天无理由退货"></textarea></label>
        </div>
        <div class="form-actions">
          <button class="secondary-button" type="button" @click="refundModalOpen = false">取消</button>
          <button class="primary-button" type="submit" :disabled="refundSubmitting">{{ refundSubmitting ? '提交中…' : '提交申请' }}</button>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.detail-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.detail-status h2 {
  margin: 0;
  font-size: 34px;
}

.status-text.status-0 { color: #8a6a00; }
.status-text.status-1 { color: #1d5fa8; }
.status-text.status-2 { color: #15553f; }
.status-text.status-3 { color: #526059; }
.status-text.status-4 { color: #8b948f; }
.status-text.status-6 { color: #6b3fa0; }

.detail-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-items {
  display: grid;
  gap: 14px;
}

.detail-item {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  align-items: center;
  gap: 14px;
}

.cart-thumb {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #eef2ef;
  color: #7d8a83;
  font-size: 22px;
}

.detail-item-info strong {
  display: block;
  margin-bottom: 4px;
}

.detail-item-info small {
  font-size: 13px;
}

.amount-lines,
.info-lines {
  display: grid;
  gap: 12px;
}

.amount-lines > div,
.info-lines > div {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.amount-total {
  padding-top: 12px;
  border-top: 1px solid var(--line);
  font-weight: 700;
}

.amount-total strong {
  font-size: 22px;
  color: var(--green-dark);
}
</style>