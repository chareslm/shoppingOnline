<script setup lang="ts">
import ProductQueueView from './ProductQueueView.vue'
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">PRODUCT GOVERNANCE</p>
        <h1>全部商品</h1>
        <p>查看全平台商品。可对已上架商品收回审核，或对已收回商品重新通过。</p>
      </div>
    </div>
    <ProductQueueView status="" empty-text="暂无商品" />
  </section>
</template>
