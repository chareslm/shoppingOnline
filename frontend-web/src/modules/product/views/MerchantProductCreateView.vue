<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { readApiError } from '@/services/http'
import { mediaUrl } from '@/utils/media'
import { merchantApi } from '@/modules/merchant/services/merchant'
import { categoryApi, productMediaApi, spuApi } from '../services/product'
import type { CategoryNode } from '../types'

const router = useRouter()
const categories = ref<CategoryNode[]>([])
const message = ref('')
const isError = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const shopName = ref('')
const createForm = reactive({
  categoryId: '',
  brand: '',
  name: '',
  subtitle: '',
  mainImage: '',
  detail: '',
})

interface SpecDimension {
  id: string
  name: string
  values: string
}

interface SkuDraft {
  skuCode: string
  attrs: Record<string, string>
  price: number
  stock: number
}

const ATTR_PRESETS: { name: string; values: string }[] = [
  { name: '颜色', values: '黑色,白色,原色' },
  { name: '内存', values: '128GB,256GB,512GB' },
  { name: '尺码', values: 'S,M,L,XL' },
  { name: '口味', values: '原味,微辣,麻辣' },
  { name: '容量', values: '250ml,500ml,1L' },
  { name: '版本', values: '标准版,豪华版' },
  { name: '材质', values: '棉,涤纶' },
]

let dimSeq = 1

function createDimension(name = '', values = ''): SpecDimension {
  return { id: `dim-${dimSeq++}`, name, values }
}

function emptySku(dims: SpecDimension[] = dimensions.value): SkuDraft {
  const attrs: Record<string, string> = {}
  for (const dim of dims) attrs[dim.id] = ''
  return { skuCode: '', attrs, price: 99.9, stock: 100 }
}

const detailImages = ref<string[]>([])
const dimensions = ref<SpecDimension[]>([
  createDimension('颜色', '黑色,白色,原色'),
  createDimension('内存', '128GB,256GB,512GB'),
])
const basePrice = ref(99.9)
const baseStock = ref(100)
const skus = ref<SkuDraft[]>([emptySku()])

const skuGridTemplate = computed(() => {
  const attrs = dimensions.value.map(() => 'minmax(88px, 1fr)').join(' ')
  return `minmax(110px, 1.1fr) ${attrs} 108px 88px auto`
})

function splitValues(raw: string): string[] {
  return raw.split(/[,，、;；]+/).map((item) => item.trim()).filter(Boolean)
}

function flatten(tree: CategoryNode[], depth = 0): { id: string; label: string }[] {
  const result: { id: string; label: string }[] = []
  for (const node of tree) {
    result.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}` })
    result.push(...flatten(node.children, depth + 1))
  }
  return result
}

const categoryOptions = computed(() => flatten(categories.value))

onMounted(async () => {
  try {
    categories.value = await categoryApi.tree()
  } catch (error) {
    message.value = readApiError(error, '类目加载失败')
    isError.value = true
  }
  try {
    shopName.value = (await merchantApi.currentShop()).name
  } catch {
    /* 创建接口仍绑定本店；店铺名仅用于展示 */
  }
})

function addDimension(preset?: { name: string; values: string }) {
  if (preset) {
    const existing = dimensions.value.find((dim) => dim.name.trim() === preset.name)
    if (existing) {
      const current = splitValues(existing.values)
      for (const value of splitValues(preset.values)) {
        if (!current.includes(value)) current.push(value)
      }
      existing.values = current.join(',')
      return
    }
    dimensions.value.push(createDimension(preset.name, preset.values))
    syncSkuAttrs()
    return
  }
  dimensions.value.push(createDimension())
  syncSkuAttrs()
}

function syncSkuAttrs() {
  for (const sku of skus.value) {
    for (const dim of dimensions.value) {
      if (sku.attrs[dim.id] == null) sku.attrs[dim.id] = ''
    }
  }
}

function removeDimension(index: number) {
  const [removed] = dimensions.value.splice(index, 1)
  if (!removed) return
  for (const sku of skus.value) {
    delete sku.attrs[removed.id]
  }
}

function addSku() {
  skus.value.push(emptySku())
}

function removeSku(index: number) {
  if (skus.value.length > 1) skus.value.splice(index, 1)
}

function cartesian(lists: string[][]): string[][] {
  return lists.reduce<string[][]>((acc, list) => {
    if (!list.length) return acc
    if (!acc.length) return list.map((value) => [value])
    return acc.flatMap((prefix) => list.map((value) => [...prefix, value]))
  }, [])
}

function codePart(value: string): string {
  const compact = value.replace(/\s+/g, '')
  const mapped: Record<string, string> = {
    黑色: 'BLK', 白色: 'WHT', 原色: 'GLD', 蓝色: 'BLU',
    标准版: 'STD', 豪华版: 'PRO',
  }
  if (mapped[compact]) return mapped[compact]
  return compact.replace(/GB/i, '').slice(0, 6)
}

function suggestSkuCode(attrs: Record<string, string>): string {
  return dimensions.value
    .map((dim) => attrs[dim.id]?.trim())
    .filter(Boolean)
    .map(codePart)
    .join('-')
}

function uniqueSkuCode(base: string, used: Set<string>): string {
  const seed = base.trim() || 'SKU'
  let code = seed
  let serial = 2
  while (used.has(code.toLowerCase())) {
    code = `${seed}-${serial++}`
  }
  used.add(code.toLowerCase())
  return code
}

function priceForCombo(combo: Record<string, string>, start: number): number {
  for (const dim of dimensions.value) {
    if (!/内存|容量|存储/.test(dim.name)) continue
    const gb = Number.parseInt(combo[dim.id] ?? '', 10)
    if (!Number.isFinite(gb)) continue
    if (gb <= 128) return start
    if (gb <= 256) return start + 800
    if (gb <= 512) return start + 2000
    return start + 3200
  }
  return start
}

function generateSpecMatrix() {
  const named = dimensions.value.filter((dim) => dim.name.trim())
  if (!named.length) {
    message.value = '请先添加规格属性，例如「颜色」「内存」「尺码」'
    isError.value = true
    return
  }
  const unnamed = named.filter((dim) => !splitValues(dim.values).length)
  if (unnamed.length) {
    message.value = `请为「${unnamed[0].name}」填写取值，多个值用逗号分隔`
    isError.value = true
    return
  }
  const start = Number(basePrice.value)
  const stock = Number(baseStock.value)
  if (!Number.isFinite(start) || start < 0.01 || !Number.isInteger(stock) || stock < 0) {
    message.value = '生成规格前请填写有效的起步价和库存'
    isError.value = true
    return
  }
  const valueLists = named.map((dim) => splitValues(dim.values))
  const combos = cartesian(valueLists)
  if (combos.length > 80) {
    message.value = `组合过多（${combos.length}），请减少属性取值后再生成`
    isError.value = true
    return
  }
  const usedCodes = new Set<string>()
  skus.value = combos.map((values) => {
    const attrs: Record<string, string> = {}
    named.forEach((dim, index) => {
      attrs[dim.id] = values[index]
    })
    return {
      skuCode: uniqueSkuCode(suggestSkuCode(attrs), usedCodes),
      attrs,
      price: priceForCombo(attrs, start),
      stock,
    }
  })
  const summary = named.map((dim, index) => `${dim.name} ${valueLists[index].length} 档`).join(' × ')
  message.value = `已生成 ${skus.value.length} 个 SKU（${summary}）`
  isError.value = false
}

function applyPhoneExample() {
  dimensions.value = [
    createDimension('颜色', '黑色,白色,原色'),
    createDimension('内存', '128GB,256GB,512GB'),
  ]
  basePrice.value = 5999
  baseStock.value = 50
  if (!createForm.name.trim()) createForm.name = '参考机型 17 Pro'
  if (!createForm.brand.trim()) createForm.brand = '示例品牌'
  if (!createForm.subtitle.trim()) createForm.subtitle = '可改颜色、内存，也可换成尺码、口味等属性'
  generateSpecMatrix()
}

function toSkuAttributesJson(sku: SkuDraft): string | undefined {
  const attributes: Record<string, string> = {}
  for (const dim of dimensions.value) {
    const name = dim.name.trim()
    const value = sku.attrs[dim.id]?.trim()
    if (name && value) attributes[name] = value
  }
  return Object.keys(attributes).length ? JSON.stringify(attributes) : undefined
}

async function uploadImage(file: File | undefined) {
  if (!file) return ''
  uploading.value = true
  try {
    const uploaded = await productMediaApi.upload(file)
    return uploaded.url
  } catch (error) {
    message.value = readApiError(error, '图片上传失败，仅支持 JPEG/PNG 且不超过 5MB')
    isError.value = true
    return ''
  } finally {
    uploading.value = false
  }
}

async function onMainImage(event: Event) {
  const input = event.target as HTMLInputElement
  const url = await uploadImage(input.files?.[0])
  input.value = ''
  if (url) createForm.mainImage = url
}

async function onDetailImage(event: Event) {
  const input = event.target as HTMLInputElement
  const url = await uploadImage(input.files?.[0])
  input.value = ''
  if (url) detailImages.value.push(url)
}

function removeDetailImage(index: number) {
  detailImages.value.splice(index, 1)
}

function buildDetailHtml() {
  const text = createForm.detail.trim()
  const imageMarkup = detailImages.value.map((url) => `<img src="${url}" alt="商品详情图" />`).join('')
  if (!text && !imageMarkup) return undefined
  const escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br />')
  return `${escaped}${imageMarkup}`
}

async function submitCreate(submitForAudit: boolean) {
  if (!createForm.name.trim() || !createForm.categoryId) {
    message.value = '请填写商品名称并选择类目'
    isError.value = true
    return
  }
  const skusPayload = []
  const usedCodes = new Set<string>()
  for (const sku of skus.value) {
    const price = Number(sku.price)
    const stock = Number(sku.stock)
    if (!Number.isFinite(price) || price < 0.01) {
      message.value = '每个 SKU 的价格须大于 0'
      isError.value = true
      return
    }
    if (!Number.isFinite(stock) || stock < 0 || !Number.isInteger(stock)) {
      message.value = '每个 SKU 的库存须为不小于 0 的整数'
      isError.value = true
      return
    }
    const attributes = toSkuAttributesJson(sku)
    if (!attributes) {
      message.value = '每个 SKU 请至少填写一个规格属性取值'
      isError.value = true
      return
    }
    const skuCode = uniqueSkuCode(sku.skuCode.trim() || suggestSkuCode(sku.attrs), usedCodes)
    skusPayload.push({
      skuCode,
      attributes,
      price,
      stock,
    })
  }
  submitting.value = true
  try {
    const created = await spuApi.create({
      categoryId: String(createForm.categoryId),
      brand: createForm.brand.trim() || undefined,
      name: createForm.name.trim(),
      subtitle: createForm.subtitle.trim() || undefined,
      mainImage: createForm.mainImage.trim() || undefined,
      images: detailImages.value.length ? detailImages.value : undefined,
      detail: buildDetailHtml(),
      skus: skusPayload,
    })
    if (submitForAudit) {
      await spuApi.changeStatus(created.id, 'SUBMIT')
    }
    await router.push({
      name: 'merchant-product-browse',
      query: { created: submitForAudit ? 'submitted' : '1' },
    })
  } catch (error) {
    message.value = readApiError(error, '创建失败，请确认已开通店铺')
    isError.value = true
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">MERCHANT CATALOG</p>
        <h1>添加商品</h1>
        <p>规格属性可自定义：颜色、内存、尺码、口味等都可以增删改名，再按取值组合生成 SKU。{{ shopName ? `将发布到店铺「${shopName}」。` : '商品归属当前登录商家的已开通店铺。' }}</p>
      </div>
    </div>
    <p v-if="message" :class="['notice', isError ? 'error' : 'success']">{{ message }}</p>
    <div class="section-card form-card">
      <div class="form-grid">
        <label>类目
          <select v-model="createForm.categoryId">
            <option value="" disabled>请选择类目</option>
            <option v-for="option in categoryOptions" :key="option.id" :value="option.id">{{ option.label }}</option>
          </select>
        </label>
        <label>品牌<input v-model="createForm.brand" placeholder="可选" /></label>
        <label class="full">商品名称<input v-model="createForm.name" placeholder="商品名称" /></label>
        <label>副标题<input v-model="createForm.subtitle" /></label>
        <label class="full">主图
          <input type="file" accept="image/jpeg,image/png" :disabled="uploading" @change="onMainImage" />
          <small class="muted">JPEG / PNG，最大 5MB</small>
        </label>
        <div v-if="createForm.mainImage" class="full image-preview">
          <img :src="mediaUrl(createForm.mainImage)" alt="主图预览" />
          <button class="text-button" type="button" @click="createForm.mainImage = ''">移除主图</button>
        </div>
        <label class="full">图文详情<textarea v-model="createForm.detail" rows="4" placeholder="文字介绍，可再插入详情图" /></label>
        <label class="full">详情图片
          <input type="file" accept="image/jpeg,image/png" :disabled="uploading" @change="onDetailImage" />
        </label>
        <div v-if="detailImages.length" class="full detail-thumbs">
          <div v-for="(url, index) in detailImages" :key="url" class="thumb">
            <img :src="mediaUrl(url)" alt="详情图" />
            <button class="text-button" type="button" @click="removeDetailImage(index)">移除</button>
          </div>
        </div>
      </div>

      <h3 class="sku-title">规格属性</h3>
      <p class="muted spec-hint">
        属性名可改成任意字段（如颜色、内存、尺码）。取值用逗号分隔。点下方常用属性可快速添加。
      </p>
      <div class="chip-row">
        <span class="muted">常用属性</span>
        <button
          v-for="preset in ATTR_PRESETS"
          :key="preset.name"
          class="chip"
          type="button"
          @click="addDimension(preset)"
        >{{ preset.name }}</button>
      </div>
      <div v-for="(dim, index) in dimensions" :key="dim.id" class="dim-row">
        <label>属性名
          <input v-model="dim.name" placeholder="如 颜色 / 尺码 / 口味" />
        </label>
        <label class="dim-values">取值（逗号分隔）
          <input v-model="dim.values" placeholder="如 黑色,白色 或 S,M,L" />
        </label>
        <button class="icon-button" type="button" :disabled="dimensions.length <= 1" @click="removeDimension(index)">−</button>
      </div>
      <div class="spec-actions">
        <button class="secondary-button" type="button" @click="addDimension()">＋ 自定义属性</button>
        <label class="inline-field">起步价
          <input v-model.number="basePrice" type="number" min="0.01" step="0.01" />
        </label>
        <label class="inline-field">每档库存
          <input v-model.number="baseStock" type="number" min="0" step="1" />
        </label>
        <button class="secondary-button" type="button" @click="generateSpecMatrix">按属性组合生成 SKU</button>
        <button class="secondary-button" type="button" @click="applyPhoneExample">填入手机参考示例</button>
      </div>

      <h3 class="sku-title">SKU 与库存</h3>
      <div class="sku-form-row sku-head">
        <span>SKU 编码</span>
        <span v-for="dim in dimensions" :key="dim.id">{{ dim.name.trim() || '未命名属性' }}</span>
        <span>价格</span>
        <span>库存</span>
        <span></span>
      </div>
      <div v-for="(sku, index) in skus" :key="index" class="sku-form-row">
        <input v-model="sku.skuCode" :placeholder="suggestSkuCode(sku.attrs) || '自动生成'" />
        <input
          v-for="dim in dimensions"
          :key="dim.id"
          v-model="sku.attrs[dim.id]"
          :placeholder="dim.name.trim() ? `${dim.name}取值` : '取值'"
          :list="`values-${dim.id}`"
        />
        <input v-model.number="sku.price" type="number" min="0.01" step="0.01" placeholder="价格" />
        <input v-model.number="sku.stock" type="number" min="0" step="1" placeholder="库存" />
        <button class="icon-button" type="button" :disabled="skus.length <= 1" @click="removeSku(index)">−</button>
      </div>
      <datalist v-for="dim in dimensions" :id="`values-${dim.id}`" :key="`list-${dim.id}`">
        <option v-for="value in splitValues(dim.values)" :key="value" :value="value" />
      </datalist>
      <button class="secondary-button" type="button" @click="addSku">＋ 添加 SKU</button>
      <div class="form-actions">
        <button class="secondary-button" type="button" :disabled="submitting || uploading" @click="submitCreate(false)">保存为草稿</button>
        <button class="primary-button" type="button" :disabled="submitting || uploading" @click="submitCreate(true)">保存并提交审核</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.sku-title { margin: 28px 0 14px; }
.spec-hint { margin: 0 0 10px; line-height: 1.6; }
.spec-actions, .chip-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin: 10px 0 16px; }
.chip { border: 1px solid var(--line); background: #fff; border-radius: 999px; padding: 4px 10px; cursor: pointer; }
.dim-row {
  display: grid;
  grid-template-columns: 180px 1fr auto;
  gap: 10px;
  align-items: end;
  margin-bottom: 10px;
}
.inline-field { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--muted); }
.inline-field input { width: 110px; }
.sku-form-row {
  display: grid;
  grid-template-columns: v-bind(skuGridTemplate);
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}
.sku-head { padding: 8px 12px; background: #f4f6f4; font-size: 12px; color: var(--muted); border-radius: 10px; }
.image-preview, .detail-thumbs { display: flex; flex-wrap: wrap; gap: 14px; align-items: flex-start; }
.image-preview img, .thumb img { width: 140px; height: 140px; object-fit: cover; border-radius: 12px; border: 1px solid var(--line); }
.thumb { display: grid; gap: 6px; justify-items: start; }
@media (max-width: 900px) {
  .dim-row, .sku-form-row { grid-template-columns: 1fr 1fr; }
}
</style>
