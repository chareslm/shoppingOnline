<script setup lang="ts">
import axios from 'axios'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { merchantApi } from '@/modules/merchant/services/merchant'
import type { IdentityDocumentType, MerchantType } from '@/modules/merchant/types'
import { authApi } from '@/services/auth'
import { readApiError } from '@/services/http'
import type { ApiResponse } from '@/types/api'

const router = useRouter()
const route = useRoute()
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const accountType = ref<'user' | 'merchant'>('user')
const form = reactive({ username: '', email: '', phone: '', password: '', confirmPassword: '' })
// 商家申请不在浏览器生成账号密码；账号审核通过后由后端创建或复用统一账号。
const merchantForm = reactive({
  merchantType: 'ENTERPRISE' as MerchantType,
  shopName: '',
  subjectName: '',
  unifiedSocialCreditCode: '',
  principalName: '',
  idType: 'MAINLAND_ID_CARD' as IdentityDocumentType,
  idNumber: '',
  phone: '',
  email: '',
})
const qualificationFiles = ref<File[]>([])

onMounted(() => {
  if (route.query.account === 'merchant' || route.query.portal === 'merchant') accountType.value = 'merchant'
})

function validateUser() {
  const username = form.username.trim()
  const email = form.email.trim()
  const phone = form.phone.trim()
  if (!username && !email && !phone) return '用户名、邮箱和手机号至少填写一项'
  if (username && !/^[A-Za-z][A-Za-z0-9_]{2,63}$/.test(username)) return '用户名须以字母开头，长度 3–64，仅含字母、数字或下划线'
  if (phone && !/^1\d{10}$/.test(phone)) return '请输入正确的 11 位中国大陆手机号'
  if (form.password.length < 12 || form.password.length > 64) return '密码长度须为 12–64 个字符'
  if (new TextEncoder().encode(form.password).length > 72) return '密码的 UTF-8 编码不能超过 72 字节'
  if (!/[a-z]/.test(form.password) || !/[A-Z]/.test(form.password) || !/\d/.test(form.password) || !/[^A-Za-z0-9\s]/.test(form.password)) {
    return '密码须同时包含大写字母、小写字母、数字和特殊字符'
  }
  if (form.password !== form.confirmPassword) return '两次输入的密码不一致'
  return ''
}

function validateMerchant() {
  // 客户端校验只用于即时反馈；文件签名、大小和全部业务规则仍由后端复核。
  if (!merchantForm.shopName.trim()) return '请输入店铺名称'
  if (merchantForm.merchantType !== 'INDIVIDUAL' && !merchantForm.subjectName.trim()) return '请输入经营主体名称'
  if (merchantForm.merchantType !== 'INDIVIDUAL' && !/^[0-9A-Z]{18}$/.test(merchantForm.unifiedSocialCreditCode.trim().toUpperCase())) {
    return '请输入正确的 18 位统一社会信用代码'
  }
  if (!merchantForm.principalName.trim()) return '请输入负责人或经营者姓名'
  if (!merchantForm.idNumber.trim()) return '请输入身份凭证号码'
  if (!/^1\d{10}$/.test(merchantForm.phone.trim())) return '请输入正确的 11 位中国大陆手机号'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(merchantForm.email.trim())) return '请输入正确的邮箱地址'
  if (!qualificationFiles.value.length) return '请上传至少一份资质许可文件'
  if (qualificationFiles.value.length > 5) return '资质文件最多上传 5 份'
  const invalidFile = qualificationFiles.value.find(
    (file) => !['application/pdf', 'image/jpeg', 'image/png'].includes(file.type) || file.size > 10 * 1024 * 1024,
  )
  if (invalidFile) return `文件 ${invalidFile.name} 格式不支持或超过 10 MB`
  return ''
}

function selectFiles(event: Event) {
  qualificationFiles.value = Array.from((event.target as HTMLInputElement).files ?? [])
}

async function submit() {
  errorMessage.value = accountType.value === 'user' ? validateUser() : validateMerchant()
  if (errorMessage.value) return

  saving.value = true
  successMessage.value = ''
  try {
    if (accountType.value === 'merchant') {
      // JSON 与私有资质文件通过同一个 multipart 请求原子提交。
      const receipt = await merchantApi.submitApplication(
        {
          merchantType: merchantForm.merchantType,
          shopName: merchantForm.shopName.trim(),
          subjectName: merchantForm.subjectName.trim() || undefined,
          unifiedSocialCreditCode: merchantForm.unifiedSocialCreditCode.trim().toUpperCase() || undefined,
          responsiblePersonName: merchantForm.principalName.trim(),
          identityDocumentType: merchantForm.idType,
          identityDocumentNumber: merchantForm.idNumber.trim(),
          contactPhone: merchantForm.phone.trim(),
          contactEmail: merchantForm.email.trim().toLowerCase(),
        },
        qualificationFiles.value,
      )
      successMessage.value = `申请 ${receipt.id} 已提交。资质和账号审核完成后，系统会向申请邮箱发送开通通知。`
      return
    }

    const username = form.username.trim()
    const email = form.email.trim()
    const phone = form.phone.trim()
    await authApi.register({
      username: username || undefined,
      email: email || undefined,
      phone: phone || undefined,
      password: form.password,
    })
    await router.replace({ name: 'login', query: { registered: '1', identifier: username || email || phone } })
  } catch (error) {
    if (axios.isAxiosError<ApiResponse<unknown>>(error) && error.response?.data?.code === 40901) {
      errorMessage.value = accountType.value === 'user' ? '用户名、邮箱或手机号已被注册' : '该邮箱已有正在处理的商家申请'
    } else {
      errorMessage.value = readApiError(error, accountType.value === 'user' ? '注册失败，请稍后重试' : '申请提交失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <main class="login-page register-page">
    <section class="login-story">
      <router-link class="brand brand-light" to="/login">
        <span class="brand-mark">S</span>
        <span><strong>SHOP</strong><small>用户中心</small></span>
      </router-link>
      <div>
        <p class="eyebrow">CREATE YOUR ACCOUNT</p>
        <h1>{{ accountType === 'user' ? '一个账号，' : '让好商品，' }}<br />{{ accountType === 'user' ? '开启完整购物旅程。' : '被更多人看见。' }}</h1>
        <p>{{ accountType === 'user' ? '注册后将自动获得普通用户角色，并创建个人资料与偏好记录。' : '提交主体身份与经营资质，平台官员将依次完成资质和账号审核。审核通过前不会创建商家权限。' }}</p>
      </div>
      <div class="trust-row"><span>身份核验</span><span>材料私密</span><span>审核留痕</span></div>
    </section>
    <section class="login-panel register-panel">
      <form class="login-card register-card" @submit.prevent="submit">
        <p class="eyebrow">GET STARTED</p>
        <h2>{{ accountType === 'user' ? '注册 SHOP' : '申请商家入驻' }}</h2>
        <div class="account-type-switch" role="tablist" aria-label="注册类型">
          <button type="button" :class="{ active: accountType === 'user' }" @click="accountType = 'user'; errorMessage = ''; successMessage = ''">个人账号</button>
          <button type="button" :class="{ active: accountType === 'merchant' }" @click="accountType = 'merchant'; errorMessage = ''; successMessage = ''">商家账号</button>
        </div>

        <template v-if="accountType === 'user'">
          <p class="muted">用户名、邮箱和手机号至少填写一项</p>
          <div class="register-grid">
            <label>用户名（可选）<input v-model="form.username" autocomplete="username" maxlength="64" placeholder="字母开头，3–64 位" /></label>
            <label>手机号（可选）<input v-model="form.phone" inputmode="numeric" autocomplete="tel" maxlength="11" placeholder="11 位手机号" /></label>
            <label class="full">邮箱（可选）<input v-model="form.email" type="email" autocomplete="email" maxlength="254" placeholder="name@example.com" /></label>
            <label>密码<input v-model="form.password" type="password" autocomplete="new-password" required minlength="12" maxlength="64" placeholder="至少 12 位，包含四类字符" /></label>
            <label>确认密码<input v-model="form.confirmPassword" type="password" autocomplete="new-password" required minlength="12" maxlength="64" placeholder="再次输入密码" /></label>
          </div>
        </template>

        <template v-else>
          <p class="muted">手机号、邮箱、身份信息和资质文件均用于审核，不会公开展示</p>
          <div class="register-grid merchant-register-grid">
            <label>商家类型
              <select v-model="merchantForm.merchantType">
                <option value="ENTERPRISE">企业</option>
                <option value="SOLE_PROPRIETOR">个体工商户</option>
                <option value="INDIVIDUAL">个人商家</option>
              </select>
            </label>
            <label>店铺名称<input v-model="merchantForm.shopName" maxlength="128" placeholder="审核通过后使用的店铺名" /></label>
            <label v-if="merchantForm.merchantType !== 'INDIVIDUAL'">经营主体名称<input v-model="merchantForm.subjectName" maxlength="200" placeholder="营业执照上的主体名称" /></label>
            <label v-if="merchantForm.merchantType !== 'INDIVIDUAL'">统一社会信用代码<input v-model="merchantForm.unifiedSocialCreditCode" maxlength="18" placeholder="18 位代码" /></label>
            <label>负责人 / 经营者姓名<input v-model="merchantForm.principalName" maxlength="64" autocomplete="name" /></label>
            <label>证件类型
              <select v-model="merchantForm.idType">
                <option value="MAINLAND_ID_CARD">中国大陆居民身份证</option>
                <option value="PASSPORT">护照</option>
                <option value="OTHER">其他有效证件</option>
              </select>
            </label>
            <label class="full">证件号码<input v-model="merchantForm.idNumber" maxlength="64" autocomplete="off" /></label>
            <label>手机号<input v-model="merchantForm.phone" maxlength="11" inputmode="numeric" autocomplete="tel" /></label>
            <label>邮箱<input v-model="merchantForm.email" maxlength="254" type="email" autocomplete="email" /></label>
            <label class="full qualification-upload">
              资质许可文件
              <input type="file" multiple accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" @change="selectFiles" />
              <small>PDF/JPG/PNG，单个不超过 10 MB，最多 5 份</small>
              <span v-if="qualificationFiles.length">{{ qualificationFiles.map((file) => file.name).join('、') }}</span>
            </label>
          </div>
        </template>
        <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="notice success">{{ successMessage }}</p>
        <button v-if="!successMessage" class="primary-button wide" type="submit" :disabled="saving">{{ saving ? '正在提交…' : accountType === 'user' ? '创建账号' : '提交入驻申请' }}</button>
        <p class="login-switch">已有账号？<router-link :to="{ path: '/login', query: accountType === 'merchant' ? { portal: 'merchant' } : {} }">返回登录</router-link></p>
      </form>
    </section>
  </main>
</template>
