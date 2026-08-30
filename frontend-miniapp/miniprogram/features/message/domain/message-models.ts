export interface ChatSession {
  sessionId: string
  userId: string
  shopId: string | null
  csUserId: string | null
  subject: string | null
  lastMessage: string | null
  lastMessageTime: string | null
  status: number
  priority: number
  unreadCount: number
  createdAt: string
}

export interface CreateSessionRequest {
  shopId?: string
  subject?: string
  firstMessage?: string
}

export interface ChatMessage {
  id: string
  sessionId: string
  senderId: string
  senderType: number
  senderName: string | null
  senderAvatar: string | null
  content: string
  msgType: number
  extraData: string | null
  isRead: number
  readTime: string | null
  status: number
  createdAt: string
}

export interface SendMessageRequest {
  content: string
  msgType?: number
  extraData?: string
}

export interface NotificationItem {
  id: string
  templateId: string
  templateCode: string
  title: string
  content: string
  category: number
  categoryDesc: string
  bizType: string | null
  bizId: string | null
  isRead: number
  readTime: string | null
  pushStatus: number
  pushTime: string | null
  createdAt: string
}

export const SESSION_STATUS_LABELS: Record<number, string> = {
  0: '进行中',
  1: '已结束',
}

export const SENDER_TYPE_LABELS: Record<number, string> = {
  1: '用户',
  2: '客服',
  3: '系统',
}

export const NOTIFICATION_CATEGORY_LABELS: Record<number, string> = {
  1: '系统通知',
  2: '订单通知',
  3: '营销通知',
  4: '客服消息',
}
