import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { reviewApi, spuApi } from '../../../features/product/data/product-api'
import {
  formatSkuAttributes,
  resolveMediaUrl,
  SPU_STATUS_LABELS,
  type Review,
  type ReviewStats,
  type Sku,
  type SpuDetail,
} from '../../../features/product/domain/product-models'
import {
  matchSku,
  selectedAttrsFromSku,
  specAxesFromSkus,
  type SpecAxis,
} from '../../../features/product/domain/sku-spec'
import { cartApi } from '../../../features/trade/data/trade-api'

Page({
  data: {
    spuId: '',
    detail: null as SpuDetail | null,
    images: [] as string[],
    skus: [] as Sku[],
    specAxes: [] as SpecAxis[],
    selectedSku: null as (Sku & { attrText: string }) | null,
    quantity: 1,
    stats: null as ReviewStats | null,
    reviews: [] as Review[],
    statusLabel: '',
    loading: false,
    submitting: false,
    error: '',
  },

  async onLoad(options: Record<string, string | undefined>) {
    const spuId = options.id ?? ''
    this.setData({ spuId })
    if (!spuId) {
      this.setData({ error: '缺少商品编号' })
      return
    }
    await this.load()
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [detail, stats, reviewPage] = await Promise.all([
        spuApi.detail(this.data.spuId),
        reviewApi.stats(this.data.spuId),
        reviewApi.listBySpu(this.data.spuId),
      ])
      const images = [detail.mainImage, ...(detail.images ?? [])]
        .filter((url): url is string => Boolean(url))
        .map((url) => resolveMediaUrl(url))
      const skus = detail.skus ?? []
      const selected = skus[0]
      this.setData({
        detail,
        images: [...new Set(images)],
        skus,
        specAxes: specAxesFromSkus(skus, selectedAttrsFromSku(selected)),
        selectedSku: selected
          ? { ...selected, attrText: formatSkuAttributes(selected.attributes) }
          : null,
        stats,
        reviews: reviewPage.items,
        statusLabel: SPU_STATUS_LABELS[detail.status] ?? detail.status,
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ loading: false })
    }
  },

  selectSpec(event: { currentTarget: { dataset: { name?: string; value?: string } } }) {
    const name = event.currentTarget.dataset.name
    const value = event.currentTarget.dataset.value
    if (!name || !value) return
    const current = this.data.selectedSku ? selectedAttrsFromSku(this.data.selectedSku) : {}
    const next = { ...current, [name]: value }
    const sku = matchSku(this.data.skus, next)
    this.setData({
      selectedSku: sku ? { ...sku, attrText: formatSkuAttributes(sku.attributes) } : null,
      specAxes: specAxesFromSkus(this.data.skus, sku ? selectedAttrsFromSku(sku) : next),
    })
  },

  selectSkuByIndex(event: { currentTarget: { dataset: { index?: number } } }) {
    const sku = this.data.skus[Number(event.currentTarget.dataset.index)]
    if (!sku) return
    this.setData({
      selectedSku: { ...sku, attrText: formatSkuAttributes(sku.attributes) },
      specAxes: specAxesFromSkus(this.data.skus, selectedAttrsFromSku(sku)),
    })
  },

  changeQuantity(event: { currentTarget: { dataset: { delta?: string } } }) {
    const delta = Number(event.currentTarget.dataset.delta)
    this.setData({ quantity: Math.max(1, this.data.quantity + delta) })
  },

  async addToCart() {
    const sku = this.data.selectedSku
    const shopId = this.data.detail?.shopId
    if (!sku || !shopId) {
      wx.showToast({ title: '请选择规格', icon: 'none' })
      return
    }
    if (sku.availableStock < this.data.quantity) {
      wx.showToast({ title: '库存不足', icon: 'none' })
      return
    }
    this.setData({ submitting: true, error: '' })
    try {
      await cartApi.addItem({
        skuId: sku.id,
        shopId,
        price: sku.price,
        quantity: this.data.quantity,
      })
      wx.showToast({ title: '已加入购物车', icon: 'success' })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ submitting: false })
    }
  },
})
