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
    const pendingModules = moduleRegistry.filter((module) =>
      ['merchant', 'product', 'message'].includes(module.key),
    )
    expect(pendingModules.every((module) => module.pages.length === 0)).toBe(true)
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
})
