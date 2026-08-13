<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { readApiError } from '@/services/http'
import { userApi } from '@/services/user'
import type { SaveUserAddress, UserAddress } from '@/types/user'

const addresses = ref<UserAddress[]>([])
const loading = ref(true)
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref<string | null>(null)
const message = ref('')
const isError = ref(false)
const emptyForm = (): SaveUserAddress => ({
  recipientName: '', recipientPhone: '', provinceCode: '', provinceName: '', cityCode: '', cityName: '',
  districtCode: '', districtName: '', detailAddress: '', postalCode: '', isDefault: false,
})
const form = reactive<SaveUserAddress>(emptyForm())

async function loadAddresses() {
  loading.value = true
  try {
    addresses.value = await userApi.addresses()
  } catch (error) {
    showMessage(readApiError(error, '地址加载失败'), true)
  } finally {
    loading.value = false
  }
}

onMounted(loadAddresses)

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

function openEdit(address: UserAddress) {
  editingId.value = address.id
  Object.assign(form, {
    recipientName: address.recipientName, recipientPhone: address.recipientPhone,
    provinceCode: address.provinceCode || '', provinceName: address.provinceName,
    cityCode: address.cityCode || '', cityName: address.cityName,
    districtCode: address.districtCode || '', districtName: address.districtName,
    detailAddress: address.detailAddress, postalCode: address.postalCode || '', isDefault: address.isDefault,
  })
  modalOpen.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) await userApi.updateAddress(editingId.value, { ...form })
    else await userApi.createAddress({ ...form })
    modalOpen.value = false
    showMessage(editingId.value ? '地址已更新' : '地址已添加')
    await loadAddresses()
  } catch (error) {
    showMessage(readApiError(error, '地址保存失败'), true)
  } finally {
    saving.value = false
  }
}

async function setDefault(address: UserAddress) {
  try {
    await userApi.setDefaultAddress(address.id)
    showMessage('默认地址已更新')
    await loadAddresses()
  } catch (error) {
    showMessage(readApiError(error, '默认地址设置失败'), true)
  }
}

async function remove(address: UserAddress) {
  if (!window.confirm(`确定删除 ${address.recipientName} 的收货地址吗？`)) return
  try {
    await userApi.deleteAddress(address.id)
    showMessage('地址已删除')
    await loadAddresses()
  } catch (error) {
    showMessage(readApiError(error, '地址删除失败'), true)
  }
}
</script>

<template>
  <section class="page-stack">
    <div class="page-heading split-heading">
      <div><p class="eyebrow">DELIVERY</p><h1>收货地址</h1><p>地址数据仅能由当前登录用户查看和修改。</p></div>
      <button class="primary-button" type="button" @click="openCreate">＋ 添加地址</button>
    </div>
    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>
    <div v-if="loading" class="loading-card">正在加载收货地址…</div>
    <div v-else-if="!addresses.length" class="section-card empty-state"><span>⌂</span><h2>还没有收货地址</h2><p>添加第一个地址后，它会自动成为默认地址。</p><button class="primary-button" type="button" @click="openCreate">添加地址</button></div>
    <div v-else class="address-grid">
      <article v-for="address in addresses" :key="address.id" :class="['address-card', { default: address.isDefault }]">
        <div class="address-card-head"><div><strong>{{ address.recipientName }}</strong><span>{{ address.recipientPhone }}</span></div><span v-if="address.isDefault" class="default-badge">默认</span></div>
        <p>{{ address.provinceName }} {{ address.cityName }} {{ address.districtName }}</p>
        <p class="address-detail">{{ address.detailAddress }}</p>
        <p v-if="address.postalCode" class="muted">邮编：{{ address.postalCode }}</p>
        <div class="card-actions">
          <button v-if="!address.isDefault" class="text-button" type="button" @click="setDefault(address)">设为默认</button>
          <span v-else></span>
          <div><button class="text-button" type="button" @click="openEdit(address)">编辑</button><button class="text-button danger" type="button" @click="remove(address)">删除</button></div>
        </div>
      </article>
    </div>

    <div v-if="modalOpen" class="modal-backdrop" @mousedown.self="modalOpen = false">
      <form class="modal-card" @submit.prevent="save">
        <div class="modal-head"><div><p class="eyebrow">ADDRESS</p><h2>{{ editingId ? '编辑地址' : '添加地址' }}</h2></div><button class="icon-button" type="button" aria-label="关闭" @click="modalOpen = false">×</button></div>
        <div class="form-grid compact">
          <label>收货人<input v-model="form.recipientName" required maxlength="64" /></label>
          <label>联系电话<input v-model="form.recipientPhone" required maxlength="32" pattern="[0-9+() -]{6,32}" /></label>
          <label>省/直辖市<input v-model="form.provinceName" required maxlength="64" placeholder="例如：浙江省" /></label>
          <label>城市<input v-model="form.cityName" required maxlength="64" placeholder="例如：杭州市" /></label>
          <label>区/县<input v-model="form.districtName" required maxlength="64" placeholder="例如：西湖区" /></label>
          <label>邮政编码<input v-model="form.postalCode" maxlength="16" /></label>
          <label class="full">详细地址<textarea v-model="form.detailAddress" required maxlength="255" rows="3" placeholder="街道、门牌号、小区、楼栋等"></textarea></label>
          <label class="checkbox-line full"><input v-model="form.isDefault" type="checkbox" />设为默认收货地址</label>
        </div>
        <div class="form-actions"><button class="secondary-button" type="button" @click="modalOpen = false">取消</button><button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存地址' }}</button></div>
      </form>
    </div>
  </section>
</template>
