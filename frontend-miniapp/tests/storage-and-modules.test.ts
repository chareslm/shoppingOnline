import { beforeEach, describe, expect, it } from 'vitest'
import { moduleRegistry } from '../miniprogram/app/module-registry'
import { stableDeviceId } from '../miniprogram/core/device/device'
import { apiRequest } from '../miniprogram/core/http/api-client'
import {
  apiBaseUrl,
  DEVELOPMENT_API_BASE_URL_STORAGE_KEY,
} from '../miniprogram/config/environment'
import {
  clearSession,
  readSession,
  writeSession,
} from '../miniprogram/core/storage/session-storage'

const storage = new Map<string, unknown>()

beforeEach(() => {
  storage.clear()
  Object.assign(globalThis, {
    wx: {
      getStorageSync: (key: string) => storage.get(key),
      setStorageSync: (key: string, value: unknown) => storage.set(key, value),
      removeStorageSync: (key: string) => storage.delete(key),
    },
  })
})

describe('session storage', () => {
  it('round-trips and clears tokens', () => {
    const session = {
      accessToken: 'access-for-test',
      refreshToken: 'refresh-for-test',
      expiresInSeconds: 1800,
    }
    writeSession(session)
    expect(readSession()).toEqual(session)
    clearSession()
    expect(readSession()).toBeNull()
  })

  it('rejects malformed local values', () => {
    storage.set('shopping.session.v1', { accessToken: 'missing fields' })
    expect(readSession()).toBeNull()
  })

  it('accepts expiresInSeconds serialized as a string from Long-to-string JSON', () => {
    storage.set('shopping.session.v1', {
      accessToken: 'access-for-test',
      refreshToken: 'refresh-for-test',
      expiresInSeconds: '1800',
    })
    expect(readSession()).toEqual({
      accessToken: 'access-for-test',
      refreshToken: 'refresh-for-test',
      expiresInSeconds: 1800,
    })
  })
})

describe('device identity', () => {
  it('generates one stable id per installation', () => {
    const first = stableDeviceId()
    expect(first).toMatch(/^miniapp-/)
    expect(stableDeviceId()).toBe(first)
  })
})

describe('environment config', () => {
  it('uses the repository-safe development default', () => {
    expect(apiBaseUrl()).toBe('http://127.0.0.1:8080')
  })

  it('uses a local storage override without changing source files', () => {
    storage.set(DEVELOPMENT_API_BASE_URL_STORAGE_KEY, ' http://127.0.0.1:9080/ ')
    expect(apiBaseUrl()).toBe('http://127.0.0.1:9080')
  })

  it('does not allow the development override in release builds', () => {
    storage.set(DEVELOPMENT_API_BASE_URL_STORAGE_KEY, 'http://127.0.0.1:9080')
    Object.assign(globalThis, {
      wx: {
        getStorageSync: (key: string) => storage.get(key),
        setStorageSync: (key: string, value: unknown) => storage.set(key, value),
        removeStorageSync: (key: string) => storage.delete(key),
        getAccountInfoSync: () => ({ miniProgram: { envVersion: 'release' } }),
      },
    })
    expect(() => apiBaseUrl()).toThrow('尚未配置小程序 API 地址')
  })
})

describe('module registry', () => {
  it('keeps all five ownership boundaries registered', () => {
    expect(moduleRegistry.map((module) => module.key)).toEqual([
      'account',
      'merchant',
      'product',
      'trade',
      'message',
    ])
    expect(moduleRegistry[0].pages).toContain('/pages/statistics/index')
    expect(moduleRegistry.find((module) => module.key === 'trade')?.pages).toEqual([
      '/pages/cart/index',
      '/pages/checkout/index',
      '/pages/payment/index',
      '/pages/orders/index',
      '/pages/order-detail/index',
    ])
    expect(moduleRegistry.find((module) => module.key === 'product')?.pages).toEqual([
      '/package-product/pages/list/index',
      '/package-product/pages/detail/index',
    ])
    expect(moduleRegistry.find((module) => module.key === 'merchant')?.pages).toEqual([
      '/package-merchant/pages/home/index',
      '/package-merchant/pages/products/index',
      '/package-merchant/pages/add-product/index',
      '/package-merchant/pages/staff/index',
      '/package-merchant/pages/stats/index',
      '/package-merchant/pages/inbox/index',
    ])
    expect(moduleRegistry.find((module) => module.key === 'message')?.pages).toEqual([
      '/package-message/pages/chat/index',
      '/package-message/pages/thread/index',
      '/package-message/pages/notifications/index',
    ])
  })
})

describe('portal access helpers', () => {
  it('stores portal mode and routes customer-service-only merchants to inbox', async () => {
    const { isCustomerServiceOnly, portalHomePath, readPortalMode, writePortalMode } =
      await import('../miniprogram/core/auth/access')
    writePortalMode('merchant')
    expect(readPortalMode()).toBe('merchant')
    expect(isCustomerServiceOnly(['CUSTOMER_SERVICE'])).toBe(true)
    expect(portalHomePath('merchant', ['CUSTOMER_SERVICE'])).toBe('/package-merchant/pages/inbox/index')
    expect(portalHomePath('merchant', ['MERCHANT_OWNER'])).toBe('/package-merchant/pages/home/index')
    expect(portalHomePath('user', ['USER'])).toBe('/pages/home/index')
  })
})

describe('token refresh queue', () => {
  it('refreshes once for concurrent 401 responses and retries both requests', async () => {
    writeSession({
      accessToken: 'expired-access',
      refreshToken: 'refresh-before-rotation',
      expiresInSeconds: 1800,
    })
    let refreshCalls = 0

    Object.assign(globalThis, {
      wx: {
        getStorageSync: (key: string) => storage.get(key),
        setStorageSync: (key: string, value: unknown) => storage.set(key, value),
        removeStorageSync: (key: string) => storage.delete(key),
        getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
        request: (options: {
          url: string
          header: Record<string, string>
          success: (response: { statusCode: number; data: unknown }) => void
        }) => {
          if (options.url.endsWith('/api/auth/refresh')) {
            refreshCalls += 1
            setTimeout(
              () =>
                options.success({
                  statusCode: 200,
                  data: {
                    code: 0,
                    message: 'success',
                    data: {
                      userId: 1,
                      username: 'tester',
                      roles: ['USER'],
                      permissions: [],
                      accessToken: 'fresh-access',
                      refreshToken: 'refresh-after-rotation',
                      expiresInSeconds: 1800,
                    },
                  },
                }),
              5,
            )
            return
          }
          if (options.header.Authorization === 'Bearer expired-access') {
            options.success({
              statusCode: 401,
              data: { code: 40101, message: 'expired', data: null },
            })
            return
          }
          options.success({
            statusCode: 200,
            data: { code: 0, message: 'success', data: options.url },
          })
        },
      },
    })

    const [first, second] = await Promise.all([
      apiRequest<string>({ path: '/api/first' }),
      apiRequest<string>({ path: '/api/second' }),
    ])

    expect(refreshCalls).toBe(1)
    expect(first).toContain('/api/first')
    expect(second).toContain('/api/second')
    expect(readSession()?.accessToken).toBe('fresh-access')
    expect(readSession()?.refreshToken).toBe('refresh-after-rotation')
  })

  it('sends PUT JSON as a string so WeChat does not drop the body', async () => {
    writeSession({
      accessToken: 'access-for-test',
      refreshToken: 'refresh-for-test',
      expiresInSeconds: 1800,
    })
    let captured: { method?: string; data?: unknown } = {}

    Object.assign(globalThis, {
      wx: {
        getStorageSync: (key: string) => storage.get(key),
        setStorageSync: (key: string, value: unknown) => storage.set(key, value),
        removeStorageSync: (key: string) => storage.delete(key),
        getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
        request: (options: {
          method?: string
          data?: unknown
          success: (response: { statusCode: number; data: unknown }) => void
        }) => {
          captured = { method: options.method, data: options.data }
          options.success({
            statusCode: 200,
            data: { code: 0, message: 'success', data: null },
          })
        },
      },
    })

    await apiRequest({
      path: '/api/auth/password',
      method: 'POST',
      data: { currentPassword: 'TempPassword123!', newPassword: 'NewPassword456!' },
    })

    expect(captured.method).toBe('POST')
    expect(captured.data).toBe(
      JSON.stringify({ currentPassword: 'TempPassword123!', newPassword: 'NewPassword456!' }),
    )
  })

  it('stringifies PUT bodies for WeChat', async () => {
    writeSession({
      accessToken: 'access-for-test',
      refreshToken: 'refresh-for-test',
      expiresInSeconds: 1800,
    })
    let captured: unknown
    Object.assign(globalThis, {
      wx: {
        getStorageSync: (key: string) => storage.get(key),
        setStorageSync: (key: string, value: unknown) => storage.set(key, value),
        removeStorageSync: (key: string) => storage.delete(key),
        getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
        request: (options: { data?: unknown; success: (response: { statusCode: number; data: unknown }) => void }) => {
          captured = options.data
          options.success({ statusCode: 200, data: { code: 0, message: 'success', data: null } })
        },
      },
    })
    await apiRequest({ path: '/api/users/me/profile', method: 'PUT', data: { nickname: 'Ada' } })
    expect(captured).toBe(JSON.stringify({ nickname: 'Ada' }))
  })

  it('does not refresh when change-password rejects the current password', async () => {
    writeSession({
      accessToken: 'access-for-test',
      refreshToken: 'refresh-for-test',
      expiresInSeconds: 1800,
    })
    let refreshCalls = 0

    Object.assign(globalThis, {
      wx: {
        getStorageSync: (key: string) => storage.get(key),
        setStorageSync: (key: string, value: unknown) => storage.set(key, value),
        removeStorageSync: (key: string) => storage.delete(key),
        getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }),
        request: (options: {
          url: string
          success: (response: { statusCode: number; data: unknown }) => void
        }) => {
          if (options.url.endsWith('/api/auth/refresh')) {
            refreshCalls += 1
            options.success({
              statusCode: 403,
              data: { code: 40303, message: 'password change required', data: null },
            })
            return
          }
          options.success({
            statusCode: 401,
            data: { code: 40102, message: 'invalid credentials', data: null },
          })
        },
      },
    })

    await expect(
      apiRequest({
        path: '/api/auth/password',
        method: 'POST',
        data: { currentPassword: 'wrong', newPassword: 'NewPassword456!' },
      }),
    ).rejects.toMatchObject({ message: '当前密码不正确', code: 40102 })
    expect(refreshCalls).toBe(0)
    expect(readSession()?.accessToken).toBe('access-for-test')
  })
})
