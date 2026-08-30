import { apiBaseUrl } from '../../../config/environment'
import { apiRequest } from '../../../core/http/api-client'
import { ApiError, type ApiEnvelope } from '../../../core/models/api'
import { readSession } from '../../../core/storage/session-storage'
import type {
  CategoryNode,
  Page,
  Review,
  ReviewStats,
  SearchItem,
  Sku,
  SpuCreateRequest,
  SpuDetail,
  SpuItem,
} from '../domain/product-models'

function withQuery(path: string, params: Record<string, string | number | undefined>): string {
  const parts: string[] = []
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === '') continue
    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  }
  return parts.length ? `${path}?${parts.join('&')}` : path
}

export const categoryApi = {
  tree(): Promise<CategoryNode[]> {
    return apiRequest({ path: '/api/categories/tree', authenticated: false })
  },
}

export const searchApi = {
  search(params: {
    keyword?: string
    categoryId?: string
    brand?: string
    priceMin?: number
    priceMax?: number
    sort?: string
    page: number
    pageSize: number
  }): Promise<Page<SearchItem>> {
    return apiRequest({
      path: withQuery('/api/search', params),
      authenticated: false,
    })
  },

  hotWords(limit = 10): Promise<{ words: { keyword: string; count: string }[] }> {
    return apiRequest({
      path: withQuery('/api/search/hot-words', { limit }),
      authenticated: false,
    })
  },
}

export const spuApi = {
  detail(spuId: string): Promise<SpuDetail> {
    return apiRequest({ path: `/api/spu/${spuId}`, authenticated: false })
  },

  merchantPage(params: {
    categoryId?: string
    keyword?: string
    status?: string
    shelf?: 'LISTED' | 'UNLISTED'
    page: number
    pageSize: number
  }): Promise<Page<SpuItem>> {
    return apiRequest({ path: withQuery('/api/merchant/spu/page', params) })
  },

  merchantDetail(spuId: string): Promise<SpuDetail> {
    return apiRequest({ path: `/api/merchant/spu/${spuId}` })
  },

  create(payload: SpuCreateRequest): Promise<SpuDetail> {
    return apiRequest({ path: '/api/merchant/spu', method: 'POST', data: payload })
  },

  changeStatus(spuId: string, action: 'SUBMIT' | 'PUBLISH' | 'OFF_SHELF', remark?: string): Promise<SpuItem> {
    return apiRequest({
      path: `/api/merchant/spu/${spuId}/status`,
      method: 'PUT',
      data: { action, remark },
    })
  },

  adjustStock(skuId: string, change: number, remark?: string): Promise<Sku> {
    return apiRequest({
      path: `/api/merchant/sku/${skuId}/stock`,
      method: 'PUT',
      data: { change, remark },
    })
  },
}

export const reviewApi = {
  listBySpu(spuId: string, page = 1, pageSize = 20): Promise<Page<Review>> {
    return apiRequest({
      path: withQuery(`/api/review/spu/${spuId}`, { page, pageSize }),
      authenticated: false,
    })
  },

  stats(spuId: string): Promise<ReviewStats> {
    return apiRequest({ path: `/api/review/spu/${spuId}/stats`, authenticated: false })
  },
}

export const productMediaApi = {
  upload(filePath: string): Promise<{ id: string; url: string; contentType: string }> {
    const accessToken = readSession()?.accessToken
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: `${apiBaseUrl()}/api/merchant/product-media`,
        filePath,
        name: 'file',
        header: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
        success: (response) => {
          try {
            const envelope = JSON.parse(response.data) as Partial<ApiEnvelope<{ id: string; url: string; contentType: string }>>
            if (response.statusCode >= 200 && response.statusCode < 300 && envelope.code === 0 && envelope.data) {
              resolve(envelope.data)
              return
            }
            reject(new ApiError(envelope.message || '上传失败', response.statusCode, envelope.code))
          } catch {
            reject(new ApiError('上传失败', response.statusCode))
          }
        },
        fail: () => reject(new ApiError('无法连接服务器，请检查网络和 API 地址', 0)),
      })
    })
  },
}
