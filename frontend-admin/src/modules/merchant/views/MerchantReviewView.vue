<script setup lang="ts">
import { ref } from 'vue'
import MerchantReviewQueueView from './MerchantReviewQueueView.vue'

const stage = ref<'qualification' | 'account'>('qualification')
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">MERCHANT ONBOARDING</p>
        <h1>商家审核</h1>
        <p>统合资质核验与账号开通。先完成资质审核，通过后再处理账号与店铺开通。</p>
      </div>
    </div>
    <el-tabs v-model="stage" class="merchant-review-tabs">
      <el-tab-pane label="资质审核" name="qualification" />
      <el-tab-pane label="账号审核" name="account" />
    </el-tabs>
    <MerchantReviewQueueView :stage="stage" embedded />
  </section>
</template>
