import { redirectToLogin } from '../../core/auth/access'
import { ApiError, errorMessage } from '../../core/models/api'
import { userApi } from '../../features/account/data/user-api'
import type { Gender } from '../../features/account/domain/user-models'

const GENDERS: Gender[] = ['UNKNOWN', 'MALE', 'FEMALE']

Page({
  data: {
    nickname: '',
    realName: '',
    avatarUrl: '',
    genderLabels: ['未设置', '男', '女'],
    genderIndex: 0,
    birthday: '',
    bio: '',
    loading: false,
    error: '',
  },

  async onLoad() {
    await this.loadProfile()
  },

  async loadProfile() {
    this.setData({ loading: true, error: '' })
    try {
      const profile = await userApi.profile()
      const genderIndex = Math.max(0, GENDERS.indexOf(profile.gender))
      this.setData({
        nickname: profile.nickname ?? '',
        realName: profile.realName ?? '',
        avatarUrl: profile.avatarUrl ?? '',
        genderIndex,
        birthday: profile.birthday ?? '',
        bio: profile.bio ?? '',
      })
    } catch (error) {
      this.handleError(error)
    } finally {
      this.setData({ loading: false })
    }
  },

  onNicknameInput(event: { detail: { value: string } }) { this.setData({ nickname: event.detail.value }) },
  onRealNameInput(event: { detail: { value: string } }) { this.setData({ realName: event.detail.value }) },
  onAvatarInput(event: { detail: { value: string } }) { this.setData({ avatarUrl: event.detail.value }) },
  onBioInput(event: { detail: { value: string } }) { this.setData({ bio: event.detail.value }) },
  onGenderChange(event: { detail: { value: string } }) { this.setData({ genderIndex: Number(event.detail.value) }) },
  onBirthdayChange(event: { detail: { value: string } }) { this.setData({ birthday: event.detail.value }) },

  async save() {
    this.setData({ loading: true, error: '' })
    try {
      await userApi.updateProfile({
        nickname: this.data.nickname.trim(),
        realName: this.data.realName.trim(),
        avatarUrl: this.data.avatarUrl.trim(),
        gender: GENDERS[this.data.genderIndex] ?? 'UNKNOWN',
        birthday: this.data.birthday || null,
        bio: this.data.bio.trim(),
      })
      wx.showToast({ title: '保存成功', icon: 'success' })
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

