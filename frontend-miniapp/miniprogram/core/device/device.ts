const DEVICE_ID_KEY = 'shopping.device-id.v1'

export function stableDeviceId(): string {
  const existing: unknown = wx.getStorageSync(DEVICE_ID_KEY)
  if (typeof existing === 'string' && existing.length > 0) {
    return existing
  }
  const id = `miniapp-${Date.now().toString(36)}-${randomPart()}-${randomPart()}`
  wx.setStorageSync(DEVICE_ID_KEY, id)
  return id
}

export function deviceName(): string {
  try {
    const system = wx.getSystemInfoSync()
    return `${system.brand || 'WeChat'} ${system.model || 'Mini Program'}`.trim()
  } catch {
    return 'WeChat Mini Program'
  }
}

function randomPart(): string {
  return Math.floor(Math.random() * 0x100000000)
    .toString(16)
    .padStart(8, '0')
}

