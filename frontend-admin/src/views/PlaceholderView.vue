<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
    </div>
    <el-card shadow="never">
      <el-empty description="功能待接入，当前不展示模拟数据" />
    </el-card>
  </section>
</template>
