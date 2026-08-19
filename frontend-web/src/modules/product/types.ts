// 商品模块前端类型，与 docs/api/product.md、docs/api/search.md、docs/api/review.md 契约一致。
// 后端雪花 ID 以字符串返回（避免 JS Number 精度问题），ID 字段统一 string；
// 分页 total / 计数类字段后端 Long 序列化为字符串。

// ---------- 类目 ----------

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

export interface Category {
  id: string
  parentId: string
  name: string
  level: number
  sortOrder: number
  icon: string | null
  status: number
}

// ---------- 商品 ----------

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

// 搜索返回项（后端使用 spuId 字段名）
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

// ---------- 评价 ----------

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

// ---------- 请求体 ----------

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

export interface CategoryCreateRequest {
  parentId: string
  name: string
  level: number
  sortOrder: number
  icon?: string
  status: number
}

// ---------- 状态展示辅助 ----------

export const SPU_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_AUDIT: '待审核',
  AUDIT_APPROVED: '审核通过',
  AUDIT_REJECTED: '已驳回/已收回',
  ON_SALE: '上架中',
  OFF_SALE: '已下架',
}

/** 将 SKU attributes JSON 展示为「颜色：黑 / 内存：256GB」 */
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

export const SORT_OPTIONS = [
  { value: 'DEFAULT', label: '综合' },
  { value: 'SALES_DESC', label: '销量优先' },
  { value: 'PRICE_ASC', label: '价格从低到高' },
  { value: 'PRICE_DESC', label: '价格从高到低' },
  { value: 'RATING_DESC', label: '好评优先' },
  { value: 'NEWEST', label: '最新上架' },
]
