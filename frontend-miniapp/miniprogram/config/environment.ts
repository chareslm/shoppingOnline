export const DEVELOPMENT_API_BASE_URL_STORAGE_KEY = 'shopping.apiBaseUrl.development'

const DEFAULT_DEVELOPMENT_API_BASE_URL = 'http://127.0.0.1:8080'
const PRODUCTION_API_BASE_URL = ''

export function apiBaseUrl(): string {
  const envVersion = currentEnvVersion()
  const value =
    envVersion === 'release' ? PRODUCTION_API_BASE_URL : developmentApiBaseUrl()

  if (!value) {
    throw new Error('尚未配置小程序 API 地址')
  }
  if (envVersion === 'release' && !value.startsWith('https://')) {
    throw new Error('正式版小程序必须使用 HTTPS API 地址')
  }
  return value.replace(/\/$/, '')
}

function developmentApiBaseUrl(): string {
  try {
    const localValue = wx.getStorageSync(DEVELOPMENT_API_BASE_URL_STORAGE_KEY)
    if (typeof localValue === 'string' && localValue.trim()) {
      return localValue.trim()
    }
  } catch {
    // Storage may be unavailable while running isolated unit tests.
  }
  return DEFAULT_DEVELOPMENT_API_BASE_URL
}

function currentEnvVersion(): 'develop' | 'trial' | 'release' {
  try {
    return wx.getAccountInfoSync().miniProgram.envVersion
  } catch {
    return 'develop'
  }
}
