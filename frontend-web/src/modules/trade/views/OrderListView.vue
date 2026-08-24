<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { readApiError } from '@/services/http'
import { orderApi, paymentApi } from '../services/trade'
import { ORDER_STATUS_LABELS, type Order } from '../types'

const router = useRouter()
const orders = ref<Order[]>([])
const loading = ref(true)
const message = ref('')
const isError = ref(false)
const activeStatus = ref<number | 'all'>('all')
const actingOrderId = ref<string | null>(null)

const FILTERS: { value: number | 'all'; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 0, label: '待支付' },
  { value: 1, label: '已支付' },
  { value: 2, label: '已发货' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '已关闭' },
  { value: 6, label: '退款中' },
  { value: 7, label: '退款完成' },
]

const filteredOrders = computed(() =>
  activeStatus.value === 'all' ? orders.value : orders.value.filter((order) => order.status === activeStatus.value),
)

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function loadOrders() {
  loading.value = true
  try {
    orders.value = await orderApi.list()
  } catch (error) {
    showMessage(readApiError(error, '订单加载失败'), true)
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)

async function withActing(orderId: string, action: () => Promise<unknown>) {
  if (actingOrderId.value) return
  actingOrderId.value = orderId
  try {
    await action()
    await loadOrders()
  } catch (error) {
    showMessage(readApiError(error, '操作失败'), true)
  } finally {
    actingOrderId.value = null
  }
}

function cancelOrder(order: Order) {
  if (!window.confirm(`确定取消订单 ${order.orderNo} 吗？`)) return
  void withActing(order.orderId, () => orderApi.cancel(order.orderId))
}

function confirmOrder(order: Order) {
  if (!window.confirm('确认已收到商品吗？')) return
  void withActing(order.orderId, () => orderApi.confirm(order.orderId))
}

async function goPay(order: Order) {
  try {
    const payment = await paymentApi.create(order.orderId)
    await router.push({ name: 'trade-payment', params: { paymentOrderId: String(payment.paymentOrderId) } })
  } catch (error) {
    showMessage(readApiError(error, '创建支付单失败'), true)
  }
}

function openDetail(order: Order) {
  void router.push({ name: 'trade-order-detail', params: { orderId: String(order.orderId) } })
}

function formatMoney(value: number) {
  return `¥${value.toFixed(2)}`
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <section class="page-stack">
    <div class="page-heading split-heading">
      <div>
        <p class="eyebrow">ORDERS</p>
        <h1>我的订单</h1>
        <p>查看订单状态，待支付订单可继续支付。</p>
      </div>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div class="filter-tabs">
      <button
        v-for="filter in FILTERS"
        :key="String(filter.value)"
        type="button"
        :class="['filter-tab', { active: activeStatus === filter.value }]"
        @click="activeStatus = filter.value"
      >
        {{ filter.label }}
      </button>
    </div>

    <div v-if="loading" class="loading-card">正在加载订单…</div>

    <div v-else-if="!filteredOrders.length" class="section-card empty-state">
      <span>📋</span>
      <h2>暂无订单</h2>
      <p>去购物车挑选商品，完成第一笔订单吧。</p>
      <router-link class="primary-button" to="/cart">去购物车</router-link>
    </div>

    <div v-else class="order-list">
      <article v-for="order in filteredOrders" :key="order.orderId" class="section-card order-card">
        <div class="order-head">
          <div class="order-meta">
            <span class="order-no">{{ order.orderNo }}</span>
            <span class="muted">{{ formatTime(order.payTime || order.closeTime || order.finishTime) }}</span>
          </div>
          <span :class="['status-badge', `status-${order.status}`]">{{ ORDER_STATUS_LABELS[order.status] }}</span>
        </div>

        <div class="order-goods" @click="openDetail(order)">
          <div v-for="item in order.items.slice(0, 3)" :key="item.itemId" class="order-thumb" :title="item.skuName || `SKU #${item.skuId}`">
            {{ item.skuImage || '📦' }}
          </div>
          <span v-if="order.items.length > 3" class="order-more">+{{ order.items.length - 3 }}</span>
          <div class="order-goods-info">
            <strong>{{ order.items[0]?.skuName || `SKU #${order.items[0]?.skuId}` }}</strong>
            <small v-if="order.items.length > 1" class="muted">等 {{ order.items.length }} 件商品</small>
          </div>
        </div>

        <div class="order-foot">
          <div class="order-amount">
            <span class="muted">实付</span>
            <strong>{{ formatMoney(order.payAmount) }}</strong>
          </div>
          <div class="order-actions">
            <button v-if="order.status === 0" class="secondary-button" type="button" :disabled="actingOrderId === order.orderId" @click="cancelOrder(order)">取消订单</button>
            <button v-if="order.status === 0" class="primary-button" type="button" :disabled="actingOrderId === order.orderId" @click="goPay(order)">去支付</button>
            <button v-if="order.status === 2" class="primary-button" type="button" :disabled="actingOrderId === order.orderId" @click="confirmOrder(order)">确认收货</button>
            <button class="text-button" type="button" @click="openDetail(order)">查看详情 →</button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.filter-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-tab {
  padding: 9px 18px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: white;
  color: #526059;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
}

.filter-tab.active {
  background: var(--green);
  border-color: var(--green);
  color: white;
}

.order-list {
  display: grid;
  gap: 18px;
}

.order-card {
  padding: 0;
  overflow: hidden;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 26px;
  border-bottom: 1px solid var(--line);
  background: #fafbf9;
}

.order-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-no {
  font-weight: 700;
  font-size: 14px;
}

.status-badge {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.status-0 { background: #fff3d6; color: #8a6a00; }
.status-1 { background: #e3f0ff; color: #1d5fa8; }
.status-2 { background: #e9f5ef; color: #15553f; }
.status-3 { background: #edf2ed; color: #526059; }
.status-4 { background: #f0f0f0; color: #8b948f; }
.status-5 { background: #f0f0f0; color: #8b948f; }
.status-6 { background: #f3e8ff; color: #6b3fa0; }
.status-7 { background: #e6f4ea; color: #1e7a46; }

.order-goods {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 26px;
  cursor: pointer;
}

.order-thumb {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #eef2ef;
  color: #7d8a83;
  font-size: 24px;
}

.order-more {
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}

.order-goods-info strong {
  display: block;
  margin-bottom: 4px;
}

.order-goods-info small {
  font-size: 13px;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 26px;
  border-top: 1px solid var(--line);
}

.order-amount {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.order-amount strong {
  font-size: 22px;
  color: var(--green-dark);
}

.order-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>