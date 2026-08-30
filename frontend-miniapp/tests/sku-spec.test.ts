import { describe, expect, it } from 'vitest'
import {
  createDimension,
  generateSkuDrafts,
  matchSku,
  parseSkuAttributes,
  specAxesFromSkus,
  toSkuCreateRequests,
} from '../miniprogram/features/product/domain/sku-spec'
import type { Sku } from '../miniprogram/features/product/domain/product-models'

function sku(id: string, attributes: string, price = 10): Sku {
  return {
    id,
    spuId: '1',
    skuCode: id,
    attributes,
    image: null,
    price,
    availableStock: 5,
    reservedStock: 0,
    soldStock: 0,
    status: 1,
  }
}

describe('sku spec helpers', () => {
  it('generates a cartesian SKU matrix with attribute JSON', () => {
    const dimensions = [
      createDimension('颜色', '黑色,白色'),
      createDimension('内存', '128GB,256GB'),
    ]
    const result = generateSkuDrafts(dimensions, '5999', '20')
    expect(result.error).toBeUndefined()
    expect(result.skus).toHaveLength(4)
    const payload = toSkuCreateRequests(dimensions, result.skus!)
    expect(payload.error).toBeUndefined()
    expect(payload.skus).toHaveLength(4)
    expect(payload.skus?.[0].attributes).toContain('颜色')
    expect(payload.skus?.map((item) => item.price)).toEqual([5999, 6799, 5999, 6799])
  })

  it('builds buyer spec axes and matches the selected combination', () => {
    const skus = [
      sku('a', '{"颜色":"黑色","内存":"128GB"}', 10),
      sku('b', '{"颜色":"白色","内存":"256GB"}', 20),
    ]
    const axes = specAxesFromSkus(skus, parseSkuAttributes(skus[0].attributes))
    expect(axes.map((axis) => axis.name)).toEqual(['颜色', '内存'])
    expect(matchSku(skus, { 颜色: '白色', 内存: '256GB' })?.id).toBe('b')
  })
})
