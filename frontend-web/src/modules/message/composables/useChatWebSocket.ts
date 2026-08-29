import { ref, onUnmounted } from 'vue'
import { getSession } from '@/utils/session'
import type { ChatMessage } from '../types'

/**
 * 客服聊天 WebSocket composable。
 * 连接后端 ws://host:port/ws/chat?token={accessToken}
 * 后端直接推送 MessageResponse JSON（无外层 envelope）。
 */
export function useChatWebSocket() {
  const connected = ref(false)
  const reconnecting = ref(false)
  const error = ref<string | null>(null)

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectDelay = 1000
  let manualClose = false
  const messageHandlers = new Set<(msg: ChatMessage) => void>()

  const baseURL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'
  const wsURL = baseURL.replace('http://', 'ws://').replace('https://', 'wss://')

  function connect() {
    const session = getSession()
    if (!session?.accessToken) {
      error.value = '未登录，无法建立 WebSocket 连接'
      return
    }

    manualClose = false
    error.value = null

    try {
      ws = new WebSocket(`${wsURL}/ws/chat?token=${encodeURIComponent(session.accessToken)}`)

      ws.onopen = () => {
        connected.value = true
        reconnecting.value = false
        reconnectDelay = 1000
      }

      ws.onmessage = (event) => {
        try {
          const msg: ChatMessage = JSON.parse(event.data)
          for (const handler of messageHandlers) {
            try {
              handler(msg)
            } catch {
              // ignore handler errors
            }
          }
        } catch {
          console.warn('Failed to parse WebSocket message:', event.data)
        }
      }

      ws.onclose = () => {
        connected.value = false
        ws = null
        if (!manualClose) {
          scheduleReconnect()
        }
      }

      ws.onerror = () => {
        error.value = 'WebSocket 连接错误'
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'WebSocket 创建失败'
    }
  }

  function scheduleReconnect() {
    if (manualClose) return
    reconnecting.value = true
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      reconnectDelay = Math.min(reconnectDelay * 2, 30_000)
      connect()
    }, reconnectDelay)
  }

  function disconnect() {
    manualClose = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
    reconnecting.value = false
  }

  function onMessage(handler: (msg: ChatMessage) => void) {
    messageHandlers.add(handler)
    return () => messageHandlers.delete(handler)
  }

  // Auto-connect on first use
  if (!ws && !manualClose) {
    connect()
  }

  onUnmounted(() => {
    disconnect()
    messageHandlers.clear()
  })

  return {
    connected,
    reconnecting,
    error,
    connect,
    disconnect,
    onMessage,
  }
}
