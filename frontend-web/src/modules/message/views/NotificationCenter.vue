<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { readApiError } from '@/services/http'
import { notificationApi } from '../services/message'
import { NOTIFICATION_CATEGORIES, NOTIFICATION_CATEGORY_LABELS, type NotificationItem } from '../types'

const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)
const loading = ref(true)
const message = ref('')
const isError = ref(false)
const activeCategory = ref<number | 'all'>('all')
const actingId = ref<string | null>(null)

function showMessage(text: string, error = false) {
  message.value = text
  isError.value = error
}

const filteredNotifications = computed(() =>
  activeCategory.value === 'all'
    ? notifications.value
    : notifications.value.filter((n) => n.category === activeCategory.value),
)

async function loadUnreadCount() {
  try {
    unreadCount.value = await notificationApi.getUnreadCount()
  } catch {
    // ignore
  }
}

async function loadNotifications() {
  loading.value = true
  message.value = ''
  try {
    notifications.value = await notificationApi.list(
      activeCategory.value === 'all' ? undefined : activeCategory.value,
    )
    await loadUnreadCount()
  } catch (error) {
    showMessage(readApiError(error, '通知加载失败'), true)
  } finally {
    loading.value = false
  }
}

function changeCategory(cat: number | 'all') {
  activeCategory.value = cat
  void loadNotifications()
}

async function markAsRead(id: string) {
  if (actingId.value) return
  actingId.value = id
  try {
    await notificationApi.markRead(id)
    const n = notifications.value.find((x) => x.id === id)
    if (n) n.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (error) {
    showMessage(readApiError(error, '标记已读失败'), true)
  } finally {
    actingId.value = null
  }
}

async function markAllRead() {
  if (!unreadCount.value) return
  try {
    await notificationApi.markAllRead()
    for (const n of notifications.value) n.isRead = 1
    unreadCount.value = 0
    showMessage('已全部标记为已读')
  } catch (error) {
    showMessage(readApiError(error, '操作失败'), true)
  }
}

function formatTime(value: string | null | undefined) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

const unreadCategories = computed(() => {
  const map = new Map<number, number>()
  for (const n of notifications.value) {
    if (n.isRead === 0) {
      map.set(n.category, (map.get(n.category) ?? 0) + 1)
    }
  }
  return map
})

onMounted(async () => {
  await Promise.all([loadNotifications(), loadUnreadCount()])
})
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">NOTIFICATIONS</p>
        <h1>消息通知</h1>
        <p>查看系统通知、订单动态和客服消息。</p>
      </div>
      <div class="page-actions">
        <span v-if="unreadCount > 0" class="unread-summary">{{ unreadCount }} 条未读</span>
        <button class="secondary-button" type="button" :disabled="!unreadCount" @click="markAllRead">全部已读</button>
        <router-link class="text-button" to="/notifications/preference">通知偏好 →</router-link>
      </div>
    </div>

    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>

    <div class="filter-tabs">
      <button
        v-for="cat in NOTIFICATION_CATEGORIES"
        :key="String(cat.value)"
        type="button"
        :class="['filter-tab', { active: activeCategory === cat.value }]"
        @click="changeCategory(cat.value)"
      >
        {{ cat.label }}
        <span v-if="cat.value !== 'all' && (unreadCategories.get(cat.value as number) ?? 0) > 0" class="tab-badge">
          {{ unreadCategories.get(cat.value as number) }}
        </span>
      </button>
    </div>

    <div v-if="loading" class="loading-card">加载通知…</div>

    <div v-else-if="!filteredNotifications.length" class="section-card empty-state">
      <span>📭</span>
      <h2>暂无通知</h2>
      <p>目前没有{{ activeCategory === 'all' ? '' : NOTIFICATION_CATEGORY_LABELS[activeCategory as number] }}通知。</p>
    </div>

    <div v-else class="notification-list">
      <article
        v-for="n in filteredNotifications"
        :key="n.id"
        :class="['section-card notification-item', { unread: n.isRead === 0 }]"
        @click="n.isRead === 0 && markAsRead(n.id)"
      >
        <div class="notif-icon" :data-category="n.category">
          {{ n.category === 1 ? '⚙️' : n.category === 2 ? '📦' : n.category === 3 ? '🎁' : '💬' }}
        </div>
        <div class="notif-body">
          <div class="notif-head">
            <strong>{{ n.title }}</strong>
            <span :class="['status-dot', { unread: n.isRead === 0 }]"></span>
          </div>
          <p class="notif-content">{{ n.content }}</p>
          <div class="notif-foot">
            <span class="muted">{{ n.categoryDesc }}</span>
            <span class="muted">{{ formatTime(n.createdAt) }}</span>
            <button
              v-if="n.isRead === 0"
              class="text-button small"
              type="button"
              :disabled="actingId === n.id"
              @click.stop="markAsRead(n.id)"
            >标记已读</button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.unread-summary {
  font-weight: 700;
  color: var(--green-dark);
}

.filter-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: white;
  color: #526059;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
}

.filter-tab.active {
  background: var(--green);
  border-color: var(--green);
  color: white;
}

.tab-badge {
  background: #e03131;
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 999px;
}

.notification-list {
  display: grid;
  gap: 12px;
}

.notification-item {
  display: flex;
  gap: 16px;
  padding: 18px 22px;
  cursor: pointer;
  transition: background .15s;
}

.notification-item:hover {
  background: #f6f8f6;
}

.notification-item.unread {
  background: #f0f7f2;
  border-color: #b2d3c2;
}

.notif-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  font-size: 22px;
  flex-shrink: 0;
}

.notif-icon[data-category="1"] { background: #e7f5ff; }
.notif-icon[data-category="2"] { background: #fff3e6; }
.notif-icon[data-category="3"] { background: #fdf0f7; }
.notif-icon[data-category="4"] { background: #e9f5ef; }

.notif-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.notif-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: transparent;
}

.status-dot.unread {
  background: #e03131;
}

.notif-content {
  margin: 0;
  color: #495057;
  line-height: 1.55;
  font-size: 14px;
}

.notif-foot {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
}
</style>
