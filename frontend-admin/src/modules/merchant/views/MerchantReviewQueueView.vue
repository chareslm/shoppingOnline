<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentChecked, Refresh } from '@element-plus/icons-vue'
import { readApiError } from '../../../services/http'
import { merchantAdminApi } from '../services/merchant'
import type { MerchantApplication } from '../types'

const props = defineProps<{ stage: 'qualification' | 'account'; embedded?: boolean }>()
const loading = ref(false)
const reviewing = ref(false)
const detailVisible = ref(false)
const applications = ref<MerchantApplication[]>([])
const selected = ref<MerchantApplication | null>(null)
const total = ref(0)
const query = reactive({ page: 1, pageSize: 20 })
const audit = reactive({ approved: true, remark: '' })

// 同一视图复用两个审核阶段；阶段决定待办状态过滤器和提交的状态机动作。
const isQualification = computed(() => props.stage === 'qualification')
const title = computed(() => (isQualification.value ? '商家资质审核' : '商家账号审核'))
const status = computed(() => (isQualification.value ? 'SUBMITTED' : 'QUALIFICATION_APPROVED'))

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

function merchantTypeLabel(type: MerchantApplication['merchantType']) {
  return { ENTERPRISE: '企业', SOLE_PROPRIETOR: '个体工商户', INDIVIDUAL: '个人商家' }[type]
}

async function loadApplications() {
  loading.value = true
  try {
    const result = await merchantAdminApi.applications({
      status: status.value,
      page: query.page,
      pageSize: query.pageSize,
    })
    applications.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(readApiError(error, '审核队列加载失败'))
  } finally {
    loading.value = false
  }
}

async function openDetail(row: MerchantApplication) {
  try {
    selected.value = await merchantAdminApi.application(row.id)
    audit.approved = true
    audit.remark = ''
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(readApiError(error, '申请详情加载失败'))
  }
}

async function submitAudit() {
  if (!selected.value) return
  if (!audit.approved && !audit.remark.trim()) {
    ElMessage.warning('驳回时必须填写原因')
    return
  }
  try {
    await ElMessageBox.confirm(
      `${audit.approved ? '通过' : '驳回'}“${selected.value.shopName}”的${isQualification.value ? '资质' : '账号'}审核？`,
      '确认审核结论',
      { type: audit.approved ? 'success' : 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  reviewing.value = true
  try {
    const action = isQualification.value ? merchantAdminApi.qualificationAudit : merchantAdminApi.accountAudit
    await action(selected.value.id, audit.approved, audit.remark.trim())
    ElMessage.success(audit.approved ? '审核已通过' : '申请已驳回')
    detailVisible.value = false
    await loadApplications()
  } catch (error) {
    ElMessage.error(readApiError(error, '审核提交失败'))
  } finally {
    reviewing.value = false
  }
}

async function retryEmail(row: MerchantApplication) {
  try {
    // 邮件失败不代表账号开通回滚；这里只重试投递，不重复执行账号/店铺建档。
    await merchantAdminApi.retryCredentialEmail(row.id)
    ElMessage.success('开通邮件已重新发送')
    await loadApplications()
  } catch (error) {
    ElMessage.error(readApiError(error, '邮件重试失败'))
  }
}

watch(() => props.stage, () => {
  // 路由复用组件实例时清空旧阶段页码，并立即刷新对应审核队列。
  query.page = 1
  void loadApplications()
})
onMounted(loadApplications)
</script>

<template>
  <section>
    <div v-if="!embedded" class="page-heading">
      <div>
        <p class="eyebrow">MERCHANT ONBOARDING</p>
        <h1>{{ title }}</h1>
        <p>{{ isQualification ? '核验主体身份与资质许可文件，通过后进入账号开通审核。' : '确认账号与店铺开通；新账号将通过 SMTP 收到一次性临时密码。' }}</p>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="query.page = 1; loadApplications()">刷新审核队列</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card merchant-review-card">
      <el-table v-loading="loading" :data="applications" row-key="id" empty-text="当前没有待审核申请">
        <el-table-column prop="id" label="申请号" width="100" />
        <el-table-column label="店铺与主体" min-width="220">
          <template #default="{ row }">
            <strong>{{ row.shopName }}</strong>
            <div class="contact-line">{{ row.subjectName || row.responsiblePersonName || '个人商家' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }"><el-tag effect="plain">{{ merchantTypeLabel(row.merchantType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="联系信息" min-width="210">
          <template #default="{ row }">{{ row.contactEmail }}<div class="contact-line">{{ row.contactPhone }}</div></template>
        </el-table-column>
        <el-table-column label="提交时间" width="180"><template #default="{ row }">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column v-if="!isQualification" label="邮件" width="120">
          <template #default="{ row }">
            <el-tag :type="row.emailDeliveryStatus === 'MAIL_FAILED' ? 'danger' : 'info'" effect="plain">{{ row.emailDeliveryStatus || '待开通' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="DocumentChecked" @click="openDetail(row)">审核</el-button>
            <el-button v-if="row.emailDeliveryStatus === 'MAIL_FAILED'" link type="danger" @click="retryEmail(row)">重发邮件</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" class="table-pagination" layout="total, prev, pager, next" :total="total" @current-change="loadApplications" />
    </el-card>

    <el-drawer v-model="detailVisible" :title="title" size="min(680px, 92vw)" destroy-on-close>
      <template v-if="selected">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请号">{{ selected.id }}</el-descriptions-item>
          <el-descriptions-item label="商家类型">{{ merchantTypeLabel(selected.merchantType) }}</el-descriptions-item>
          <el-descriptions-item label="店铺名称">{{ selected.shopName }}</el-descriptions-item>
          <el-descriptions-item label="主体名称">{{ selected.subjectName || '个人商家' }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ selected.responsiblePersonName }}</el-descriptions-item>
          <el-descriptions-item label="证件号码">{{ selected.maskedIdentityDocumentNumber }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ selected.contactEmail }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ selected.contactPhone }}</el-descriptions-item>
        </el-descriptions>

        <div class="qualification-files">
          <h3>资质许可文件</h3>
          <el-empty v-if="!selected.files?.length" description="没有可用文件" :image-size="72" />
          <el-button
            v-for="file in selected.files"
            :key="file.id"
            plain
            @click="merchantAdminApi.downloadFile(selected!.id, file.id, file.originalName)"
          >
            {{ file.originalName }}
          </el-button>
        </div>

        <el-form label-position="top" class="merchant-audit-form">
          <el-form-item label="审核结论">
            <el-radio-group v-model="audit.approved">
              <el-radio-button :value="true">通过</el-radio-button>
              <el-radio-button :value="false">驳回</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="audit.approved ? '审核备注（可选）' : '驳回原因'">
            <el-input v-model="audit.remark" type="textarea" :rows="4" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitAudit">提交审核结论</el-button>
      </template>
    </el-drawer>
  </section>
</template>
