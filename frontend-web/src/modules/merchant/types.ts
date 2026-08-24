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
