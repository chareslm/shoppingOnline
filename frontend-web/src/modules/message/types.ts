// 消息模块前端类型，与 docs/api/chat.md、docs/api/message.md 契约一致。
// 注意：后端雪花 ID 以字符串形式返回（避免超出 JS Number 安全范围），ID 字段统一为 string。

// ---------- 聊天会话 ----------

export interface ChatSession {
  sessionId: string
  userId: string
  shopId: string | null
  csUserId: string | null
  subject: string | null
  lastMessage: string | null
  lastMessageTime: string | null
  /** 0 进行中 / 1 已结束 */
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

// ---------- 聊天消息 ----------

export interface ChatMessage {
  id: string
  sessionId: string
  senderId: string
  /** 1 用户 / 2 客服 / 3 系统 */
  senderType: number
  senderName: string | null
  senderAvatar: string | null
  content: string
  /** 1 文本 / 2 图片 / 3 商品卡片 / 4 系统通知 */
  msgType: number
  extraData: string | null
  /** 0 未读 / 1 已读 */
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

// ---------- 站内信通知 ----------

export interface NotificationItem {
  id: string
  templateId: string
  templateCode: string
  title: string
  content: string
  /** 1 系统 / 2 订单 / 3 营销 / 4 客服 */
  category: number
  categoryDesc: string
  bizType: string | null
  bizId: string | null
  /** 0 未读 / 1 已读 */
  isRead: number
  readTime: string | null
  /** 0 未推送 / 1 成功 / 2 失败 */
  pushStatus: number
  pushTime: string | null
  createdAt: string
}

export interface NotificationPreference {
  id: string
  userId: string
  /** 0 关闭 / 1 开启 */
  systemEnabled: number
  orderEnabled: number
  marketingEnabled: number
  serviceEnabled: number
}

export interface UpdatePreferenceRequest {
  systemEnabled: number
  orderEnabled: number
  marketingEnabled: number
  serviceEnabled: number
}

export interface MarkBatchReadRequest {
  notificationIds: string[]
}

// ---------- 枚举标签 ----------

export const SESSION_STATUS_LABELS: Record<number, string> = {
  0: '进行中',
  1: '已结束',
}

export const SENDER_TYPE_LABELS: Record<number, string> = {
  1: '用户',
  2: '客服',
  3: '系统',
}

export const MSG_TYPE_LABELS: Record<number, string> = {
  1: '文本',
  2: '图片',
  3: '商品卡片',
  4: '系统通知',
}

export const NOTIFICATION_CATEGORY_LABELS: Record<number, string> = {
  1: '系统通知',
  2: '订单通知',
  3: '营销通知',
  4: '客服消息',
}

export const NOTIFICATION_CATEGORIES: { value: number | 'all'; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 1, label: '系统' },
  { value: 2, label: '订单' },
  { value: 3, label: '营销' },
  { value: 4, label: '客服' },
]

export const PUSH_STATUS_LABELS: Record<number, string> = {
  0: '未推送',
  1: '已推送',
  2: '推送失败',
}
