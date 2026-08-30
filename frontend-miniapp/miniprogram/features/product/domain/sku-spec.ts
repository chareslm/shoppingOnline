import type { Sku, SkuCreateRequest } from './product-models'
import { formatSkuAttributes } from './product-models'

export interface SpecDimension {
  id: string
  name: string
  values: string
}

export interface SkuDraft {
  key: string
  skuCode: string
  attrs: Record<string, string>
  attrText: string
  price: string
  stock: string
}

export interface SpecAxisValue {
  value: string
  selected: boolean
}

export interface SpecAxis {
  name: string
  values: SpecAxisValue[]
}

const CODE_MAP: Record<string, string> = {
  黑色: 'BLK',
  白色: 'WHT',
  原色: 'GLD',
  蓝色: 'BLU',
  标准版: 'STD',
  豪华版: 'PRO',
}

let dimSeq = 1
let skuSeq = 1

export function createDimension(name = '', values = ''): SpecDimension {
  return { id: `dim-${dimSeq++}`, name, values }
}

export function splitSpecValues(raw: string): string[] {
  return raw.split(/[,，、;；]+/).map((item) => item.trim()).filter(Boolean)
}

export function parseSkuAttributes(raw: string | null | undefined): Record<string, string> {
  if (!raw?.trim()) return {}
  try {
    const parsed = JSON.parse(raw) as unknown
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const result: Record<string, string> = {}
      for (const [key, value] of Object.entries(parsed as Record<string, unknown>)) {
        if (value != null && String(value).trim()) result[key] = String(value)
      }
      return result
    }
  } catch {
    /* 历史脏数据可能不是 JSON */
  }
  return { 规格: raw }
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
  return CODE_MAP[compact] ?? compact.replace(/GB/i, '').slice(0, 6)
}

export function suggestSkuCode(dimensions: SpecDimension[], attrs: Record<string, string>): string {
  return dimensions
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

function priceForCombo(dimensions: SpecDimension[], combo: Record<string, string>, start: number): number {
  for (const dim of dimensions) {
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

export function skuAttributesJson(dimensions: SpecDimension[], attrs: Record<string, string>): string | undefined {
  const attributes: Record<string, string> = {}
  for (const dim of dimensions) {
    const name = dim.name.trim()
    const value = attrs[dim.id]?.trim()
    if (name && value) attributes[name] = value
  }
  return Object.keys(attributes).length ? JSON.stringify(attributes) : undefined
}

export function emptySkuDraft(dimensions: SpecDimension[], price = '99.9', stock = '100'): SkuDraft {
  const attrs: Record<string, string> = {}
  for (const dim of dimensions) attrs[dim.id] = ''
  return {
    key: `sku-${skuSeq++}`,
    skuCode: '',
    attrs,
    attrText: '待填写规格',
    price,
    stock,
  }
}

export function generateSkuDrafts(
  dimensions: SpecDimension[],
  basePrice: string,
  baseStock: string,
): { skus?: SkuDraft[]; error?: string; summary?: string } {
  const named = dimensions.filter((dim) => dim.name.trim())
  if (!named.length) return { error: '请先添加规格属性，例如「颜色」「内存」「尺码」' }
  const unnamed = named.find((dim) => !splitSpecValues(dim.values).length)
  if (unnamed) return { error: `请为「${unnamed.name}」填写取值，多个值用逗号分隔` }
  const start = Number(basePrice)
  const stock = Number(baseStock)
  if (!Number.isFinite(start) || start < 0.01 || !Number.isInteger(stock) || stock < 0) {
    return { error: '生成规格前请填写有效的起步价和库存' }
  }
  const valueLists = named.map((dim) => splitSpecValues(dim.values))
  const combos = cartesian(valueLists)
  if (combos.length > 80) return { error: `组合过多（${combos.length}），请减少属性取值后再生成` }
  const used = new Set<string>()
  const skus = combos.map((values) => {
    const attrs: Record<string, string> = {}
    named.forEach((dim, index) => {
      attrs[dim.id] = values[index]
    })
    return {
      key: `sku-${skuSeq++}`,
      skuCode: uniqueSkuCode(suggestSkuCode(named, attrs), used),
      attrs,
      attrText: formatSkuAttributes(skuAttributesJson(named, attrs)),
      price: String(priceForCombo(named, attrs, start)),
      stock: String(stock),
    }
  })
  const summary = named.map((dim, index) => `${dim.name} ${valueLists[index].length} 档`).join(' × ')
  return { skus, summary: `已生成 ${skus.length} 个 SKU（${summary}）` }
}

export function toSkuCreateRequests(
  dimensions: SpecDimension[],
  drafts: SkuDraft[],
): { skus?: SkuCreateRequest[]; error?: string } {
  const used = new Set<string>()
  const skus: SkuCreateRequest[] = []
  for (const draft of drafts) {
    const price = Number(draft.price)
    const stock = Number(draft.stock)
    if (!Number.isFinite(price) || price < 0.01) return { error: '每个 SKU 的价格须大于 0' }
    if (!Number.isInteger(stock) || stock < 0) return { error: '每个 SKU 的库存须为不小于 0 的整数' }
    const attributes = skuAttributesJson(dimensions, draft.attrs)
    if (!attributes) return { error: '每个 SKU 请至少填写一个规格属性取值，可先点「按属性组合生成 SKU」' }
    skus.push({
      skuCode: uniqueSkuCode(draft.skuCode.trim() || suggestSkuCode(dimensions, draft.attrs), used),
      attributes,
      price,
      stock,
    })
  }
  if (!skus.length) return { error: '请至少生成一个 SKU' }
  return { skus }
}

export function specAxesFromSkus(skus: Sku[], selected: Record<string, string>): SpecAxis[] {
  const order: string[] = []
  const map = new Map<string, string[]>()
  for (const sku of skus) {
    const attrs = parseSkuAttributes(sku.attributes)
    for (const [name, value] of Object.entries(attrs)) {
      if (!map.has(name)) {
        map.set(name, [])
        order.push(name)
      }
      const values = map.get(name)!
      if (!values.includes(value)) values.push(value)
    }
  }
  return order.map((name) => ({
    name,
    values: (map.get(name) ?? []).map((value) => ({
      value,
      selected: selected[name] === value,
    })),
  }))
}

export function selectedAttrsFromSku(sku: Sku | undefined): Record<string, string> {
  return sku ? parseSkuAttributes(sku.attributes) : {}
}

export function matchSku(skus: Sku[], selected: Record<string, string>): Sku | undefined {
  const exact = skus.find((sku) => {
    const attrs = parseSkuAttributes(sku.attributes)
    const keys = Object.keys(attrs)
    if (!keys.length) return Object.keys(selected).length === 0
    return keys.every((key) => !selected[key] || attrs[key] === selected[key])
  })
  return exact ?? skus[0]
}
