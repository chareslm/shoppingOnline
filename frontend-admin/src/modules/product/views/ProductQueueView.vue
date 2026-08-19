<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { readApiError } from '../../../services/http'
import { mediaUrl } from '../../../utils/media'
import { productAdminApi } from '../services/product'
import { formatSkuAttributes, SPU_STATUS_LABELS, type AdminSpuDetail, type AdminSpuItem } from '../types'

const props = defineProps<{ status: string; emptyText: string }>()
const loading = ref(false)
const items = ref<AdminSpuItem[]>([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, pageSize: 20 })
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminSpuDetail | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await productAdminApi.page({
      keyword: query.keyword.trim() || undefined,
      status: props.status.trim() || undefined,
      page: query.page,
      pageSize: query.pageSize,
    })
    items.value = result.items
    total.value = Number(result.total)
  } catch (error) {
    ElMessage.error(readApiError(error, '商品列表加载失败'))
  } finally {
    loading.value = false
  }
}

function priceText(row: { priceMin: number | null; priceMax: number | null }) {
  if (row.priceMin == null) return '—'
  const min = `¥${Number(row.priceMin).toFixed(2)}`
  if (row.priceMax != null && Number(row.priceMax) !== Number(row.priceMin)) {
    return `${min} ~ ¥${Number(row.priceMax).toFixed(2)}`
  }
  return min
}

async function openDetail(row: AdminSpuItem) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await productAdminApi.detail(row.id)
  } catch (error) {
    ElMessage.error(readApiError(error, '商品详情加载失败'))
    drawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function audit(row: AdminSpuItem | AdminSpuDetail, result: 'APPROVE' | 'REJECT' | 'REVOKE') {
  const titles = { APPROVE: '通过并上架', REJECT: '驳回', REVOKE: '收回审核' }
  let remark: string | undefined
  try {
    if (result === 'REJECT' || result === 'REVOKE') {
      const prompted = await ElMessageBox.prompt(`确认对「${row.name}」执行「${titles[result]}」？可填写原因。`, '商品审核', {
        type: 'warning',
        inputPlaceholder: '审核意见（可选）',
        inputType: 'textarea',
      })
      remark = String(prompted.value ?? '').trim() || undefined
    } else {
      await ElMessageBox.confirm(`确认通过「${row.name}」并立即上架？请先核对照片、规格、价格和详情。`, '商品审核', {
        type: 'success',
      })
    }
  } catch {
    return
  }
  try {
    await productAdminApi.audit(row.id, result, remark)
    ElMessage.success('已提交审核结论')
    drawerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error, '审核失败'))
  }
}

watch(() => props.status, () => {
  query.page = 1
  void load()
})
onMounted(load)
</script>

<template>
  <section>
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="按名称搜索" clearable @keyup.enter="query.page = 1; load()" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="query.page = 1; load()">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="items" :empty-text="emptyText">
        <el-table-column label="主图" width="88">
          <template #default="{ row }">
            <el-image
              v-if="row.mainImage"
              :src="mediaUrl(row.mainImage)"
              fit="cover"
              class="thumb"
              :preview-src-list="[mediaUrl(row.mainImage)]"
              preview-teleported
            />
            <span v-else class="muted">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品" min-width="240">
          <template #default="{ row }">
            <strong>{{ row.name }}</strong>
            <div class="contact-line">{{ row.subtitle || '无副标题' }}</div>
            <div class="contact-line">{{ row.brand || '未填品牌' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="店铺" min-width="160">
          <template #default="{ row }">{{ row.shopName || (row.shopId ? `店铺 #${row.shopId}` : '未知店铺') }}</template>
        </el-table-column>
        <el-table-column label="价格" width="170">
          <template #default="{ row }">{{ priceText(row) }}</template>
        </el-table-column>
        <el-table-column label="销量/评分" width="120">
          <template #default="{ row }">{{ row.sales }} / {{ Number(row.rating).toFixed(1) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag effect="plain">{{ SPU_STATUS_LABELS[row.status] ?? row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
            <el-button v-if="row.status === 'PENDING_AUDIT'" link type="success" @click="audit(row, 'APPROVE')">通过上架</el-button>
            <el-button v-if="row.status === 'PENDING_AUDIT'" link type="danger" @click="audit(row, 'REJECT')">驳回</el-button>
            <el-button v-if="row.status === 'ON_SALE' || row.status === 'AUDIT_APPROVED' || row.status === 'OFF_SALE'" link type="warning" @click="audit(row, 'REVOKE')">收回</el-button>
            <el-button v-if="row.status === 'AUDIT_REJECTED'" link type="success" @click="audit(row, 'APPROVE')">重新通过</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" class="table-pagination" layout="total, prev, pager, next" :total="total" @current-change="load" />
    </el-card>

    <el-drawer v-model="drawerVisible" size="640px" title="商品审核详情" destroy-on-close>
      <div v-loading="detailLoading" class="audit-detail">
        <template v-if="detail">
          <h3>主图</h3>
          <div v-if="detail.mainImage" class="gallery">
            <el-image
              :src="mediaUrl(detail.mainImage)"
              fit="cover"
              class="hero"
              :preview-src-list="[mediaUrl(detail.mainImage)]"
              preview-teleported
            />
          </div>
          <p v-else class="muted">未上传主图</p>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="商品名称" :span="2">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="副标题" :span="2">{{ detail.subtitle || '—' }}</el-descriptions-item>
            <el-descriptions-item label="品牌">{{ detail.brand || '—' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ SPU_STATUS_LABELS[detail.status] ?? detail.status }}</el-descriptions-item>
            <el-descriptions-item label="店铺">{{ detail.shopName || (detail.shopId ? `店铺 #${detail.shopId}` : '未知店铺') }}</el-descriptions-item>
            <el-descriptions-item label="类目">{{ detail.categoryName || detail.categoryId }}</el-descriptions-item>
            <el-descriptions-item label="价格">{{ priceText(detail) }}</el-descriptions-item>
            <el-descriptions-item label="销量 / 评分">{{ detail.sales }} / {{ Number(detail.rating).toFixed(1) }}</el-descriptions-item>
            <el-descriptions-item label="提交时间" :span="2">{{ detail.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="上次审核意见" :span="2">{{ detail.auditRemark || '—' }}</el-descriptions-item>
          </el-descriptions>
          <h3>SKU</h3>
          <el-table :data="detail.skus" size="small">
            <el-table-column label="编码" min-width="120">
              <template #default="{ row }">{{ row.skuCode || row.id }}</template>
            </el-table-column>
            <el-table-column label="规格" min-width="180">
              <template #default="{ row }">{{ formatSkuAttributes(row.attributes, '—') }}</template>
            </el-table-column>
            <el-table-column label="价格" width="110">
              <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="availableStock" label="可售" width="80" />
            <el-table-column prop="reservedStock" label="预占" width="80" />
            <el-table-column prop="soldStock" label="已售" width="80" />
          </el-table>
          <h3>图文详情</h3>
          <div v-if="detail.images.length" class="gallery">
            <el-image
              v-for="url in detail.images"
              :key="url"
              :src="mediaUrl(url)"
              fit="cover"
              class="detail-thumb"
              :preview-src-list="detail.images.map(mediaUrl)"
              preview-teleported
            />
          </div>
          <div v-if="detail.detail" class="rich-detail" v-html="detail.detail" />
          <p v-if="!detail.images.length && !detail.detail" class="muted">商家未填写图文详情</p>
          <div class="audit-actions">
            <el-button v-if="detail.status === 'PENDING_AUDIT'" type="success" @click="audit(detail, 'APPROVE')">通过上架</el-button>
            <el-button v-if="detail.status === 'PENDING_AUDIT'" type="danger" @click="audit(detail, 'REJECT')">驳回</el-button>
            <el-button v-if="detail.status === 'ON_SALE' || detail.status === 'AUDIT_APPROVED' || detail.status === 'OFF_SALE'" type="warning" @click="audit(detail, 'REVOKE')">收回</el-button>
            <el-button v-if="detail.status === 'AUDIT_REJECTED'" type="success" @click="audit(detail, 'APPROVE')">重新通过</el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.thumb { width: 56px; height: 56px; border-radius: 8px; }
.hero { width: 160px; height: 160px; border-radius: 12px; }
.detail-thumb { width: 120px; height: 120px; border-radius: 10px; }
.gallery { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.contact-line { color: var(--el-text-color-secondary); font-size: 12px; margin-top: 4px; }
.muted { color: var(--el-text-color-secondary); }
.audit-detail h3 { margin: 20px 0 10px; font-size: 15px; }
.rich-detail { border: 1px solid var(--el-border-color); border-radius: 8px; padding: 12px; max-height: 280px; overflow: auto; }
.rich-detail :deep(img) { max-width: 100%; }
.audit-actions { display: flex; gap: 8px; margin-top: 20px; }
</style>
