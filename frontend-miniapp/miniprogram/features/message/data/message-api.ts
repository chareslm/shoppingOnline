import { apiRequest } from '../../../core/http/api-client'
import type {
  ChatMessage,
  ChatSession,
  CreateSessionRequest,
  NotificationItem,
  SendMessageRequest,
} from '../domain/message-models'

function withQuery(path: string, params: Record<string, string | number | undefined>): string {
  const parts: string[] = []
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === '') continue
    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  }
  return parts.length ? `${path}?${parts.join('&')}` : path
}

export const chatSessionApi = {
  create(payload: CreateSessionRequest): Promise<ChatSession> {
    return apiRequest({ path: '/api/chat/sessions', method: 'POST', data: payload })
  },

  listMy(): Promise<ChatSession[]> {
    return apiRequest({ path: '/api/chat/sessions' })
  },

  listCs(): Promise<ChatSession[]> {
    return apiRequest({ path: '/api/chat/sessions/cs' })
  },

  assign(sessionId: string): Promise<ChatSession> {
    return apiRequest({ path: `/api/chat/sessions/${sessionId}/assign`, method: 'PUT' })
  },
}

export const chatMessageApi = {
  send(sessionId: string, payload: SendMessageRequest): Promise<ChatMessage> {
    return apiRequest({ path: `/api/chat/messages/${sessionId}`, method: 'POST', data: payload })
  },

  list(sessionId: string, page = 1, pageSize = 50): Promise<ChatMessage[]> {
    return apiRequest({ path: withQuery(`/api/chat/messages/${sessionId}`, { page, pageSize }) })
  },
}

export const notificationApi = {
  list(category?: number, page = 1, pageSize = 20): Promise<NotificationItem[]> {
    return apiRequest({
      path: withQuery('/api/message/notifications', { category, page, pageSize }),
    })
  },

  markRead(notificationId: string): Promise<void> {
    return apiRequest({ path: `/api/message/notifications/${notificationId}/read`, method: 'PUT' })
  },

  markAllRead(): Promise<void> {
    return apiRequest({ path: '/api/message/notifications/read-all', method: 'PUT' })
  },
}
