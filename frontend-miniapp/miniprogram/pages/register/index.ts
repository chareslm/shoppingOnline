import { errorMessage } from '../../core/models/api'
import { authApi } from '../../features/account/data/auth-api'
import { merchantApi, type LocalFile } from '../../features/merchant/data/merchant-api'
import {
  IDENTITY_TYPE_OPTIONS,
  MERCHANT_TYPE_OPTIONS,
  type IdentityDocumentType,
  type MerchantType,
} from '../../features/merchant/domain/merchant-models'

Page({
  data: {
    accountType: 'user' as 'user' | 'merchant',
    username: '',
    password: '',
    confirmation: '',
    merchantType: 'ENTERPRISE' as MerchantType,
    merchantTypeIndex: 0,
    merchantTypeLabels: MERCHANT_TYPE_OPTIONS.map((item) => item.label),
    shopName: '',
    subjectName: '',
    unifiedSocialCreditCode: '',
    responsiblePersonName: '',
    identityDocumentType: 'MAINLAND_ID_CARD' as IdentityDocumentType,
    identityTypeIndex: 0,
    identityTypeLabels: IDENTITY_TYPE_OPTIONS.map((item) => item.label),
    identityDocumentNumber: '',
    contactPhone: '',
    contactEmail: '',
    files: [] as LocalFile[],
    loading: false,
    error: '',
    success: '',
  },

  onLoad(options: Record<string, string | undefined>) {
    if (options.account === 'merchant' || options.portal === 'merchant') {
      this.setData({ accountType: 'merchant' })
    }
  },

  selectUser() {
    this.setData({ accountType: 'user', error: '', success: '' })
  },
  selectMerchant() {
    this.setData({ accountType: 'merchant', error: '', success: '' })
  },
  onUsernameInput(event: { detail: { value: string } }) {
    this.setData({ username: event.detail.value })
  },
  onPasswordInput(event: { detail: { value: string } }) {
    this.setData({ password: event.detail.value })
  },
  onConfirmationInput(event: { detail: { value: string } }) {
    this.setData({ confirmation: event.detail.value })
  },
  onShopNameInput(event: { detail: { value: string } }) {
    this.setData({ shopName: event.detail.value })
  },
  onSubjectNameInput(event: { detail: { value: string } }) {
    this.setData({ subjectName: event.detail.value })
  },
  onCreditCodeInput(event: { detail: { value: string } }) {
    this.setData({ unifiedSocialCreditCode: event.detail.value })
  },
  onPersonInput(event: { detail: { value: string } }) {
    this.setData({ responsiblePersonName: event.detail.value })
  },
  onIdNumberInput(event: { detail: { value: string } }) {
    this.setData({ identityDocumentNumber: event.detail.value })
  },
  onPhoneInput(event: { detail: { value: string } }) {
    this.setData({ contactPhone: event.detail.value })
  },
  onEmailInput(event: { detail: { value: string } }) {
    this.setData({ contactEmail: event.detail.value })
  },
  onMerchantTypeChange(event: { detail: { value: string } }) {
    const index = Number(event.detail.value)
    const option = MERCHANT_TYPE_OPTIONS[index]
    if (!option) return
    this.setData({ merchantTypeIndex: index, merchantType: option.value })
  },
  onIdentityTypeChange(event: { detail: { value: string } }) {
    const index = Number(event.detail.value)
    const option = IDENTITY_TYPE_OPTIONS[index]
    if (!option) return
    this.setData({ identityTypeIndex: index, identityDocumentType: option.value })
  },

  chooseFiles() {
    const remain = 5 - this.data.files.length
    if (remain <= 0) {
      this.setData({ error: '资质文件最多上传 5 份' })
      return
    }
    const append = (files: LocalFile[]) => {
      this.setData({ files: [...this.data.files, ...files].slice(0, 5), error: '' })
    }
    wx.chooseMessageFile({
      count: remain,
      type: 'file',
      extension: ['pdf', 'jpg', 'jpeg', 'png'],
      success: (result) => {
        append(result.tempFiles.map((file) => ({ path: file.path, name: file.name })))
      },
      fail: () => {
        wx.chooseImage({
          count: remain,
          success: (result) => {
            append(
              result.tempFilePaths.map((path, index) => ({
                path,
                name: `image-${Date.now()}-${index}.jpg`,
              })),
            )
          },
        })
      },
    })
  },

  removeFile(event: { currentTarget: { dataset: { index?: number } } }) {
    const index = Number(event.currentTarget.dataset.index)
    this.setData({ files: this.data.files.filter((_, current) => current !== index) })
  },

  async submit() {
    if (this.data.accountType === 'merchant') {
      await this.submitMerchant()
      return
    }
    const username = this.data.username.trim()
    if (!username || !this.data.password) {
      this.setData({ error: '请输入用户名和密码' })
      return
    }
    if (this.data.password !== this.data.confirmation) {
      this.setData({ error: '两次输入的密码不一致' })
      return
    }
    this.setData({ loading: true, error: '', success: '' })
    try {
      await authApi.register({ username, password: this.data.password })
      wx.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: `/pages/login/index?identifier=${encodeURIComponent(username)}` })
      }, 500)
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ loading: false })
    }
  },

  async submitMerchant() {
    if (!this.data.shopName.trim() || !this.data.responsiblePersonName.trim()) {
      this.setData({ error: '请填写店铺名称和负责人姓名' })
      return
    }
    if (!this.data.contactPhone.trim() || !this.data.contactEmail.trim()) {
      this.setData({ error: '请填写联系电话和邮箱' })
      return
    }
    if (!this.data.identityDocumentNumber.trim()) {
      this.setData({ error: '请输入身份凭证号码' })
      return
    }
    if (this.data.files.length < 1) {
      this.setData({ error: '请选择 1–5 份资质文件' })
      return
    }
    this.setData({ loading: true, error: '', success: '' })
    try {
      const receipt = await merchantApi.submitApplication(
        {
          merchantType: this.data.merchantType,
          shopName: this.data.shopName.trim(),
          subjectName: this.data.subjectName.trim() || undefined,
          unifiedSocialCreditCode: this.data.unifiedSocialCreditCode.trim().toUpperCase() || undefined,
          responsiblePersonName: this.data.responsiblePersonName.trim(),
          identityDocumentType: this.data.identityDocumentType,
          identityDocumentNumber: this.data.identityDocumentNumber.trim(),
          contactPhone: this.data.contactPhone.trim(),
          contactEmail: this.data.contactEmail.trim().toLowerCase(),
        },
        this.data.files,
      )
      this.setData({ success: `申请 ${receipt.id} 已提交。审核通过后将向申请邮箱发送开通通知。` })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
    } finally {
      this.setData({ loading: false })
    }
  },
})
