export type MerchantApplicationStatus =
  | 'SUBMITTED'
  | 'QUALIFICATION_APPROVED'
  | 'REJECTED'
  | 'ACCOUNT_APPROVED'

export interface QualificationFile {
  id: string
  originalName: string
  contentType: string
  fileSize: number
}

export interface MerchantApplication {
  id: string
  merchantType: 'ENTERPRISE' | 'SOLE_PROPRIETOR' | 'INDIVIDUAL'
  shopName: string
  subjectName?: string | null
  unifiedSocialCreditCode?: string | null
  responsiblePersonName: string
  maskedIdentityDocumentNumber: string
  contactPhone: string
  contactEmail: string
  status: MerchantApplicationStatus
  shopStatus?: 'OPEN' | 'SUSPENDED' | 'FROZEN' | 'CLOSED' | null
  rejectionReason?: string | null
  emailDeliveryStatus?: 'PENDING' | 'SENT' | 'MAIL_FAILED' | 'SKIPPED'
  accountUserId?: string | null
  accountReused?: boolean | null
  files?: QualificationFile[]
  createdAt: string
  updatedAt?: string
}

export interface MerchantApplicationPage {
  items: MerchantApplication[]
  total: number
  page: number
  pageSize: number
}

export interface AdminShopStaff {
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
