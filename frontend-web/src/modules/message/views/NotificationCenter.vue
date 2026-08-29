<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">NOTIFICATIONS</p>
        <h1>消息.通知</h1>
        <p class="subtle">查看系统、订单、营销和客服消息</p>
      </div>
      <div class="heading-actions">
        <span v-if="unreadCount > 0" class="unread-summary">{{ unreadCount }} 条未读</span>
        <button class="btn-ghost" :disabled="!unreadCount" @click="handleMarkAllRead">全部已读</button>
      </div>
    </div>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="tab"
        :class="{ active: activeTab === tab.value }"
        @click="handleTabChange(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count > 0" class="tab-badge">{{ tab.count }}</span>
      </button>
    </div>

    <div class="notification-list">
      <div
        v-for="item in filteredNotifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !isReadFlag(item.isRead) }"
        @click="handleMarkRead(item)"
      >
        <div class="notif-indicator" v-if="!isReadFlag(item.isRead)"></div>
        <div class="notif-content">
          <div class="notif-header">
            <span class="notif-category">{{ getCategoryLabel(item.category) }}</span>
            <span class="notif-time">{{ formatTime(item.createdAt) }}</span>
          </div>
          <div class="notif-title">{{ item.title }}</div>
          <div class="notif-body">{{ item.content }}</div>
        </div>
        <div v-if="!isReadFlag(item.isRead)" class="notif-actions">
          <button class="btn-link" @click.stop="handleMarkRead(item)">标记已读</button>
        </div>
      </div>
      <div v-if="!filteredNotifications.length && !loading" class="empty-state">
        <span>📭</span>
        <p>暂无{{ activeTab === -1 ? '' : getCategoryLabel(activeTab) }}通知</p>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { notificationApi } from '@/modules/message/services/message'
import {
  NotificationCategory,
  NOTIFICATION_CATEGORY_LABELS,
  getCategoryLabel,
  isReadFlag,
} from '@/modules/message/types'
import type { UserNotification } from '@/modules/message/types'
import { readApiError } from '@/services/http'

const notifications = ref<UserNotification[]>([])
const unreadCount = ref(0)
const loading = ref(false)
const activeTab = ref<-1 | number>(-1) // -1 = ALL

const tabs = computed(() => {
  const categories: number[] = [
    NotificationCategory.SYSTEM,
    NotificationCategory.ORDER,
    NotificationCategory.MARKETING,
    NotificationCategory.SERVICE,
  ]
  const result: { value: -1 | number; label: string; count: number }[] = [
    { value: -1, label: '全部', count: unreadCount.value },
  ]
  for (const cat of categories) {
    const count = notifications.value.filter((n) => n.category === cat && !isReadFlag(n.isRead)).length
    result.push({ value: cat, label: NOTIFICATION_CATEGORY_LABELS[cat] || '未知', count })
  }
  return result
})

const filteredNotifications = computed(() => {
  if (activeTab.value === -1) return notifications.value
  return notifications.value.filter((n) => n.category === activeTab.value)
})

function handleTabChange(value: -1 | number) {
  activeTab.value = value
  loadData()
}

function formatTime(iso: string) {
  const d = new Date(iso)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function loadData() {
  loading.value = true
  try {
    const category = activeTab.value === -1 ? undefined : activeTab.value
    const [list, count] = await Promise.all([
      notificationApi.list(category),
      notificationApi.getUnreadCount(),
    ])
    notifications.value = list
    unreadCount.value = count
  } catch (err) {
    console.error('加载通知失败:', readApiError(err))
  } finally {
    loading.value = false
  }
}

async function handleMarkRead(item: UserNotification) {
  if (isReadFlag(item.isRead)) return
  try {
    await notificationApi.markRead(item.id)
    item.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (err) {
    console.error('标记已读失败:', readApiError(err))
  }
}

async function handleMarkAllRead() {
  try {
    await notificationApi.markAllRead()
    notifications.value.forEach((n) => (n.isRead = 1))
    unreadCount.value = 0
  } catch (err) {
    console.error('全部已读失败:', readApiError(err))
  }
}

onMounted(loadData)
</script>

<style scoped>
.heading-actions { display: flex; align-items: center; gap: 12px; }
.unread-summary { font-size: 13px; color: #ff6b35; font-weight: 500; }
.btn-ghost { background: transparent; border: 1px solid var(--line, #e8e8e8); border-radius: 8px; padding: 6px 16px; cursor: pointer; font-size: 13px; }
.btn-ghost:hover { background: #f5f5f5; }
.btn-ghost:disabled { opacity: 0.5; cursor: not-allowed; }
.tabs { display: flex; gap: 4px; margin-bottom: 16px; border-bottom: 1px solid var(--line); padding-bottom: 0; }
.tab { display: flex; align-items: center; gap: 6px; padding: 10px 16px; background: transparent; border: none; border-bottom: 2px solid transparent; cursor: pointer; font-size: 14px; color: #666; }
.tab.active { color: var(--green, #00843d); border-bottom-color: var(--green); font-weight: 600; }
.tab-badge { background: #ff6b35; color: #fff; border-radius: 10px; padding: 0 6px; font-size: 11px; min-width: 18px; text-align: center; }
.notification-list { display: flex; flex-direction: column; gap: 1px; }
.notification-item { display: flex; gap: 12px; padding: 16px; background: var(--paper, #fff); border-radius: 8px; cursor: pointer; transition: background 0.15s; position: relative; }
.notification-item:hover { background: #f9f9f9; }
.notification-item.unread { background: #f0f9f4; }
.notif-indicator { position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--green); border-radius: 8px 0 0 8px; }
.notif-content { flex: 1; }
.notif-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.notif-category { font-size: 12px; color: var(--green); background: #e6f4ea; padding: 2px 8px; border-radius: 4px; }
.notif-time { font-size: 12px; color: #999; }
.notif-title { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.notif-body { font-size: 13px; color: #666; line-height: 1.5; }
.notif-actions { display: flex; align-items: flex-start; }
.btn-link { background: transparent; border: none; color: var(--green); cursor: pointer; font-size: 13px; padding: 4px 8px; }
.empty-state { text-align: center; padding: 60px 20px; color: #999; }
.empty-state span { font-size: 48px; display: block; margin-bottom: 12px; }
.loading { text-align: center; padding: 40px; color: #999; }
</style>