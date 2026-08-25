import { ref, onUnmounted } from 'vue'
import { getSession } from '@/utils/session'

const baseURL = import.meta.env.VITE_API_BASE_URL?.replace(/^http/, 'ws') || 'ws://localhost:8080'

export type MessageHandler = (data: unknown) => void

export function useChatWebSocket(autoDisconnect = true) {
  const connected = ref(false)
  const handlers = new Set<MessageHandler>()
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const MAX_RECONNECT_ATTEMPTS = 10

  function connect() {
    const session = getSession()
    if (!session?.accessToken) {
      connected.value = false
      return
    }

    const token = encodeURIComponent(session.accessToken)
    ws = new WebSocket(`${baseURL}/ws/chat?token=${token}`)

    ws.onopen = () => {
      connected.value = true
      reconnectAttempts = 0
    }

    ws.onmessage = (event) => {
      try {
        // 后端直接推送 MessageResponse 或 NotificationResponse 对象
        const data = JSON.parse(event.data)
        handlers.forEach((handler) => handler(data))
      } catch {
        // 忽略非 JSON 消息
      }
    }

    ws.onclose = () => {
      connected.value = false
      ws = null
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        reconnectAttempts++
        reconnectTimer = setTimeout(connect, Math.min(1000 * reconnectAttempts, 10000))
      }
    }

    ws.onerror = () => {
      ws?.close()
    }
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
    reconnectAttempts = 0
  }

  function onMessage(handler: MessageHandler) {
    handlers.add(handler)
    return () => handlers.delete(handler)
  }

  function reconnect() {
    disconnect()
    reconnectAttempts = 0
    connect()
  }

  if (autoDisconnect) {
    onUnmounted(() => {
      disconnect()
    })
  }

  return {
    connected,
    connect,
    disconnect,
    onMessage,
    reconnect,
  }
}

/** 全局单例 WebSocket（避免多次连接） */
let globalWs: ReturnType<typeof useChatWebSocket> | null = null
let globalRefCount = 0
export function useGlobalChatWebSocket() {
  globalRefCount++
  if (!globalWs) {
    globalWs = useChatWebSocket(false) // 全局实例不自动断开
    globalWs.connect()
  }
  return {
    ...globalWs,
    disconnect: () => {
      globalRefCount--
      if (globalRefCount <= 0) {
        globalRefCount = 0
        globalWs?.disconnect()
        globalWs = null
      }
    },
  }
}