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
  const userAgent = navigator.userAgent
  const browser = userAgent.match(/Edg\/([\d.]+)/)?.[1]
    ? `Microsoft Edge ${userAgent.match(/Edg\/([\d.]+)/)?.[1]}`
    : userAgent.match(/Chrome\/([\d.]+)/)?.[1]
      ? `Chrome ${userAgent.match(/Chrome\/([\d.]+)/)?.[1]}`
      : userAgent.match(/Firefox\/([\d.]+)/)?.[1]
        ? `Firefox ${userAgent.match(/Firefox\/([\d.]+)/)?.[1]}`
        : userAgent.match(/Version\/([\d.]+).*Safari/)?.[1]
          ? `Safari ${userAgent.match(/Version\/([\d.]+).*Safari/)?.[1]}`
          : 'Web browser'
  const platform = /Windows/i.test(userAgent)
    ? 'Windows'
    : /Android/i.test(userAgent)
      ? 'Android'
      : /iPhone|iPad/i.test(userAgent)
        ? 'iOS'
        : /Macintosh/i.test(userAgent)
          ? 'macOS'
          : /Linux/i.test(userAgent)
            ? 'Linux'
            : 'Unknown OS'
  return `${browser} on ${platform}`
}
