<script setup lang="ts">
import { ref } from 'vue'
import StaffAuditQueueView from './StaffAuditQueueView.vue'

const stage = ref<'PENDING_AUDIT' | 'ACTIVE' | 'REJECTED' | 'REVOKED'>('PENDING_AUDIT')
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">CUSTOMER SERVICE GOVERNANCE</p>
        <h1>客服审核</h1>
        <p>商家提交客服账号后，由平台审核通过才可登录。已通过账号可撤销，已驳回或撤销后可重新授予。</p>
      </div>
    </div>
    <el-tabs v-model="stage">
      <el-tab-pane label="待审核" name="PENDING_AUDIT" />
      <el-tab-pane label="已通过" name="ACTIVE" />
      <el-tab-pane label="已驳回" name="REJECTED" />
      <el-tab-pane label="已撤销" name="REVOKED" />
    </el-tabs>
    <StaffAuditQueueView
      :status="stage"
      :empty-text="stage === 'PENDING_AUDIT' ? '没有待审核客服' : stage === 'ACTIVE' ? '没有已通过客服' : stage === 'REJECTED' ? '没有已驳回客服' : '没有已撤销客服'"
    />
  </section>
</template>
