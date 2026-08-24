<script setup lang="ts">
import { ref } from 'vue'
import ProductQueueView from './ProductQueueView.vue'

const stage = ref<'PENDING_AUDIT' | 'ON_SALE' | 'AUDIT_REJECTED'>('PENDING_AUDIT')
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">PRODUCT GOVERNANCE</p>
        <h1>商品审核</h1>
        <p>商家提交后在此审核。通过即上架；已上架可收回；收回后可重新通过。</p>
      </div>
    </div>
    <el-tabs v-model="stage" class="merchant-review-tabs">
      <el-tab-pane label="待审核" name="PENDING_AUDIT" />
      <el-tab-pane label="已上架" name="ON_SALE" />
      <el-tab-pane label="已收回/驳回" name="AUDIT_REJECTED" />
    </el-tabs>
    <ProductQueueView :status="stage" :empty-text="stage === 'PENDING_AUDIT' ? '没有待审核商品' : stage === 'ON_SALE' ? '没有已上架商品' : '没有已收回或驳回的商品'" />
  </section>
</template>
