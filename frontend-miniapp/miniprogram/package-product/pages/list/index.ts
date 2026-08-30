import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { searchApi } from '../../../features/product/data/product-api'
import { resolveMediaUrl, type SearchItem } from '../../../features/product/domain/product-models'

const PAGE_SIZE = 10

Page({
  data: {
    keyword: '',
    hotWords: [] as { keyword: string; count: string }[],
    items: [] as (SearchItem & { imageUrl: string; priceText: string })[],
    page: 1,
    total: 0,
    loading: false,
    finished: false,
    error: '',
  },

  async onLoad() {
    await this.loadHotWords()
    await this.load(true)
  },

  async onPullDownRefresh() {
    await this.load(true)
    wx.stopPullDownRefresh()
  },

  async onReachBottom() {
    if (this.data.finished || this.data.loading) return
    await this.load(false)
  },

  onKeywordInput(event: { detail: { value: string } }) {
    this.setData({ keyword: event.detail.value })
  },

  applyHotWord(event: { currentTarget: { dataset: { word?: string } } }) {
    const word = event.currentTarget.dataset.word ?? ''
    this.setData({ keyword: word })
    void this.load(true)
  },

  search() {
    void this.load(true)
  },

  async loadHotWords() {
    try {
      const result = await searchApi.hotWords(8)
      this.setData({ hotWords: result.words ?? [] })
    } catch {
      /* 热词失败不阻断列表 */
    }
  },

  async load(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const result = await searchApi.search({
        keyword: this.data.keyword.trim() || undefined,
        page,
        pageSize: PAGE_SIZE,
      })
      const mapped = result.items.map((item) => ({
        ...item,
        imageUrl: resolveMediaUrl(item.mainImage),
        priceText: item.priceMin == null ? '—' : `¥${item.priceMin}`,
      }))
      const items = reset ? mapped : [...this.data.items, ...mapped]
      const total = Number(result.total)
      this.setData({
        items,
        page,
        total,
        finished: items.length >= total || mapped.length < PAGE_SIZE,
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event: { currentTarget: { dataset: { index?: number } } }) {
    const id = this.data.items[Number(event.currentTarget.dataset.index)]?.spuId
    if (!id) return
    wx.navigateTo({ url: `/package-product/pages/detail/index?id=${id}` })
  },
})
