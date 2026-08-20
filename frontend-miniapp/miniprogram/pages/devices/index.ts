import { redirectToLogin } from '../../core/auth/access'
import { errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'
import type { DeviceSession } from '../../features/account/domain/auth-models'

interface DisplayDevice extends DeviceSession {
  title: string
  typeLabel: string
  lastActiveLabel: string
  expiresLabel: string
}

Page({
  data: {
    devices: [] as DisplayDevice[],
    loading: true,
    actingId: '',
    error: '',
  },

  async onShow() {
    await this.loadDevices()
  },

  async onPullDownRefresh() {
    await this.loadDevices()
    wx.stopPullDownRefresh()
  },

  async loadDevices() {
    this.setData({ loading: true, error: '' })
    try {
      const devices = await authApi.devices()
      this.setData({ devices: devices.map(toDisplayDevice) })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ loading: false })
    }
  },

  async revokeDevice(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id)
    const device = (this.data.devices as DisplayDevice[]).find((item) => item.id === id)
    if (!device) return
    const confirmed = await confirm(
      device.current ? '退出当前设备' : '退出这台设备',
      '该设备的 Refresh Token 将立即失效，无法继续刷新登录状态。',
    )
    if (!confirmed) return

    this.setData({ actingId: id, error: '' })
    try {
      await authApi.revokeDevice(id, device.current)
      if (device.current) {
        redirectToLogin()
        return
      }
      await this.loadDevices()
      wx.showToast({ title: '设备已退出', icon: 'success' })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ actingId: '' })
    }
  },

  async revokeOthers() {
    const confirmed = await confirm(
      '退出其他设备',
      '除当前设备外，其他设备的登录会话都会失效。',
    )
    if (!confirmed) return

    this.setData({ actingId: 'others', error: '' })
    try {
      await authApi.revokeOtherDevices()
      await this.loadDevices()
      wx.showToast({ title: '其他设备已退出', icon: 'success' })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ actingId: '' })
    }
  },
})

function toDisplayDevice(device: DeviceSession): DisplayDevice {
  return {
    ...device,
    title: device.deviceName || deviceLabel(device.deviceType),
    typeLabel: deviceLabel(device.deviceType),
    lastActiveLabel: formatTime(device.lastActiveAt),
    expiresLabel: device.sessionExpiresAt ? formatTime(device.sessionExpiresAt) : '无有效会话',
  }
}

function deviceLabel(type: DeviceSession['deviceType']): string {
  return ({
    WEB: '网页浏览器',
    ANDROID: 'Android App',
    MINIAPP: '微信小程序',
    ADMIN_WEB: '管理端',
  })[type]
}

function formatTime(value: string): string {
  const date = new Date(value)
  const two = (number: number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${two(date.getMonth() + 1)}-${two(date.getDate())} ${two(date.getHours())}:${two(date.getMinutes())}`
}

function confirm(title: string, content: string): Promise<boolean> {
  return new Promise((resolve) => {
    wx.showModal({
      title,
      content,
      confirmText: '确认退出',
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    })
  })
}
