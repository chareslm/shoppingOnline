import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { spuApi } from '../../../features/product/data/product-api'
import {
  formatSkuAttributes,
  SPU_STATUS_LABELS,
  type Sku,
  type SpuDetail,
  type SpuItem,
} from '../../../features/product/domain/product-models'

type Shelf = 'ALL' | 'LISTED' | 'UNLISTED'

Page({
  data: {
    shelf: 'ALL' as Shelf,
    items: [] as (SpuItem & { statusLabel: string; priceText: string })[],
    selected: null as SpuDetail | null,
    stockSkus: [] as (Sku & { attrText: string; draft: string })[],
    loading: false,
    error: '',
  },

  async onShow() {
    await this.loadList()
  },

  async onPullDownRefresh() {
    await this.loadList()
    wx.stopPullDownRefresh()
  },

  selectAll() { this.setShelf('ALL') },
  selectListed() { this.setShelf('LISTED') },
  selectUnlisted() { this.setShelf('UNLISTED') },

  setShelf(shelf: Shelf) {
    this.setData({ shelf })
    void this.loadList()
  },

  async loadList() {
    this.setData({ loading: true, error: '' })
    try {
      const result = await spuApi.merchantPage({
        shelf: this.data.shelf === 'ALL' ? undefined : this.data.shelf,
        page: 1,
        pageSize: 50,
      })
      this.setData({
        items: result.items.map((item) => ({
          ...item,
          statusLabel: SPU_STATUS_LABELS[item.status] ?? item.status,
          priceText: item.priceMin == null ? '—' : `¥${item.priceMin}`,
        })),
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ loading: false })
    }
  },

  async changeStatus(event: { currentTarget: { dataset: { index?: number; action?: string } } }) {
    const id = this.data.items[Number(event.currentTarget.dataset.index)]?.id
    const action = event.currentTarget.dataset.action as 'SUBMIT' | 'PUBLISH' | 'OFF_SHELF' | undefined
    if (!id || !action) return
    try {
      await spuApi.changeStatus(id, action)
      wx.showToast({ title: action === 'SUBMIT' ? '已提交' : action === 'PUBLISH' ? '已上架' : '已下架' })
      await this.loadList()
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  async openStock(event: { currentTarget: { dataset: { index?: number } } }) {
    const id = this.data.items[Number(event.currentTarget.dataset.index)]?.id
    if (!id) return
    try {
      const selected = await spuApi.merchantDetail(id)
      this.setData({
        selected,
        stockSkus: (selected.skus ?? []).map((sku) => ({
          ...sku,
          attrText: formatSkuAttributes(sku.attributes),
          draft: String(sku.availableStock),
        })),
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  onStockInput(event: { currentTarget: { dataset: { index?: number } }; detail: { value: string } }) {
    const index = Number(event.currentTarget.dataset.index)
    this.setData({
      stockSkus: this.data.stockSkus.map((sku, i) =>
        i === index ? { ...sku, draft: event.detail.value } : sku,
      ),
    })
  },

  async saveStock(event: { currentTarget: { dataset: { index?: number } } }) {
    const sku = this.data.stockSkus[Number(event.currentTarget.dataset.index)]
    const selected = this.data.selected
    if (!sku || !selected) return
    const next = Number(sku.draft)
    if (!Number.isInteger(next) || next < 0) {
      this.setData({ error: '库存须为不小于 0 的整数' })
      return
    }
    const change = next - sku.availableStock
    if (change === 0) return
    try {
      await spuApi.adjustStock(sku.id, change, '商家调整可售库存')
      wx.showToast({ title: '库存已更新' })
      const selectedIndex = this.data.items.findIndex((item) => item.id === selected.id)
      await this.openStock({ currentTarget: { dataset: { index: selectedIndex } } })
      await this.loadList()
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },
})
