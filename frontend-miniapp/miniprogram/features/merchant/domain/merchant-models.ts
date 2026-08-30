export type MerchantType = 'ENTERPRISE' | 'SOLE_PROPRIETOR' | 'INDIVIDUAL'
export type IdentityDocumentType = 'MAINLAND_ID_CARD' | 'PASSPORT' | 'OTHER'

export interface MerchantApplicationRequest {
  merchantType: MerchantType
  shopName: string
  subjectName?: string
  unifiedSocialCreditCode?: string
  responsiblePersonName: string
  identityDocumentType: IdentityDocumentType
  identityDocumentNumber: string
  contactPhone: string
  contactEmail: string
}

export interface MerchantApplicationReceipt {
  id: string
  status: string
}

export interface ShopSummary {
  id: string
  name: string
  status: string
}

export interface ShopStaffAccount {
  id: string
  shopId: string
  shopName: string | null
  userId: string
  displayName: string
  maskedEmail: string | null
  username: string | null
  status: 'PENDING_AUDIT' | 'ACTIVE' | 'REJECTED' | 'REVOKED' | 'DISABLED'
  auditRemark: string | null
  emailDeliveryStatus: 'PENDING' | 'SENT' | 'MAIL_FAILED' | 'SKIPPED'
  mustChangePassword: boolean
  createdAt: string
}

export interface ShopMetrics {
  paidOrderCount: string
  paidBuyerCount: string
  grossPaidAmount: string
  successfulRefundAmount: string
  netCashflowActivity: string
  averageOrderValue: string | null
  soldQuantity: string
  onSaleProductSnapshot: string
  displayedReviewCount: string
  averageRating: string | null
}

export interface ShopOverview {
  metricVersion: string
  timezone: string
  generatedAt: string
  dataAsOf: string
  range: { startAt: string; endAt: string }
  shopId: string
  shopName: string
  metrics: ShopMetrics
}

export const STAFF_STATUS_LABELS: Record<ShopStaffAccount['status'], string> = {
  PENDING_AUDIT: '待审核',
  ACTIVE: '已开通',
  REJECTED: '已驳回',
  REVOKED: '已撤销',
  DISABLED: '已停用',
}

export const MERCHANT_TYPE_OPTIONS: { value: MerchantType; label: string }[] = [
  { value: 'ENTERPRISE', label: '企业' },
  { value: 'SOLE_PROPRIETOR', label: '个体工商户' },
  { value: 'INDIVIDUAL', label: '个人商家' },
]

export const IDENTITY_TYPE_OPTIONS: { value: IdentityDocumentType; label: string }[] = [
  { value: 'MAINLAND_ID_CARD', label: '居民身份证' },
  { value: 'PASSPORT', label: '护照' },
  { value: 'OTHER', label: '其他证件' },
]
