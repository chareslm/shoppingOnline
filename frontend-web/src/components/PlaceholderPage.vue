<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
    </div>
    <div class="section-card empty-state">
      <span>◇</span>
      <h2>功能建设中</h2>
      <p>该页面已预留入口，待对应模块接口就绪后接入真实数据。</p>
    </div>
  </section>
</template>
