import { apiBaseUrl } from '../../../config/environment'
import { apiRequest } from '../../../core/http/api-client'
import { ApiError, type ApiEnvelope } from '../../../core/models/api'
import type {
  MerchantApplicationReceipt,
  MerchantApplicationRequest,
  ShopOverview,
  ShopStaffAccount,
  ShopSummary,
} from '../domain/merchant-models'

function withQuery(path: string, params: Record<string, string | number | undefined>): string {
  const parts: string[] = []
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === '') continue
    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  }
  return parts.length ? `${path}?${parts.join('&')}` : path
}

export interface LocalFile {
  path: string
  name: string
}

function encodeUtf8(text: string): Uint8Array {
  return new TextEncoder().encode(text)
}

function concatBytes(chunks: Uint8Array[]): ArrayBuffer {
  const total = chunks.reduce((sum, chunk) => sum + chunk.length, 0)
  const output = new Uint8Array(total)
  let offset = 0
  for (const chunk of chunks) {
    output.set(chunk, offset)
    offset += chunk.length
  }
  return output.buffer
}

function readFile(path: string): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    wx.getFileSystemManager().readFile({
      filePath: path,
      success: (result) => resolve(result.data as ArrayBuffer),
      fail: () => reject(new ApiError('读取文件失败', 0)),
    })
  })
}

function guessContentType(filename: string): string {
  const lower = filename.toLowerCase()
  if (lower.endsWith('.png')) return 'image/png'
  if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return 'image/jpeg'
  if (lower.endsWith('.pdf')) return 'application/pdf'
  return 'application/octet-stream'
}

function unwrapEnvelope<T>(statusCode: number, data: unknown): T {
  const envelope = data as Partial<ApiEnvelope<T>> | null
  if (statusCode >= 200 && statusCode < 300 && envelope && envelope.code === 0) {
    return envelope.data as T
  }
  throw new ApiError(envelope?.message || '请求失败', statusCode, envelope?.code)
}

export const merchantApi = {
  async submitApplication(
    application: MerchantApplicationRequest,
    files: LocalFile[],
  ): Promise<MerchantApplicationReceipt> {
    const boundary = `----ShoppingBoundary${Date.now()}`
    const chunks: Uint8Array[] = []
    const pushText = (text: string) => chunks.push(encodeUtf8(text))

    pushText(
      `--${boundary}\r\nContent-Disposition: form-data; name="application"\r\nContent-Type: application/json\r\n\r\n${JSON.stringify(application)}\r\n`,
    )

    for (const file of files) {
      const binary = new Uint8Array(await readFile(file.path))
      pushText(
        `--${boundary}\r\nContent-Disposition: form-data; name="files"; filename="${file.name}"\r\nContent-Type: ${guessContentType(file.name)}\r\n\r\n`,
      )
      chunks.push(binary)
      pushText('\r\n')
    }
    pushText(`--${boundary}--\r\n`)

    return new Promise((resolve, reject) => {
      wx.request({
        url: `${apiBaseUrl()}/api/merchant/applications`,
        method: 'POST',
        header: { 'content-type': `multipart/form-data; boundary=${boundary}` },
        data: concatBytes(chunks),
        success: (response) => {
          try {
            resolve(unwrapEnvelope<MerchantApplicationReceipt>(response.statusCode, response.data))
          } catch (error) {
            reject(error)
          }
        },
        fail: () => reject(new ApiError('无法连接服务器，请检查网络和 API 地址', 0)),
      })
    })
  },

  currentShop(): Promise<ShopSummary> {
    return apiRequest({ path: '/api/merchant/shop' })
  },

  listStaff(): Promise<ShopStaffAccount[]> {
    return apiRequest({ path: '/api/merchant/staff' })
  },

  createStaff(payload: { email: string; displayName: string; username?: string }): Promise<ShopStaffAccount> {
    return apiRequest({ path: '/api/merchant/staff', method: 'POST', data: payload })
  },

  retryStaffEmail(staffId: string): Promise<ShopStaffAccount> {
    return apiRequest({ path: `/api/merchant/staff/${staffId}/credential-email/retry`, method: 'POST' })
  },

  statisticsOverview(startAt: string, endAt: string): Promise<ShopOverview> {
    return apiRequest({
      path: withQuery('/api/merchant/statistics/overview', {
        startAt,
        endAt,
        timezone: 'Asia/Shanghai',
        granularity: 'DAY',
      }),
    })
  },
}
