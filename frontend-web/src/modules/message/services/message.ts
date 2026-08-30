import { http, unwrap } from '@/services/http'
import { MessageType } from '@/modules/message/types'
import type {
  ChatSession,
  ChatMessage,
  CreateSessionRequest,
  SendMessageRequest,
  UserNotification,
  NotificationPreference,
} from '@/modules/message/types'

/** 聊天会话 API */
export const chatSessionApi = {
  /** 创建会话（买家发起） */
  create: async (shopId: string, subject?: string, firstMessage?: string) => {
    const data: CreateSessionRequest = {
      shopId: Number(shopId),
      subject,
      firstMessage,
    }
    const resp = await http.post('/api/chat/sessions', data)
    return unwrap<ChatSession>(resp.data)
  },

  /** 买家：获取我的会话列表 */
  listMy: async () => {
    const resp = await http.get('/api/chat/sessions')
    return unwrap<ChatSession[]>(resp.data)
  },

  /** 客服：获取我的会话列表（含未分配） */
  listCs: async () => {
    const resp = await http.get('/api/chat/sessions/cs')
    return unwrap<ChatSession[]>(resp.data)
  },

  /** 获取会话详情 */
  get: async (sessionId: string) => {
    const resp = await http.get(`/api/chat/sessions/${sessionId}`)
    return unwrap<ChatSession>(resp.data)
  },

  /** 客服领取/分配会话（当前客服自动成为处理人） */
  assign: async (sessionId: string) => {
    const resp = await http.put(`/api/chat/sessions/${sessionId}/assign`)
    return unwrap<ChatSession>(resp.data)
  },

  /** 关闭会话 */
  close: async (sessionId: string) => {
    const resp = await http.put(`/api/chat/sessions/${sessionId}/close`)
    return unwrap<void>(resp.data)
  },
}

/** 聊天消息 API */
export const chatMessageApi = {
  /** 发送消息 */
  send: async (sessionId: string, content: string, msgType: number = MessageType.TEXT) => {
    const data: SendMessageRequest = { content, msgType }
    const resp = await http.post(`/api/chat/messages/${sessionId}`, data)
    return unwrap<ChatMessage>(resp.data)
  },

  /** 获取消息历史 */
  list: async (sessionId: string, page = 1, pageSize = 50) => {
    const resp = await http.get(`/api/chat/messages/${sessionId}`, { params: { page, pageSize } })
    return unwrap<ChatMessage[]>(resp.data)
  },

  /** 离线补拉（拉取指定 ID 之后的消息） */
  pullOffline: async (sessionId: string, lastMessageId?: string) => {
    const params = lastMessageId ? { lastMessageId: Number(lastMessageId) } : {}
    const resp = await http.get(`/api/chat/messages/${sessionId}/offline`, { params })
    return unwrap<ChatMessage[]>(resp.data)
  },

  /** 批量标记已读 */
  markBatchRead: async (sessionId: string, messageIds: string[]) => {
    const ids = messageIds.map(Number)
    const resp = await http.put(`/api/chat/messages/${sessionId}/read`, ids)
    return unwrap<void>(resp.data)
  },
}

/** 通知 API */
export const notificationApi = {
  /** 获取我的通知列表 */
  list: async (category?: number, page = 1, pageSize = 20) => {
    const params: Record<string, number> = { page, pageSize }
    if (category !== undefined && category !== null) {
      params.category = category
    }
    const resp = await http.get('/api/message/notifications', { params })
    return unwrap<UserNotification[]>(resp.data)
  },

  /** 获取未读数 */
  getUnreadCount: async () => {
    const resp = await http.get('/api/message/notifications/unread-count')
    return unwrap<number>(resp.data)
  },

  /** 标记单条已读 */
  markRead: async (notificationId: string) => {
    const resp = await http.put(`/api/message/notifications/${notificationId}/read`)
    return unwrap<void>(resp.data)
  },

  /** 全部标记已读 */
  markAllRead: async () => {
    const resp = await http.put('/api/message/notifications/read-all')
    return unwrap<void>(resp.data)
  },

  /** 获取通知偏好 */
  getPreference: async () => {
    const resp = await http.get('/api/message/notifications/preference')
    return unwrap<NotificationPreference>(resp.data)
  },

  /** 更新通知偏好 */
  updatePreference: async (pref: Partial<NotificationPreference>) => {
    const body = {
      systemEnabled: pref.systemEnabled ?? 1,
      orderEnabled: pref.orderEnabled ?? 1,
      marketingEnabled: pref.marketingEnabled ?? 0,
      serviceEnabled: pref.serviceEnabled ?? 1,
    }
    const resp = await http.put('/api/message/notifications/preference', body)
    return unwrap<NotificationPreference>(resp.data)
  },
}