import { http, unwrap } from '@/services/http'
import type { ApiResponse } from '@/types/api'
import type {
  ChatMessage,
  ChatSession,
  CreateSessionRequest,
  MarkBatchReadRequest,
  NotificationItem,
  NotificationPreference,
  SendMessageRequest,
  UpdatePreferenceRequest,
} from '../types'

// ---------- 会话管理 ----------

export const chatSessionApi = {
  async create(payload: CreateSessionRequest) {
    return unwrap((await http.post<ApiResponse<ChatSession>>('/api/chat/sessions', payload)).data)
  },
  async listMy() {
    return unwrap((await http.get<ApiResponse<ChatSession[]>>('/api/chat/sessions')).data)
  },
  async listCs() {
    return unwrap((await http.get<ApiResponse<ChatSession[]>>('/api/chat/sessions/cs')).data)
  },
  async get(sessionId: string) {
    return unwrap((await http.get<ApiResponse<ChatSession>>(`/api/chat/sessions/${sessionId}`)).data)
  },
  async assign(sessionId: string) {
    return unwrap((await http.put<ApiResponse<ChatSession>>(`/api/chat/sessions/${sessionId}/assign`)).data)
  },
  async close(sessionId: string) {
    return unwrap((await http.put<ApiResponse<null>>(`/api/chat/sessions/${sessionId}/close`)).data)
  },
  async getUnread(sessionId: string) {
    return unwrap((await http.get<ApiResponse<number>>(`/api/chat/sessions/${sessionId}/unread`)).data)
  },
}

// ---------- 消息收发 ----------

export const chatMessageApi = {
  async send(sessionId: string, payload: SendMessageRequest) {
    return unwrap((await http.post<ApiResponse<ChatMessage>>(`/api/chat/messages/${sessionId}`, payload)).data)
  },
  async list(sessionId: string, page = 1, pageSize = 50) {
    return unwrap((await http.get<ApiResponse<ChatMessage[]>>(`/api/chat/messages/${sessionId}`, { params: { page, pageSize } })).data)
  },
  async pullOffline(sessionId: string, lastMessageId?: string) {
    return unwrap((await http.get<ApiResponse<ChatMessage[]>>(`/api/chat/messages/${sessionId}/offline`, { params: lastMessageId ? { lastMessageId } : {} })).data)
  },
  async markAsRead(sessionId: string, messageIds: string[]) {
    return unwrap((await http.put<ApiResponse<null>>(`/api/chat/messages/${sessionId}/read`, messageIds)).data)
  },
  async recall(messageId: string) {
    return unwrap((await http.delete<ApiResponse<null>>(`/api/chat/messages/${messageId}`)).data)
  },
}

// ---------- 站内信通知 ----------

export const notificationApi = {
  async list(category?: number, page = 1, pageSize = 20) {
    const params: Record<string, unknown> = { page, pageSize }
    if (category != null) params.category = category
    return unwrap((await http.get<ApiResponse<NotificationItem[]>>('/api/message/notifications', { params })).data)
  },
  async getUnreadCount() {
    return unwrap((await http.get<ApiResponse<number>>('/api/message/notifications/unread-count')).data)
  },
  async markRead(notificationId: string) {
    return unwrap((await http.put<ApiResponse<null>>(`/api/message/notifications/${notificationId}/read`)).data)
  },
  async markBatchRead(payload: MarkBatchReadRequest) {
    return unwrap((await http.put<ApiResponse<null>>('/api/message/notifications/read-batch', payload)).data)
  },
  async markAllRead() {
    return unwrap((await http.put<ApiResponse<null>>('/api/message/notifications/read-all')).data)
  },
  async getPreference() {
    return unwrap((await http.get<ApiResponse<NotificationPreference>>('/api/message/notifications/preference')).data)
  },
  async updatePreference(payload: UpdatePreferenceRequest) {
    return unwrap((await http.put<ApiResponse<NotificationPreference>>('/api/message/notifications/preference', payload)).data)
  },
}
