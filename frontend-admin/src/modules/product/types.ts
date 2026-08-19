export const SPU_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_AUDIT: '待审核',
  AUDIT_APPROVED: '审核通过',
  AUDIT_REJECTED: '已驳回/已收回',
  ON_SALE: '上架中',
  OFF_SALE: '已下架',
}

export interface AdminSpuItem {
  id: string
  shopId: string
  shopName: string | null
  categoryId: string
  brand: string | null
  name: string
  subtitle: string | null
  mainImage: string | null
  priceMin: number | null
  priceMax: number | null
  sales: number
  rating: number
  status: string
}

export interface AdminSku {
  id: string
  spuId: string
  skuCode: string | null
  attributes: string | null
  image: string | null
  price: number
  availableStock: number
  reservedStock: number
  soldStock: number
  status: number
}

export interface AdminSpuDetail extends AdminSpuItem {
  categoryName: string | null
  images: string[]
  detail: string | null
  auditRemark: string | null
  createdAt: string
  skus: AdminSku[]
}

export function formatSkuAttributes(raw: string | null | undefined, empty = '默认规格'): string {
  if (!raw?.trim()) return empty
  try {
    const parsed = JSON.parse(raw) as unknown
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const parts = Object.entries(parsed as Record<string, unknown>)
        .filter(([, value]) => value != null && String(value).trim())
        .map(([key, value]) => `${key}：${String(value)}`)
      if (parts.length) return parts.join(' / ')
    }
  } catch {
    /* ignore */
  }
  return raw
}

export interface AdminSpuPage {
  items: AdminSpuItem[]
  total: number
  page: number
  pageSize: number
}

export interface AdminCategory {
  id: string
  parentId: string
  name: string
  level: number
  sortOrder: number
  icon: string | null
  status: number
}
