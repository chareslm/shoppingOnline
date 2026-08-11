import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { userApi } from '../../features/account/data/user-api'

Page({
  data: {
    addressId: 0,
    recipientName: '',
    recipientPhone: '',
    region: [] as string[],
    regionText: '',
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    detailAddress: '',
    postalCode: '',
    isDefault: false,
    loading: false,
    error: '',
  },

  async onLoad(options: Record<string, string | undefined>) {
    const addressId = Number(options.id ?? 0)
    if (!Number.isSafeInteger(addressId) || addressId <= 0) return
    this.setData({ addressId, loading: true })
    try {
      const addresses = await userApi.addresses()
      const address = addresses.find((item) => item.id === addressId)
      if (!address) throw new ApiError('地址不存在', 404, 40401)
      const region = [address.provinceName, address.cityName, address.districtName]
      this.setData({
        recipientName: address.recipientName,
        recipientPhone: address.recipientPhone,
        region,
        regionText: region.join(' '),
        provinceCode: address.provinceCode ?? '',
        cityCode: address.cityCode ?? '',
        districtCode: address.districtCode ?? '',
        detailAddress: address.detailAddress,
        postalCode: address.postalCode ?? '',
        isDefault: address.isDefault,
      })
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  onNameInput(event: { detail: { value: string } }) { this.setData({ recipientName: event.detail.value }) },
  onPhoneInput(event: { detail: { value: string } }) { this.setData({ recipientPhone: event.detail.value }) },
  onDetailInput(event: { detail: { value: string } }) { this.setData({ detailAddress: event.detail.value }) },
  onPostalInput(event: { detail: { value: string } }) { this.setData({ postalCode: event.detail.value }) },
  onDefaultChange(event: { detail: { value: boolean } }) { this.setData({ isDefault: event.detail.value }) },
  onRegionChange(event: { detail: { value: string[]; code?: string[] } }) {
    const [provinceName = '', cityName = '', districtName = ''] = event.detail.value
    const [provinceCode = '', cityCode = '', districtCode = ''] = event.detail.code ?? []
    this.setData({
      region: event.detail.value,
      regionText: event.detail.value.join(' '),
      provinceCode,
      cityCode,
      districtCode,
    })
    void provinceName
    void cityName
    void districtName
  },

  async save() {
    const [provinceName = '', cityName = '', districtName = ''] = this.data.region
    if (
      !this.data.recipientName.trim() ||
      !this.data.recipientPhone.trim() ||
      !provinceName ||
      !this.data.detailAddress.trim()
    ) {
      this.setData({ error: '请完整填写收货人、手机号、省市区和详细地址' })
      return
    }
    this.setData({ loading: true, error: '' })
    const input = {
      recipientName: this.data.recipientName.trim(),
      recipientPhone: this.data.recipientPhone.trim(),
      provinceCode: this.data.provinceCode,
      provinceName,
      cityCode: this.data.cityCode,
      cityName,
      districtCode: this.data.districtCode,
      districtName,
      detailAddress: this.data.detailAddress.trim(),
      postalCode: this.data.postalCode.trim(),
      isDefault: this.data.isDefault,
    }
    try {
      if (this.data.addressId > 0) {
        await userApi.updateAddress(this.data.addressId, input)
      } else {
        await userApi.createAddress(input)
      }
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 500)
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  handleError(error: unknown) {
    this.setData({ error: errorMessage(error) })
    if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
  },
})
