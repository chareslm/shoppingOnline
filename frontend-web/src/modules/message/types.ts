// ========== 枚举值（对应后端 ordinal） ==========
export const SessionStatus = {
  IN_PROGRESS: 0,
  CLOSED: 1,
} as const
export type SessionStatusValue = (typeof SessionStatus)[keyof typeof SessionStatus]

export const SenderType = {
  USER: 1,
  CS: 2,
  SYSTEM: 3,
} as const
export type SenderTypeValue = (typeof SenderType)[keyof typeof SenderType]

export const MessageType = {
  TEXT: 1,
  IMAGE: 2,
  PRODUCT_CARD: 3,
  SYSTEM: 4,
} as const
export type MessageTypeValue = (typeof MessageType)[keyof typeof MessageType]

export const NotificationCategory = {
  SYSTEM: 1,
  ORDER: 2,
  MARKETING: 3,
  SERVICE: 4,
} as const
export type NotificationCategoryValue = (typeof NotificationCategory)[keyof typeof NotificationCategory]

// ========== 聊天会话 ==========
export interface ChatSession {
  sessionId: string
  userId: string
  shopId: string | null
  csUserId: string | null
  subject: string | null
  status: number
  priority: number
  lastMessage: string | null
  lastMessageTime: string | null
  unreadCount: number
  createdAt: string
}

export interface CreateSessionRequest {
  shopId: number
  subject?: string
  firstMessage?: string
}

// ========== 聊天消息 ==========
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

// ========== 通知（站内信） ==========
export interface UserNotification {
  id: string
  templateId: string | null
  templateCode: string | null
  title: string
  content: string
  category: number
  categoryDesc: string | null
  bizType: string | null
  bizId: string | null
  isRead: number
  readTime: string | null
  pushStatus: number | null
  pushTime: string | null
  createdAt: string
}

export interface UnreadCount {
  unreadCount: number
}

export interface NotificationPreference {
  id: string
  userId: string
  systemEnabled: number
  orderEnabled: number
  marketingEnabled: number
  serviceEnabled: number
}

export interface UpdatePreferenceRequest {
  systemEnabled?: number
  orderEnabled?: number
  marketingEnabled?: number
  serviceEnabled?: number
}

// ========== 标签映射 ==========
export const SESSION_STATUS_LABELS: Record<number, string> = {
  0: '进行中',
  1: '已结束',
}

export const SENDER_TYPE_LABELS: Record<number, string> = {
  1: '买家',
  2: '客服',
  3: '系统',
}

export const NOTIFICATION_CATEGORY_LABELS: Record<number, string> = {
  1: '系统通知',
  2: '订单通知',
  3: '营销活动',
  4: '客服消息',
}

export function getCategoryLabel(category: number) {
  return NOTIFICATION_CATEGORY_LABELS[category] ?? '未知'
}

export function getSenderTypeLabel(senderType: number) {
  return SENDER_TYPE_LABELS[senderType] ?? '未知'
}

export function isUserSender(senderType: number) {
  return senderType === SenderType.USER
}

export function isCsSender(senderType: number) {
  return senderType === SenderType.CS
}

export function isSystemSender(senderType: number) {
  return senderType === SenderType.SYSTEM
}

export function isReadFlag(value: number) {
  return value === 1
}