import { redirectToLogin } from '../../../core/auth/access'
import { ApiError, errorMessage } from '../../../core/models/api'
import { categoryApi, productMediaApi, spuApi } from '../../../features/product/data/product-api'
import { flattenCategories } from '../../../features/product/domain/product-models'
import {
  createDimension,
  emptySkuDraft,
  generateSkuDrafts,
  toSkuCreateRequests,
  type SkuDraft,
  type SpecDimension,
} from '../../../features/product/domain/sku-spec'

const PRESETS = [
  { name: '颜色', values: '黑色,白色,原色' },
  { name: '内存', values: '128GB,256GB,512GB' },
  { name: '尺码', values: 'S,M,L,XL' },
  { name: '口味', values: '原味,微辣,麻辣' },
]

Page({
  data: {
    categoryLabels: [] as string[],
    categoryIds: [] as string[],
    categoryIndex: 0,
    name: '',
    brand: '',
    basePrice: '99.9',
    baseStock: '100',
    mainImage: '',
    dimensions: [
      createDimension('颜色', '黑色,白色,原色'),
      createDimension('内存', '128GB,256GB,512GB'),
    ] as SpecDimension[],
    presets: PRESETS,
    skus: [] as SkuDraft[],
    uploading: false,
    submitting: false,
    error: '',
    hint: '填写规格属性后点「按属性组合生成 SKU」，再核对每个规格的价格和库存。',
  },

  async onLoad() {
    this.setData({ skus: [emptySkuDraft(this.data.dimensions, this.data.basePrice, this.data.baseStock)] })
    try {
      const tree = await categoryApi.tree()
      const options = flattenCategories(tree)
      this.setData({
        categoryLabels: options.map((item) => item.label),
        categoryIds: options.map((item) => item.id),
      })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    }
  },

  onCategoryChange(event: { detail: { value: string } }) {
    this.setData({ categoryIndex: Number(event.detail.value) })
  },
  onNameInput(event: { detail: { value: string } }) { this.setData({ name: event.detail.value }) },
  onBrandInput(event: { detail: { value: string } }) { this.setData({ brand: event.detail.value }) },
  onBasePriceInput(event: { detail: { value: string } }) { this.setData({ basePrice: event.detail.value }) },
  onBaseStockInput(event: { detail: { value: string } }) { this.setData({ baseStock: event.detail.value }) },

  onDimNameInput(event: { currentTarget: { dataset: { index?: number } }; detail: { value: string } }) {
    const index = Number(event.currentTarget.dataset.index)
    const dimensions = this.data.dimensions.map((dim, i) => (i === index ? { ...dim, name: event.detail.value } : dim))
    this.setData({ dimensions })
  },
  onDimValuesInput(event: { currentTarget: { dataset: { index?: number } }; detail: { value: string } }) {
    const index = Number(event.currentTarget.dataset.index)
    const dimensions = this.data.dimensions.map((dim, i) => (i === index ? { ...dim, values: event.detail.value } : dim))
    this.setData({ dimensions })
  },
  addDimension() {
    this.setData({ dimensions: [...this.data.dimensions, createDimension()] })
  },
  addPreset(event: { currentTarget: { dataset: { index?: number } } }) {
    const preset = PRESETS[Number(event.currentTarget.dataset.index)]
    if (!preset) return
    const existing = this.data.dimensions.find((dim) => dim.name.trim() === preset.name)
    if (existing) {
      const current = existing.values.split(/[,，、;；]+/).map((item) => item.trim()).filter(Boolean)
      for (const value of preset.values.split(',')) {
        if (!current.includes(value)) current.push(value)
      }
      existing.values = current.join(',')
      this.setData({ dimensions: this.data.dimensions })
      return
    }
    this.setData({ dimensions: [...this.data.dimensions, createDimension(preset.name, preset.values)] })
  },
  removeDimension(event: { currentTarget: { dataset: { index?: number } } }) {
    const index = Number(event.currentTarget.dataset.index)
    if (this.data.dimensions.length <= 1) return
    this.setData({ dimensions: this.data.dimensions.filter((_, i) => i !== index) })
  },

  generateSkus() {
    const result = generateSkuDrafts(this.data.dimensions, this.data.basePrice, this.data.baseStock)
    if (result.error || !result.skus) {
      this.setData({ error: result.error ?? '无法生成 SKU' })
      return
    }
    this.setData({ skus: result.skus, error: '', hint: result.summary ?? '' })
  },
  addSku() {
    this.setData({
      skus: [...this.data.skus, emptySkuDraft(this.data.dimensions, this.data.basePrice, this.data.baseStock)],
    })
  },
  removeSku(event: { currentTarget: { dataset: { index?: number } } }) {
    const index = Number(event.currentTarget.dataset.index)
    if (this.data.skus.length <= 1) return
    this.setData({ skus: this.data.skus.filter((_, i) => i !== index) })
  },
  onSkuCodeInput(event: { currentTarget: { dataset: { index?: number } }; detail: { value: string } }) {
    this.patchSku(Number(event.currentTarget.dataset.index), { skuCode: event.detail.value })
  },
  onSkuPriceInput(event: { currentTarget: { dataset: { index?: number } }; detail: { value: string } }) {
    this.patchSku(Number(event.currentTarget.dataset.index), { price: event.detail.value })
  },
  onSkuStockInput(event: { currentTarget: { dataset: { index?: number } }; detail: { value: string } }) {
    this.patchSku(Number(event.currentTarget.dataset.index), { stock: event.detail.value })
  },
  patchSku(index: number, patch: Partial<SkuDraft>) {
    this.setData({
      skus: this.data.skus.map((sku, i) => (i === index ? { ...sku, ...patch } : sku)),
    })
  },

  async chooseImage() {
    const result = await wx.chooseImage({ count: 1 })
    const filePath = result.tempFilePaths[0]
    if (!filePath) return
    this.setData({ uploading: true, error: '' })
    try {
      const uploaded = await productMediaApi.upload(filePath)
      this.setData({ mainImage: uploaded.url })
      wx.showToast({ title: '图片已上传' })
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ uploading: false })
    }
  },

  async create(submit: boolean) {
    const categoryId = this.data.categoryIds[this.data.categoryIndex]
    const name = this.data.name.trim()
    if (!categoryId || !name) {
      this.setData({ error: '请选择类目并填写商品名称' })
      return
    }
    const built = toSkuCreateRequests(this.data.dimensions, this.data.skus)
    if (built.error || !built.skus) {
      this.setData({ error: built.error ?? '请先生成 SKU' })
      return
    }
    this.setData({ submitting: true, error: '' })
    try {
      const created = await spuApi.create({
        categoryId,
        brand: this.data.brand.trim() || undefined,
        name,
        mainImage: this.data.mainImage || undefined,
        images: this.data.mainImage ? [this.data.mainImage] : undefined,
        skus: built.skus,
      })
      if (submit) {
        await spuApi.changeStatus(created.id, 'SUBMIT')
        wx.showToast({ title: '已提交审核' })
      } else {
        wx.showToast({ title: '已保存草稿' })
      }
      setTimeout(() => wx.navigateBack(), 400)
    } catch (error) {
      this.setData({ error: errorMessage(error) })
      if (error instanceof ApiError && error.statusCode === 401) redirectToLogin()
    } finally {
      this.setData({ submitting: false })
    }
  },

  saveDraft() { void this.create(false) },
  saveAndSubmit() { void this.create(true) },
})
