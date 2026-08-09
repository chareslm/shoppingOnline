const DEVICE_ID_KEY = 'shopping.web.device-id'

function createDeviceId() {
  if (crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function getDeviceId() {
  const existing = localStorage.getItem(DEVICE_ID_KEY)
  if (existing) return existing

  const deviceId = createDeviceId()
  localStorage.setItem(DEVICE_ID_KEY, deviceId)
  return deviceId
}

export function getDeviceName() {
  return navigator.userAgent.slice(0, 120) || 'Customer web browser'
}
