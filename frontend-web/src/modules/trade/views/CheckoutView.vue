<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { readApiError } from '@/services/http'
import { userApi } from '@/services/user'
import type { UserAddress } from '@/types/user'
import { cartApi, orderApi, paymentApi } from '../services/trade'
import type { CartItem } from '../types'

const router = useRouter()
const checkedItems = ref<CartItem[]>([])
const addresses = ref<UserAddress[]>([])
const selectedAddressId = ref<string | null>(null)
const remark = ref('')
const loading = ref(true)
const submitting = ref(false)
const message = ref('')
const isError = ref(false)

const selectedAddress = computed(() => addresses.value.find((address) => address.id === selectedAddressId.value) ?? null)
const totalAmount = computed(() => checkedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0))

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

onMounted(async () => {
  try {
    const [cart, addressList] = await Promise.all([cartApi.get(), userApi.addresses()])
    checkedItems.value = cart.groups.flatMap((group) => group.items).filter((item) => item.checked === 1)
    addresses.value = addressList
    selectedAddressId.value = addressList.find((address) => address.isDefault)?.id ?? addressList[0]?.id ?? null
  } catch (error) {
    showMessage(readApiError(error, '结算信息加载失败'), true)
  } finally {
    loading.value = false
  }
})

async function submit() {
  if (!selectedAddress.value) {
    showMessage('请先选择收货地址', true)
    return
  }
  if (!checkedItems.value.length) {
    showMessage('没有可结算的商品，请返回购物车勾选', true)
    return
  }
  submitting.value = true
  try {
    const address = selectedAddress.value
    const orders = await orderApi.create({
      receiverName: address.recipientName,
      receiverPhone: address.recipientPhone,
      receiverAddress: `${address.provinceName} ${address.cityName} ${address.districtName} ${address.detailAddress}`.trim(),
      remark: remark.value.trim() || undefined,
    })
    const payment = await paymentApi.create(orders[0].orderId)
    await router.push({ name: 'trade-payment', params: { paymentOrderId: String(payment.paymentOrderId) } })
  } catch (error) {
    showMessage(readApiError(error, '下单失败'), true)
  } finally {
    submitting.value = false
  }
}

function formatMoney(value: number) {
  return `¥${value.toFixed(2)}`
}
</script>

<template>
  <section class="page-stack narrow-page">
    <div class="page-heading split-heading">
      <div>
        <p class="eyebrow">CHECKOUT</p>
        <h1>确认订单</h1>
        <p>核对商品与收货信息后提交订单。</p>
      </div>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div v-if="loading" class="loading-card">正在加载结算信息…</div>

    <template v-else-if="checkedItems.length">
      <div class="section-card">
        <p class="eyebrow">DELIVERY</p>
        <h2 class="card-title">收货地址</h2>
        <div v-if="!addresses.length" class="empty-inline">
          <p class="muted">还没有收货地址，请先到「收货地址」页面添加。</p>
          <router-link class="text-button" to="/addresses">去添加地址 →</router-link>
        </div>
        <div v-else class="address-pick-grid">
          <button
            v-for="address in addresses"
            :key="address.id"
            type="button"
            :class="['address-pick', { active: address.id === selectedAddressId }]"
            @click="selectedAddressId = address.id"
          >
            <span class="address-pick-head">
              <strong>{{ address.recipientName }}</strong>
              <span class="muted">{{ address.recipientPhone }}</span>
              <span v-if="address.isDefault" class="default-badge">默认</span>
            </span>
            <span class="address-pick-detail">{{ address.provinceName }} {{ address.cityName }} {{ address.districtName }} {{ address.detailAddress }}</span>
          </button>
        </div>
      </div>

      <div class="section-card">
        <p class="eyebrow">GOODS</p>
        <h2 class="card-title">商品清单</h2>
        <div class="checkout-items">
          <div v-for="item in checkedItems" :key="item.itemId" class="checkout-item">
            <div class="cart-thumb">{{ item.skuImage || '📦' }}</div>
            <div class="checkout-info">
              <strong>{{ item.skuName || `SKU #${item.skuId}` }}</strong>
              <small class="muted">{{ formatMoney(item.price) }} × {{ item.quantity }}</small>
            </div>
            <strong>{{ formatMoney(item.price * item.quantity) }}</strong>
          </div>
        </div>
      </div>

      <div class="section-card">
        <p class="eyebrow">REMARK</p>
        <h2 class="card-title">订单备注</h2>
        <input v-model="remark" class="remark-input" maxlength="255" placeholder="选填，例如：请尽快发货" />
      </div>

      <div class="section-card checkout-summary">
        <div class="checkout-total">
          <span class="muted">应付金额</span>
          <strong>{{ formatMoney(totalAmount) }}</strong>
        </div>
        <button class="primary-button wide" type="button" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '提交订单' }}
        </button>
      </div>
    </template>

    <div v-else class="section-card empty-state">
      <span>🛒</span>
      <h2>没有可结算的商品</h2>
      <p>请返回购物车勾选要结算的商品。</p>
      <router-link class="primary-button" to="/cart">返回购物车</router-link>
    </div>
  </section>
</template>

<style scoped>
.card-title {
  margin: 0 0 18px;
}

.address-pick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.address-pick {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: white;
  text-align: left;
  cursor: pointer;
  transition: border-color .15s, box-shadow .15s;
}

.address-pick.active {
  border-color: var(--green);
  box-shadow: inset 4px 0 var(--green);
}

.address-pick-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.address-pick-detail {
  display: block;
  color: #46534c;
  font-size: 14px;
  line-height: 1.6;
}

.empty-inline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.checkout-items {
  display: grid;
  gap: 14px;
}

.checkout-item {
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

.checkout-info strong {
  display: block;
  margin-bottom: 4px;
}

.checkout-info small {
  font-size: 13px;
}

.remark-input {
  width: 100%;
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  font-size: 14px;
}

.checkout-summary {
  display: grid;
  gap: 18px;
}

.checkout-total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 14px;
}

.checkout-total strong {
  font-size: 30px;
  color: var(--green-dark);
}
</style>