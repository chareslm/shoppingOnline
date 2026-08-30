import { apiBaseUrl } from '../../../config/environment'

export interface CategoryNode {
  id: string
  parentId: string
  name: string
  level: number
  sortOrder: number
  icon: string | null
  status: number
  children: CategoryNode[]
}

export interface Sku {
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

export interface SpuDetail {
  id: string
  shopId: string
  shopName: string | null
  categoryId: string
  brand: string | null
  name: string
  subtitle: string | null
  mainImage: string | null
  images: string[]
  detail: string | null
  priceMin: number | null
  priceMax: number | null
  sales: number
  rating: number
  status: string
  auditRemark: string | null
  createdAt: string
  skus: Sku[]
}

export interface SpuItem {
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

export interface SearchItem {
  spuId: string
  shopId: string
  categoryId: string
  brand: string | null
  name: string
  subtitle: string | null
  mainImage: string | null
  priceMin: number | null
  priceMax: number | null
  sales: number
  rating: number
}

export interface Page<T> {
  items: T[]
  total: string
  page: number
  pageSize: number
}

export interface Review {
  id: string
  spuId: string
  skuId: string
  userId: string
  rating: number
  content: string | null
  images: string[]
  anonymous: boolean
  createdAt: string
  reply: string | null
}

export interface ReviewStats {
  averageRating: number
  totalCount: string
  fiveStar: string
  fourStar: string
  threeStar: string
  twoStar: string
  oneStar: string
  positiveRate: number
}

export interface SpuCreateRequest {
  categoryId: string
  brand?: string
  name: string
  subtitle?: string
  mainImage?: string
  images?: string[]
  detail?: string
  skus: SkuCreateRequest[]
}

export interface SkuCreateRequest {
  skuCode?: string
  attributes?: string
  image?: string
  price: number
  stock: number
}

export const SPU_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_AUDIT: '待审核',
  AUDIT_APPROVED: '审核通过',
  AUDIT_REJECTED: '已驳回/已收回',
  ON_SALE: '上架中',
  OFF_SALE: '已下架',
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
    /* 历史脏数据可能不是 JSON */
  }
  return raw
}

export function flattenCategories(tree: CategoryNode[], depth = 0): { id: string; label: string }[] {
  const result: { id: string; label: string }[] = []
  for (const node of tree) {
    result.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}` })
    if (node.children?.length) result.push(...flattenCategories(node.children, depth + 1))
  }
  return result
}

export function resolveMediaUrl(path: string | null | undefined): string {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  const base = apiBaseUrl()
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`
}
