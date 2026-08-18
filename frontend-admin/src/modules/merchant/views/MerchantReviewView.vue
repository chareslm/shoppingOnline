<script setup lang="ts">
import { ref } from 'vue'
import MerchantReviewQueueView from './MerchantReviewQueueView.vue'

const stage = ref<'pending' | 'approved' | 'revoked'>('pending')
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">MERCHANT ONBOARDING</p>
        <h1>商家审核</h1>
        <p>资质审核通过后即开通商家账号与店铺。已通过账号可撤销；已撤销账号可重新授予。</p>
      </div>
    </div>
    <el-tabs v-model="stage" class="merchant-review-tabs">
      <el-tab-pane label="待审核" name="pending" />
      <el-tab-pane label="已通过" name="approved" />
      <el-tab-pane label="已撤销" name="revoked" />
    </el-tabs>
    <MerchantReviewQueueView :stage="stage" embedded />
  </section>
</template>
