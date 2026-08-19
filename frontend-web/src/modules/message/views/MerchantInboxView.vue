<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { isCustomerServiceOnly } from '@/types/auth'

const auth = useAuthStore()
const forCustomerService = computed(() => isCustomerServiceOnly(auth.session?.roles ?? []))
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">MERCHANT MESSAGE</p>
        <h1>用户沟通</h1>
        <p v-if="forCustomerService">客服账号只能使用本页。会话接入完成后，将在此接待用户咨询。</p>
        <p v-else>与用户的客服会话、转接和处理记录将在此接入。</p>
      </div>
    </div>
    <div class="section-card empty-state">
      <span>💬</span>
      <h2>暂无待处理会话</h2>
      <p>{{ forCustomerService ? '当前没有用户咨询。请保持在线，新会话出现后会显示在这里。' : '客服聊天能力接入后，店铺会话将显示在这里。' }}</p>
    </div>
  </section>
</template>
