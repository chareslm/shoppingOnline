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
