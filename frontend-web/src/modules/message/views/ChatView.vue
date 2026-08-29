<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { readApiError } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { chatMessageApi, chatSessionApi } from '../services/message'
import { useChatWebSocket } from '../composables/useChatWebSocket'
import {
  SENDER_TYPE_LABELS,
  SESSION_STATUS_LABELS,
  type ChatMessage,
  type ChatSession,
} from '../types'

const auth = useAuthStore()

// ---------- 状态 ----------
const sessions = ref<ChatSession[]>([])
const activeSessionId = ref<string | null>(null)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const loadingSessions = ref(true)
const loadingMessages = ref(false)
const sending = ref(false)
const messageError = ref('')
const showNewSessionDialog = ref(false)
const newSessionSubject = ref('')
const newSessionFirstMessage = ref('')
const wsConnected = ref(false)
const wsReconnecting = ref(false)

// ---------- WebSocket ----------
const { connected, reconnecting, connect, disconnect, onMessage } = useChatWebSocket()
let offMessage: (() => void) | null = null

watch(connected, (val) => { wsConnected.value = val })
watch(reconnecting, (val) => { wsReconnecting.value = val })

function handleWsMessage(msg: ChatMessage) {
  // 如果消息属于当前会话，追加到消息列表
  if (activeSessionId.value && msg.sessionId === activeSessionId.value) {
    const exists = messages.value.some((m) => m.id === msg.id)
    if (!exists) {
      messages.value.push(msg)
      scrollToBottom()
      // 自动标记为已读
      markMessagesAsRead([msg.id])
    }
  }
  // 更新会话列表中的最后消息和未读数
  updateSessionFromMessage(msg)
}

function updateSessionFromMessage(msg: ChatMessage) {
  const session = sessions.value.find((s) => s.sessionId === msg.sessionId)
  if (session) {
    session.lastMessage = msg.content
    session.lastMessageTime = msg.createdAt
    if (msg.senderId !== auth.session?.userId) {
      session.unreadCount = (session.unreadCount ?? 0) + 1
    }
  }
}

// ---------- 会话操作 ----------
async function loadSessions() {
  loadingSessions.value = true
  messageError.value = ''
  try {
    sessions.value = await chatSessionApi.listMy()
  } catch (error) {
    messageError.value = readApiError(error, '会话加载失败')
  } finally {
    loadingSessions.value = false
  }
}

async function selectSession(sessionId: string) {
  activeSessionId.value = sessionId
  messages.value = []
  await loadMessages(sessionId)
  // 标记该会话所有消息为已读
  const unreadMessages = messages.value.filter((m) => m.isRead === 0 && m.senderId !== auth.session?.userId)
  if (unreadMessages.length > 0) {
    await markMessagesAsRead(unreadMessages.map((m) => m.id))
  }
  // 更新会话未读数
  const session = sessions.value.find((s) => s.sessionId === sessionId)
  if (session) session.unreadCount = 0
}

async function loadMessages(sessionId: string) {
  loadingMessages.value = true
  try {
    messages.value = await chatMessageApi.list(sessionId, 1, 50)
    await nextTick()
    scrollToBottom()
  } catch (error) {
    messageError.value = readApiError(error, '消息加载失败')
  } finally {
    loadingMessages.value = false
  }
}

async function markMessagesAsRead(messageIds: string[]) {
  if (!activeSessionId.value || messageIds.length === 0) return
  try {
    await chatMessageApi.markAsRead(activeSessionId.value, messageIds)
    for (const msg of messages.value) {
      if (messageIds.includes(msg.id)) msg.isRead = 1
    }
  } catch {
    // 已读标记失败不阻断
  }
}

async function createSession() {
  if (!newSessionFirstMessage.value.trim()) return
  try {
    const session = await chatSessionApi.create({
      subject: newSessionSubject.value.trim() || '客服咨询',
      firstMessage: newSessionFirstMessage.value.trim(),
    })
    showNewSessionDialog.value = false
    newSessionSubject.value = ''
    newSessionFirstMessage.value = ''
    await loadSessions()
    if (session?.sessionId) {
      await selectSession(session.sessionId)
    }
  } catch (error) {
    messageError.value = readApiError(error, '创建会话失败')
  }
}

async function sendMessage() {
  if (!activeSessionId.value || !inputText.value.trim() || sending.value) return
  const content = inputText.value.trim()
  sending.value = true
  messageError.value = ''
  try {
    const msg = await chatMessageApi.send(activeSessionId.value, { content })
    if (msg) {
      messages.value.push(msg)
      scrollToBottom()
      // 更新会话列表
      updateSessionFromMessage(msg)
    }
    inputText.value = ''
  } catch (error) {
    messageError.value = readApiError(error, '发送失败')
  } finally {
    sending.value = false
  }
}

async function closeSession(sessionId: string) {
  if (!window.confirm('确定要结束本次客服对话吗？')) return
  try {
    await chatSessionApi.close(sessionId)
    if (activeSessionId.value === sessionId) {
      activeSessionId.value = null
      messages.value = []
    }
    await loadSessions()
  } catch (error) {
    messageError.value = readApiError(error, '关闭会话失败')
  }
}

async function recallMessage(messageId: string) {
  if (!window.confirm('确定要撤回这条消息吗？')) return
  try {
    await chatMessageApi.recall(messageId)
    messages.value = messages.value.filter((m) => m.id !== messageId)
  } catch (error) {
    messageError.value = readApiError(error, '撤回失败')
  }
}

function scrollToBottom() {
  nextTick(() => {
    const container = document.querySelector('.chat-messages')
    if (container) container.scrollTop = container.scrollHeight
  })
}

function formatTime(value: string | null | undefined) {
  if (!value) return ''
  return value.replace('T', ' ').slice(5, 16)
}

function isOwnMessage(msg: ChatMessage) {
  return msg.senderId === auth.session?.userId
}

const activeSession = computed(() =>
  sessions.value.find((s) => s.sessionId === activeSessionId.value) ?? null,
)

onMounted(async () => {
  await loadSessions()
  // 自动连接 WebSocket
  connect()
  offMessage = onMessage(handleWsMessage)
})

onUnmounted(() => {
  if (offMessage) offMessage()
  disconnect()
})
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">CHAT</p>
        <h1>客服聊天</h1>
        <p>如有疑问，请联系在线客服为您解答。</p>
      </div>
      <div class="page-actions">
        <span v-if="wsReconnecting" class="ws-status reconnecting">WebSocket 重连中…</span>
        <span v-else-if="wsConnected" class="ws-status connected">● 在线</span>
        <span v-else class="ws-status disconnected">○ 离线</span>
        <button class="primary-button" type="button" @click="showNewSessionDialog = true">发起咨询</button>
      </div>
    </div>

    <p v-if="messageError" class="notice error">{{ messageError }}</p>

    <div class="chat-layout">
      <!-- 会话列表 -->
      <aside class="chat-sessions">
        <div v-if="loadingSessions" class="loading-card">加载会话…</div>
        <div v-else-if="!sessions.length" class="empty-state">
          <span>💬</span>
          <h2>暂无会话</h2>
          <p>点击右上角「发起咨询」联系客服</p>
        </div>
        <template v-else>
          <button
            v-for="s in sessions"
            :key="s.sessionId"
            type="button"
            :class="['session-item', { active: activeSessionId === s.sessionId, closed: s.status === 1 }]"
            @click="selectSession(s.sessionId)"
          >
            <div class="session-head">
              <span class="session-subject">{{ s.subject || '客服咨询' }}</span>
              <span :class="['status-badge', `status-${s.status}`]">{{ SESSION_STATUS_LABELS[s.status] }}</span>
            </div>
            <div class="session-preview">
              <span class="muted preview-text">{{ s.lastMessage || '暂无消息' }}</span>
              <span v-if="s.unreadCount > 0" class="unread-badge">{{ s.unreadCount }}</span>
            </div>
            <div class="session-time muted">{{ formatTime(s.lastMessageTime) }}</div>
          </button>
        </template>
      </aside>

      <!-- 聊天区域 -->
      <div class="chat-main">
        <template v-if="!activeSession">
          <div class="empty-state chat-empty">
            <span>💬</span>
            <h2>选择或创建一个会话</h2>
            <p>从左侧选择已有会话，或点击「发起咨询」开始新对话。</p>
          </div>
        </template>

        <template v-else>
          <div class="chat-header">
            <div>
              <strong>{{ activeSession.subject || '客服咨询' }}</strong>
              <span class="muted"> · {{ SESSION_STATUS_LABELS[activeSession.status] }}</span>
            </div>
            <button
              v-if="activeSession.status === 0"
              class="text-button"
              type="button"
              @click="closeSession(activeSession.sessionId)"
            >结束对话</button>
          </div>

          <div v-if="loadingMessages" class="loading-card">加载消息…</div>

          <div v-else class="chat-messages">
            <div v-if="!messages.length" class="empty-state">
              <span>💭</span>
              <p>暂无消息，开始对话吧。</p>
            </div>
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['message-item', isOwnMessage(msg) ? 'own' : 'other']"
            >
              <div class="message-avatar">
                {{ (msg.senderName || SENDER_TYPE_LABELS[msg.senderType] || '?').slice(0, 1) }}
              </div>
              <div class="message-body">
                <div class="message-meta">
                  <span class="sender-name">{{ msg.senderName || SENDER_TYPE_LABELS[msg.senderType] }}</span>
                  <span class="muted">{{ formatTime(msg.createdAt) }}</span>
                </div>
                <div class="message-bubble" :data-type="msg.msgType">
                  {{ msg.content }}
                </div>
                <div v-if="isOwnMessage(msg)" class="message-actions">
                  <span class="muted">{{ msg.isRead === 1 ? '已读' : '未读' }}</span>
                  <button class="text-button small" type="button" @click="recallMessage(msg.id)">撤回</button>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <input
              v-model="inputText"
              class="chat-input-field"
              type="text"
              placeholder="输入消息，Enter 发送"
              :disabled="activeSession.status === 1"
              @keyup.enter="sendMessage"
            />
            <button
              class="primary-button"
              type="button"
              :disabled="!inputText.trim() || sending || activeSession.status === 1"
              @click="sendMessage"
            >{{ sending ? '发送中…' : '发送' }}</button>
          </div>
        </template>
      </div>
    </div>

    <!-- 新建会话弹窗 -->
    <div v-if="showNewSessionDialog" class="dialog-overlay" @click.self="showNewSessionDialog = false">
      <div class="dialog-card">
        <h2>发起客服咨询</h2>
        <div class="dialog-field">
          <label>咨询主题（可选）</label>
          <input v-model="newSessionSubject" type="text" placeholder="例：订单问题、商品咨询" />
        </div>
        <div class="dialog-field">
          <label>咨询内容</label>
          <textarea v-model="newSessionFirstMessage" placeholder="请描述您的问题…" rows="4"></textarea>
        </div>
        <div class="dialog-actions">
          <button class="secondary-button" type="button" @click="showNewSessionDialog = false">取消</button>
          <button class="primary-button" type="button" :disabled="!newSessionFirstMessage.trim()" @click="createSession">开始咨询</button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ws-status {
  font-size: 13px;
  font-weight: 600;
}
.ws-status.connected { color: #2b8a3e; }
.ws-status.reconnecting { color: #e67700; }
.ws-status.disconnected { color: #868e96; }

.chat-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  min-height: 500px;
}

/* 会话列表 */
.chat-sessions {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.session-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 14px 16px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  transition: background .15s, border-color .15s;
}

.session-item:hover {
  background: #f6f8f6;
}

.session-item.active {
  background: #e9f5ef;
  border-color: #b2d3c2;
}

.session-item.closed {
  opacity: 0.55;
}

.session-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.session-subject {
  font-weight: 700;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.preview-text {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.unread-badge {
  background: #e03131;
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 999px;
  flex-shrink: 0;
}

.session-time {
  font-size: 12px;
}

/* 聊天区域 */
.chat-main {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  min-height: 500px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--line);
  background: #fafbf9;
}

.chat-empty {
  flex: 1;
  display: grid;
  place-items: center;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 75%;
}

.message-item.own {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #e9f5ef;
  color: var(--green-dark);
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.message-item.own .message-avatar {
  background: #e3f0ff;
  color: #1d5fa8;
}

.message-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.message-item.own .message-body {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.sender-name {
  font-weight: 600;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  background: #f2f5f2;
  border-top-left-radius: 4px;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-item.own .message-bubble {
  background: var(--green);
  color: white;
  border-top-left-radius: 14px;
  border-top-right-radius: 4px;
}

.message-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

/* 输入区 */
.chat-input {
  display: flex;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--line);
  background: #fafbf9;
}

.chat-input-field {
  flex: 1;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: white;
  font-size: 15px;
}

.chat-input-field:focus {
  outline: none;
  border-color: var(--green);
}

/* 弹窗 */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: grid;
  place-items: center;
  z-index: 100;
}

.dialog-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  width: 420px;
  max-width: 90vw;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-card h2 {
  margin: 0;
}

.dialog-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dialog-field label {
  font-size: 14px;
  font-weight: 600;
}

.dialog-field input,
.dialog-field textarea {
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  font-size: 15px;
  font-family: inherit;
  resize: vertical;
}

.dialog-field input:focus,
.dialog-field textarea:focus {
  outline: none;
  border-color: var(--green);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

@media (max-width: 720px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }
  .chat-sessions {
    max-height: 200px;
  }
}
</style>
