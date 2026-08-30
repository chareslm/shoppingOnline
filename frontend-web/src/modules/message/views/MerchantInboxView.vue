<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">CS WORKBENCH</p>
        <h1>客服工作台</h1>
        <p class="subtle">处理买家咨询会话，实时回复客户消息</p>
      </div>
      <div class="workbench-actions">
        <span class="connection-status" :class="{ connected: wsConnected }">
          <span class="dot"></span>
          {{ wsConnected ? '实时连接' : '连接断开' }}
        </span>
        <button class="btn-ghost" @click="loadSessions">刷新</button>
      </div>
    </div>

    <div class="chat-layout">
      <!-- 会话列表 -->
      <aside class="session-sidebar">
        <div class="sidebar-header">
          <h3>待处理会话</h3>
          <div class="header-actions">
            <span class="badge">{{ sessions.length }}</span>
          </div>
        </div>
        <div class="filter-tabs">
          <button :class="{ active: filter === 'all' }" @click="filter = 'all'">全部</button>
          <button :class="{ active: filter === 'mine' }" @click="filter = 'mine'">我的</button>
          <button :class="{ active: filter === 'unassigned' }" @click="filter = 'unassigned'">未分配</button>
        </div>
        <div class="session-list">
          <div
            v-for="s in filteredSessions"
            :key="s.sessionId"
            class="session-item"
            :class="{ active: selectedSession?.sessionId === s.sessionId }"
            @click="selectSession(s)"
          >
            <div class="session-info">
              <div class="session-title">{{ s.subject || '买家咨询' }}</div>
              <div class="session-preview">{{ s.lastMessage || '暂无消息' }}</div>
            </div>
            <div class="session-meta">
              <span v-if="s.csUserId == null" class="unassigned-tag">待分配</span>
              <span v-else-if="isMySession(s)" class="mine-tag">我的</span>
              <span v-if="s.unreadCount > 0" class="unread-dot">{{ s.unreadCount }}</span>
              <span class="session-time">{{ formatTime(s.lastMessageTime) }}</span>
            </div>
          </div>
          <div v-if="!filteredSessions.length && !loading" class="empty-sessions">
            <span>🎉</span>
            <p>暂无{{ filter === 'unassigned' ? '待分配' : '' }}会话</p>
          </div>
          <div v-if="loading" class="loading">加载中...</div>
        </div>
      </aside>

      <!-- 消息面板 -->
      <main class="chat-main">
        <template v-if="selectedSession">
          <div class="chat-header">
            <div class="chat-title-area">
              <h3>{{ selectedSession.subject || '买家咨询' }}</h3>
              <div class="chat-sub-info">
                <span class="shop-tag">店铺 #{{ selectedSession.shopId }}</span>
                <span v-if="selectedSession.csUserId == null" class="unassigned-tag">待分配</span>
                <span v-else-if="isMySession(selectedSession)" class="mine-tag">我处理中</span>
              </div>
            </div>
            <div class="chat-header-actions">
              <button
                v-if="selectedSession.csUserId == null"
                class="btn-primary"
                @click="handleClaim"
                :disabled="claiming"
              >{{ claiming ? '领取中...' : '领取会话' }}</button>
              <button class="btn-ghost" @click="handleClose" :disabled="closing">
                {{ closing ? '关闭中...' : '结束会话' }}
              </button>
            </div>
          </div>

          <div class="message-list" ref="messageListRef">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ self: isCsSender(msg.senderType), system: isSystemSender(msg.senderType) }"
            >
              <div v-if="isSystemSender(msg.senderType)" class="system-msg">{{ msg.content }}</div>
              <template v-else>
                <div class="bubble">{{ msg.content }}</div>
                <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
              </template>
            </div>
            <div v-if="loadingMessages" class="loading">加载中...</div>
            <div v-if="!messages.length && !loadingMessages" class="empty-messages">暂无消息</div>
          </div>

          <div class="chat-input">
            <textarea
              v-model="inputText"
              placeholder="输入回复，Enter 发送，Shift+Enter 换行"
              @keydown.enter.exact.prevent="sendMessage"
              rows="1"
              :disabled="!canReply"
            />
            <button
              class="btn-send"
              :disabled="!inputText.trim() || !canReply"
              @click="sendMessage"
            >发送</button>
          </div>
        </template>
        <div v-else class="no-selection">
          <span>💬</span>
          <p>选择左侧会话开始处理</p>
        </div>
      </main>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { chatSessionApi, chatMessageApi } from '@/modules/message/services/message'
import { useGlobalChatWebSocket } from '@/modules/message/composables/useChatWebSocket'
import { useAuthStore } from '@/stores/auth'
import { isCsSender, isSystemSender, MessageType } from '@/modules/message/types'
import type { ChatSession, ChatMessage } from '@/modules/message/types'
import { readApiError } from '@/services/http'

const authStore = useAuthStore()
const currentUserId = computed(() => authStore.session?.userId || '')

const sessions = ref<ChatSession[]>([])
const selectedSession = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const loading = ref(false)
const loadingMessages = ref(false)
const claiming = ref(false)
const closing = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const filter = ref<'all' | 'mine' | 'unassigned'>('all')

let pollingTimer: ReturnType<typeof setInterval> | null = null
let wsUnsubscribe: (() => void) | null = null

// WebSocket
const ws = useGlobalChatWebSocket()
const wsConnected = ws.connected

wsUnsubscribe = ws.onMessage((data) => {
  // 后端直接推送 MessageResponse 对象
  const msg = data as ChatMessage
  if (msg.sessionId) {
    if (selectedSession.value && msg.sessionId === selectedSession.value.sessionId) {
      messages.value.push(msg)
      scrollToBottom()
    }
    const session = sessions.value.find((s) => s.sessionId === msg.sessionId)
    if (session) {
      session.lastMessage = msg.content
      session.lastMessageTime = msg.createdAt
      if (!isCsSender(msg.senderType)) {
        session.unreadCount++
      }
    } else {
      loadSessions()
    }
  }
})

const filteredSessions = computed(() => {
  if (filter.value === 'all') return sessions.value
  if (filter.value === 'mine') return sessions.value.filter((s) => isMySession(s))
  if (filter.value === 'unassigned') return sessions.value.filter((s) => s.csUserId == null)
  return sessions.value
})

function isMySession(s: ChatSession) {
  return s.csUserId === currentUserId.value
}

const canReply = computed(() => {
  if (!selectedSession.value) return false
  if (selectedSession.value.csUserId == null || selectedSession.value.csUserId === currentUserId.value) return true
  return false
})

async function loadSessions() {
  loading.value = true
  try {
    sessions.value = await chatSessionApi.listCs()
  } catch (err) {
    console.error('加载会话列表失败:', readApiError(err))
  } finally {
    loading.value = false
  }
}

async function selectSession(session: ChatSession) {
  selectedSession.value = session
  messages.value = []
  loadingMessages.value = true
  try {
    messages.value = await chatMessageApi.list(session.sessionId)
    scrollToBottom()
  } catch (err) {
    console.error('加载消息失败:', readApiError(err))
  } finally {
    loadingMessages.value = false
  }
  if (session.unreadCount > 0) {
    session.unreadCount = 0
  }
}

async function handleClaim() {
  if (!selectedSession.value) return
  claiming.value = true
  try {
    const updated = await chatSessionApi.assign(selectedSession.value.sessionId)
    selectedSession.value = updated
    const idx = sessions.value.findIndex((s) => s.sessionId === updated.sessionId)
    if (idx >= 0) sessions.value[idx] = updated
  } catch (err) {
    alert('领取会话失败：' + readApiError(err))
  } finally {
    claiming.value = false
  }
}

async function handleClose() {
  if (!selectedSession.value) return
  if (!confirm('确定要结束此会话吗？')) return
  closing.value = true
  try {
    await chatSessionApi.close(selectedSession.value.sessionId)
    selectedSession.value = null
    messages.value = []
    loadSessions()
  } catch (err) {
    alert('关闭会话失败：' + readApiError(err))
  } finally {
    closing.value = false
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !selectedSession.value) return
  // 如果会话未分配给我，先尝试领取
  if (selectedSession.value.csUserId == null) {
    await handleClaim()
    if (!selectedSession.value || selectedSession.value.csUserId == null) return
  }
  try {
    const msg = await chatMessageApi.send(selectedSession.value.sessionId, text, MessageType.TEXT)
    messages.value.push(msg)
    selectedSession.value.lastMessage = text
    selectedSession.value.lastMessageTime = msg.createdAt
    inputText.value = ''
    scrollToBottom()
  } catch (err) {
    alert('发送失败：' + readApiError(err))
  }
}

function formatTime(iso?: string | null) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 轮询
function startPolling() {
  pollingTimer = setInterval(async () => {
    try {
      await loadSessions()
    } catch { /* 静默 */ }

    if (selectedSession.value) {
      try {
        const latest = await chatMessageApi.list(selectedSession.value.sessionId)
        const existingIds = new Set(messages.value.map((m) => m.id))
        for (const msg of latest) {
          if (!existingIds.has(msg.id)) {
            messages.value.push(msg)
          }
        }
        if (messages.value.length !== latest.length) {
          messages.value = latest
          scrollToBottom()
        }
      } catch { /* 静默 */ }
    }
  }, 15000)
}

onMounted(async () => {
  await loadSessions()
  ws.connect()
  startPolling()
})

onUnmounted(() => {
  if (pollingTimer) clearInterval(pollingTimer)
  if (wsUnsubscribe) wsUnsubscribe()
})
</script>

<style scoped>
.workbench-actions { display: flex; align-items: center; gap: 12px; }
.connection-status { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #999; }
.connection-status .dot { width: 8px; height: 8px; border-radius: 50%; background: #ccc; }
.connection-status.connected { color: var(--green, #00843d); }
.connection-status.connected .dot { background: var(--green); }
.btn-ghost { background: transparent; border: 1px solid var(--line); border-radius: 8px; padding: 6px 16px; cursor: pointer; font-size: 13px; }
.btn-ghost:hover { background: #f5f5f5; }
.btn-ghost:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: var(--green); color: #fff; border: none; border-radius: 8px; padding: 6px 16px; cursor: pointer; font-weight: 500; font-size: 13px; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-layout { display: flex; gap: 16px; height: calc(100vh - 180px); min-height: 500px; }
.session-sidebar { width: 300px; flex-shrink: 0; background: var(--paper, #fff); border: 1px solid var(--line); border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.sidebar-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--line); }
.sidebar-header h3 { font-size: 14px; margin: 0; }
.badge { background: var(--green); color: #fff; border-radius: 10px; padding: 2px 8px; font-size: 12px; }
.filter-tabs { display: flex; gap: 0; border-bottom: 1px solid var(--line); }
.filter-tabs button { flex: 1; background: transparent; border: none; padding: 10px; cursor: pointer; font-size: 13px; color: #666; border-bottom: 2px solid transparent; }
.filter-tabs button.active { color: var(--green); border-bottom-color: var(--green); font-weight: 600; }
.session-list { flex: 1; overflow-y: auto; }
.session-item { padding: 12px 16px; border-bottom: 1px solid var(--line); cursor: pointer; transition: background 0.15s; }
.session-item:hover { background: #f5f5f5; }
.session-item.active { background: #e6f4ea; border-left: 3px solid var(--green); }
.session-info { display: flex; flex-direction: column; gap: 4px; }
.session-title { font-weight: 600; font-size: 14px; }
.session-preview { font-size: 12px; color: #888; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; gap: 6px; }
.unassigned-tag { background: #fff3cd; color: #856404; padding: 1px 6px; border-radius: 4px; font-size: 11px; }
.mine-tag { background: #e6f4ea; color: var(--green); padding: 1px 6px; border-radius: 4px; font-size: 11px; }
.unread-dot { background: var(--green); color: #fff; border-radius: 10px; padding: 1px 6px; font-size: 11px; flex-shrink: 0; }
.session-time { font-size: 11px; color: #aaa; }
.empty-sessions { text-align: center; padding: 40px 20px; color: #999; }
.empty-sessions span { font-size: 36px; display: block; margin-bottom: 8px; }
.empty-sessions p { font-size: 13px; }
.loading { text-align: center; padding: 24px; color: #999; font-size: 13px; }
.chat-main { flex: 1; background: var(--paper, #fff); border: 1px solid var(--line); border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.chat-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid var(--line); }
.chat-title-area { display: flex; flex-direction: column; gap: 4px; }
.chat-header h3 { margin: 0; font-size: 15px; }
.chat-sub-info { display: flex; gap: 8px; align-items: center; }
.shop-tag { font-size: 12px; color: #888; background: #f5f5f5; padding: 2px 8px; border-radius: 4px; }
.chat-header-actions { display: flex; gap: 8px; }
.message-list { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
.message-item { display: flex; flex-direction: column; max-width: 70%; }
.message-item.self { align-self: flex-end; align-items: flex-end; }
.message-item.system { align-self: center; }
.bubble { padding: 10px 14px; border-radius: 12px; background: #f0f0f0; word-break: break-word; }
.message-item.self .bubble { background: var(--green); color: #fff; }
.msg-time { font-size: 11px; color: #aaa; margin-top: 4px; }
.system-msg { background: #fff3cd; color: #856404; padding: 6px 14px; border-radius: 12px; font-size: 12px; }
.empty-messages { text-align: center; color: #999; padding: 40px; }
.chat-input { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--line); }
.chat-input textarea { flex: 1; resize: none; padding: 10px; border: 1px solid var(--line); border-radius: 8px; font-size: 14px; font-family: inherit; }
.btn-send { background: var(--green); color: #fff; border: none; border-radius: 8px; padding: 0 20px; cursor: pointer; font-weight: 500; }
.btn-send:disabled { opacity: 0.5; cursor: not-allowed; }
.no-selection { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: #999; }
.no-selection span { font-size: 48px; }
</style>