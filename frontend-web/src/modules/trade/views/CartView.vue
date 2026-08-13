<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { readApiError } from '@/services/http'
import { cartApi } from '../services/trade'
import type { Cart, CartGroup, CartItem } from '../types'

const router = useRouter()
const cart = ref<Cart | null>(null)
const loading = ref(true)
const message = ref('')
const isError = ref(false)
const updatingIds = ref<Set<string>>(new Set())

const allItems = computed<CartItem[]>(() => cart.value?.groups.flatMap((group) => group.items) ?? [])
const checkedItems = computed<CartItem[]>(() => allItems.value.filter((item) => item.checked === 1))
const allChecked = computed(() => allItems.value.length > 0 && checkedItems.value.length === allItems.value.length)
const selectedTotal = computed(() => checkedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0))

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

async function loadCart() {
  loading.value = true
  try {
    cart.value = await cartApi.get()
  } catch (error) {
    showMessage(readApiError(error, '购物车加载失败'), true)
  } finally {
    loading.value = false
  }
}

onMounted(loadCart)

async function withLock(itemId: string, action: () => Promise<unknown>) {
  if (updatingIds.value.has(itemId)) return
  updatingIds.value.add(itemId)
  try {
    await action()
    await loadCart()
  } catch (error) {
    showMessage(readApiError(error, '操作失败'), true)
  } finally {
    updatingIds.value.delete(itemId)
  }
}

function changeQuantity(item: CartItem, delta: number) {
  const next = item.quantity + delta
  if (next < 1) return
  void withLock(item.itemId, () => cartApi.updateQuantity(item.itemId, { quantity: next }))
}

function toggleItem(item: CartItem) {
  void withLock(item.itemId, () => cartApi.updateChecked(item.itemId, { checked: item.checked !== 1 }))
}

function toggleAll() {
  const target = !allChecked.value
  void Promise.all(allItems.value.map((item) => cartApi.updateChecked(item.itemId, { checked: target })))
    .then(loadCart)
    .catch((error) => showMessage(readApiError(error, '操作失败'), true))
}

async function removeItem(item: CartItem) {
  if (!window.confirm('确定从购物车移除该商品吗？')) return
  void withLock(item.itemId, () => cartApi.removeItem(item.itemId))
}

function goCheckout() {
  if (!checkedItems.value.length) {
    showMessage('请先勾选要结算的商品', true)
    return
  }
  void router.push({ name: 'trade-checkout' })
}

function formatMoney(value: number) {
  return `¥${value.toFixed(2)}`
}

function groupTitle(group: CartGroup) {
  return group.shopName ?? `店铺 #${group.shopId}`
}
</script>

<template>
  <section class="page-stack">
    <div class="page-heading split-heading">
      <div>
        <p class="eyebrow">CART</p>
        <h1>购物车</h1>
        <p>勾选要结算的商品，结算时按店铺拆分订单。</p>
      </div>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div v-if="loading" class="loading-card">正在加载购物车…</div>

    <div v-else-if="!allItems.length" class="section-card empty-state">
      <span>🛒</span>
      <h2>购物车是空的</h2>
      <p>商品模块接入后，可从商品页将心仪的商品加入购物车。</p>
    </div>

    <template v-else>
      <div v-for="group in cart!.groups" :key="group.groupId" class="section-card cart-group">
        <div class="cart-group-head">
          <span class="cart-shop-mark">S</span>
          <strong>{{ groupTitle(group) }}</strong>
        </div>
        <div class="cart-items">
          <div v-for="item in group.items" :key="item.itemId" class="cart-item">
            <input
              class="cart-check"
              type="checkbox"
              :checked="item.checked === 1"
              :disabled="updatingIds.has(item.itemId)"
              @change="toggleItem(item)"
            />
            <div class="cart-thumb">{{ item.skuImage || '📦' }}</div>
            <div class="cart-info">
              <strong>{{ item.skuName || `SKU #${item.skuId}` }}</strong>
              <small class="muted">{{ formatMoney(item.price) }} / 件</small>
            </div>
            <div class="cart-qty">
              <button type="button" class="qty-button" :disabled="item.quantity <= 1 || updatingIds.has(item.itemId)" @click="changeQuantity(item, -1)">−</button>
              <span>{{ item.quantity }}</span>
              <button type="button" class="qty-button" :disabled="updatingIds.has(item.itemId)" @click="changeQuantity(item, 1)">＋</button>
            </div>
            <div class="cart-subtotal"><strong>{{ formatMoney(item.price * item.quantity) }}</strong></div>
            <button class="text-button danger" type="button" :disabled="updatingIds.has(item.itemId)" @click="removeItem(item)">删除</button>
          </div>
        </div>
      </div>

      <div class="section-card cart-summary">
        <label class="checkbox-line">
          <input type="checkbox" :checked="allChecked" @change="toggleAll" />
          全选（{{ checkedItems.length }}/{{ allItems.length }}）
        </label>
        <div class="cart-total">
          <span class="muted">已选合计</span>
          <strong>{{ formatMoney(selectedTotal) }}</strong>
          <button class="primary-button" type="button" :disabled="!checkedItems.length" @click="goCheckout">去结算</button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.cart-group {
  padding: 0;
  overflow: hidden;
}

.cart-group-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 26px;
  border-bottom: 1px solid var(--line);
  background: #fafbf9;
}

.cart-shop-mark {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  background: var(--lime);
  color: var(--green-dark);
  font-weight: 800;
}

.cart-items {
  padding: 0 26px;
}

.cart-item {
  display: grid;
  grid-template-columns: auto 58px 1fr auto auto auto;
  align-items: center;
  gap: 18px;
  padding: 20px 0;
  border-bottom: 1px solid var(--line);
}

.cart-item:last-child {
  border-bottom: 0;
}

.cart-check {
  width: 18px;
  height: 18px;
  accent-color: var(--green);
}

.cart-thumb {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #eef2ef;
  color: #7d8a83;
  font-size: 26px;
}

.cart-info strong {
  display: block;
  margin-bottom: 6px;
}

.cart-info small {
  font-size: 13px;
}

.cart-qty {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qty-button {
  width: 30px;
  height: 30px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: white;
  font-size: 16px;
  line-height: 1;
}

.qty-button:disabled {
  opacity: .45;
}

.cart-subtotal {
  min-width: 88px;
  text-align: right;
}

.cart-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.cart-total {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cart-total strong {
  font-size: 26px;
  color: var(--green-dark);
}
</style>
