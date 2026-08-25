<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">CUSTOMER SERVICE</p>
        <h1>在线客服</h1>
        <p class="subtle">选择会话开始与客服沟通</p>
      </div>
      <button class="btn-primary" @click="showCreateDialog = true">+ 发起新会话</button>
    </div>

    <div class="chat-layout">
      <!-- 会话列表 -->
      <aside class="session-sidebar">
        <div class="sidebar-header">
          <h3>我的会话</h3>
          <span v-if="sessions.length" class="count-badge">{{ sessions.length }}</span>
        </div>
        <div class="session-list">
          <div
            v-for="s in sessions"
            :key="s.sessionId"
            class="session-item"
            :class="{ active: selectedSession?.sessionId === s.sessionId }"
            @click="selectSession(s)"
          >
            <div class="session-info">
              <div class="session-title">{{ s.subject || '客服咨询' }}</div>
              <div class="session-preview">{{ s.lastMessage || '暂无消息' }}</div>
            </div>
            <div class="session-meta">
              <span v-if="s.unreadCount > 0" class="unread-dot">{{ s.unreadCount }}</span>
              <span class="session-time">{{ formatTime(s.lastMessageTime) }}</span>
            </div>
          </div>
          <div v-if="!sessions.length && !loading" class="empty-sessions">暂无会话，点击右上角发起</div>
          <div v-if="loading" class="loading">加载中...</div>
        </div>
      </aside>

      <!-- 消息面板 -->
      <main class="chat-main">
        <template v-if="selectedSession">
          <div class="chat-header">
            <h3>{{ selectedSession.subject || '客服咨询' }}</h3>
            <span class="status-tag">{{ SESSION_STATUS_LABELS[selectedSession.status] || '未知' }}</span>
          </div>

          <div class="message-list" ref="messageListRef">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ self: isUserSender(msg.senderType), system: isSystemSender(msg.senderType) }"
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
              placeholder="输入消息，Enter 发送，Shift+Enter 换行"
              @keydown.enter.exact.prevent="sendMessage"
              rows="1"
            />
            <button class="btn-send" :disabled="!inputText.trim()" @click="sendMessage">发送</button>
          </div>
        </template>
        <div v-else class="no-selection">
          <span>💬</span>
          <p>选择左侧会话开始聊天</p>
          <button class="btn-primary" @click="showCreateDialog = true">发起新会话</button>
        </div>
      </main>
    </div>

    <!-- 新建会话弹窗 -->
    <div v-if="showCreateDialog" class="dialog-mask" @click.self="showCreateDialog = false">
      <div class="dialog">
        <h3>发起客服会话</h3>
        <div class="form-group">
          <label>店铺 ID</label>
          <input v-model="newSession.shopId" placeholder="请输入店铺 ID" />
        </div>
        <div class="form-group">
          <label>会话主题（可选）</label>
          <input v-model="newSession.subject" placeholder="如：商品咨询、售后问题" />
        </div>
        <div class="form-group">
          <label>首条消息（可选）</label>
          <textarea v-model="newSession.firstMessage" rows="3" placeholder="描述您的问题" />
        </div>
        <div class="dialog-actions">
          <button class="btn-ghost" @click="showCreateDialog = false">取消</button>
          <button class="btn-primary" :disabled="!newSession.shopId.trim() || creating" @click="createSession">
            {{ creating ? '创建中...' : '创建会话' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { chatSessionApi, chatMessageApi } from '@/modules/message/services/message'
import { useGlobalChatWebSocket } from '@/modules/message/composables/useChatWebSocket'
import {
  SESSION_STATUS_LABELS,
  MessageType,
  isUserSender,
  isSystemSender,
} from '@/modules/message/types'
import type { ChatSession, ChatMessage } from '@/modules/message/types'
import { readApiError } from '@/services/http'

const sessions = ref<ChatSession[]>([])
const selectedSession = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const loading = ref(false)
const loadingMessages = ref(false)
const creating = ref(false)
const showCreateDialog = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

const newSession = ref({ shopId: '', subject: '', firstMessage: '' })

let pollingTimer: ReturnType<typeof setInterval> | null = null
let wsUnsubscribe: (() => void) | null = null

// WebSocket
const ws = useGlobalChatWebSocket()
wsUnsubscribe = ws.onMessage((data) => {
  // 后端直接推送 MessageResponse 对象
  const msg = data as ChatMessage
  if (msg.sessionId && selectedSession.value) {
    if (msg.sessionId === selectedSession.value.sessionId) {
      messages.value.push(msg)
      scrollToBottom()
    }
    // 更新会话列表
    const session = sessions.value.find((s) => s.sessionId === msg.sessionId)
    if (session) {
      session.lastMessage = msg.content
      session.lastMessageTime = msg.createdAt
      if (!isUserSender(msg.senderType)) {
        session.unreadCount++
      }
    } else {
      // 新会话的消息，重新加载列表
      loadSessions()
    }
  }
})

async function loadSessions() {
  loading.value = true
  try {
    sessions.value = await chatSessionApi.listMy()
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

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !selectedSession.value) return
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

async function createSession() {
  if (!newSession.value.shopId.trim()) return
  creating.value = true
  try {
    const session = await chatSessionApi.create(
      newSession.value.shopId,
      newSession.value.subject || undefined,
      newSession.value.firstMessage || undefined,
    )
    showCreateDialog.value = false
    newSession.value = { shopId: '', subject: '', firstMessage: '' }
    await loadSessions()
    await selectSession(session)
  } catch (err) {
    alert('创建会话失败：' + readApiError(err))
  } finally {
    creating.value = false
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

// 轮询：每 10 秒拉取当前会话的新消息
function startPolling() {
  pollingTimer = setInterval(async () => {
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
      } catch { /* 静默忽略 */ }
    }
  }, 10000)
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
.chat-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 180px);
  min-height: 500px;
}
.session-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: var(--paper, #fff);
  border: 1px solid var(--line, #e8e8e8);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--line);
}
.sidebar-header h3 { font-size: 14px; margin: 0; }
.count-badge {
  background: var(--green, #00843d);
  color: #fff;
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 12px;
}
.session-list {
  flex: 1;
  overflow-y: auto;
}
.session-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
  transition: background 0.15s;
}
.session-item:hover { background: #f5f5f5; }
.session-item.active { background: #e6f4ea; border-left: 3px solid var(--green); }
.session-info { display: flex; flex-direction: column; gap: 4px; }
.session-title { font-weight: 600; font-size: 14px; }
.session-preview { font-size: 12px; color: #888; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
.unread-dot {
  background: var(--green);
  color: #fff;
  border-radius: 10px;
  padding: 1px 6px;
  font-size: 11px;
}
.session-time { font-size: 11px; color: #aaa; }
.empty-sessions, .empty-messages, .loading { text-align: center; padding: 24px; color: #999; font-size: 13px; }
.chat-main {
  flex: 1;
  background: var(--paper, #fff);
  border: 1px solid var(--line);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
}
.chat-header h3 { margin: 0; font-size: 15px; }
.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #e6f4ea;
  color: var(--green);
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.message-item { display: flex; flex-direction: column; max-width: 70%; }
.message-item.self { align-self: flex-end; align-items: flex-end; }
.message-item.system { align-self: center; }
.bubble {
  padding: 10px 14px;
  border-radius: 12px;
  background: #f0f0f0;
  word-break: break-word;
}
.message-item.self .bubble { background: var(--green, #00843d); color: #fff; }
.msg-time { font-size: 11px; color: #aaa; margin-top: 4px; }
.system-msg {
  background: #fff3cd;
  color: #856404;
  padding: 6px 14px;
  border-radius: 12px;
  font-size: 12px;
}
.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--line);
}
.chat-input textarea {
  flex: 1;
  resize: none;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
}
.btn-send {
  background: var(--green);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 0 20px;
  cursor: pointer;
  font-weight: 500;
}
.btn-send:disabled { opacity: 0.5; cursor: not-allowed; }
.no-selection {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #999;
}
.no-selection span { font-size: 48px; }
.btn-primary {
  background: var(--green);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 20px;
  cursor: pointer;
  font-weight: 500;
}
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-ghost {
  background: transparent;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 8px 16px;
  cursor: pointer;
}
.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.dialog {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 420px;
  max-width: 90vw;
}
.dialog h3 { margin: 0 0 16px; }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 13px; color: #666; }
.form-group input, .form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  box-sizing: border-box;
}
.dialog-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 20px; }
</style>