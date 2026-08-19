<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { readApiError } from '../services/http'
import { systemApi, type SmtpSetting } from '../services/system'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const form = reactive({
  enabled: true,
  host: '',
  port: 465,
  username: '',
  password: '',
  fromAddress: '',
  smtpAuth: true,
  starttlsEnabled: false,
  currentPassword: '',
  testTo: '',
})
const meta = reactive({
  passwordConfigured: false,
  usingEnvironmentFallback: false,
})

function applySetting(setting: SmtpSetting) {
  form.enabled = setting.enabled !== false
  form.host = setting.host ?? ''
  form.port = setting.port || 465
  form.username = setting.username ?? ''
  form.password = ''
  form.fromAddress = setting.fromAddress ?? ''
  form.smtpAuth = setting.smtpAuth
  form.starttlsEnabled = setting.port === 465 || setting.port === 994 ? false : setting.starttlsEnabled
  form.currentPassword = ''
  meta.passwordConfigured = setting.passwordConfigured
  meta.usingEnvironmentFallback = setting.usingEnvironmentFallback
}

function applyExampleHost() {
  form.host = 'smtp.example.com'
  form.port = 587
  form.smtpAuth = true
  form.starttlsEnabled = true
}

watch(
  () => form.port,
  (port) => {
    if (port === 465 || port === 994) {
      form.starttlsEnabled = false
    }
    if (port === 587) {
      form.starttlsEnabled = true
    }
  },
)

async function load() {
  loading.value = true
  try {
    applySetting(await systemApi.smtp())
  } catch (error) {
    ElMessage.error(readApiError(error, 'SMTP 配置加载失败'))
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.currentPassword) {
    ElMessage.warning('请输入当前管理员密码进行二次确认')
    return
  }
  saving.value = true
  try {
    const saved = await systemApi.updateSmtp({
      enabled: form.enabled,
      host: form.host.trim() || undefined,
      port: form.port || 465,
      username: form.username.trim() || undefined,
      password: form.password.trim() || undefined,
      fromAddress: form.fromAddress.trim() || undefined,
      smtpAuth: form.smtpAuth,
      starttlsEnabled: form.starttlsEnabled,
      currentPassword: form.currentPassword,
    })
    applySetting(saved)
        ElMessage.success(form.enabled ? 'SMTP 配置已保存' : '已关闭 SMTP。新建账号不会发信，初始密码为 123456QWERqwer!@')
  } catch (error) {
    const message = readApiError(error, 'SMTP 配置保存失败')
    ElMessage.error(message === 'invalid credentials' ? '当前管理员密码错误' : message)
  } finally {
    saving.value = false
    form.currentPassword = ''
  }
}

async function testMail() {
  if (!form.currentPassword) {
    ElMessage.warning('请输入当前管理员密码进行二次确认')
    return
  }
  testing.value = true
  try {
    await systemApi.testSmtp({
      to: form.testTo.trim() || undefined,
      currentPassword: form.currentPassword,
    })
    ElMessage.success('测试邮件已发送，请检查收件箱和垃圾箱')
  } catch (error) {
    const message = readApiError(error, '测试邮件发送失败')
    ElMessage.error(message === 'invalid credentials' ? '当前管理员密码错误' : message)
  } finally {
    testing.value = false
    form.currentPassword = ''
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">SYSTEM MAIL</p>
        <h1>SMTP 配置</h1>
        <p>保存后立即用于商家开通和管理员手动建号邮件。可关闭发信：关闭后不发送邮件，新账号初始密码固定为 123456QWERqwer!@。</p>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never">
      <el-alert
        v-if="!form.enabled"
        class="smtp-alert"
        title="SMTP 已关闭"
        description="平台不会发送任何邮件。商家开通、管理员建号、客服账号的初始密码均为 123456QWERqwer!@，首次登录仍须改密。"
        type="error"
        :closable="false"
        show-icon
      />
      <el-alert
        v-if="meta.usingEnvironmentFallback"
        class="smtp-alert"
        title="当前正在使用环境变量中的 SMTP 回退配置"
        description="管理端尚未保存主机。保存后将改用数据库中的运行时配置。"
        type="info"
        :closable="false"
        show-icon
      />
      <el-alert
        class="smtp-alert"
        title="发信说明"
        description="请使用邮箱服务商提供的 SMTP 授权码，不要使用网页登录密码。发件人建议与 SMTP 账号一致。"
        type="info"
        :closable="false"
        show-icon
      />
      <el-form label-position="top" class="smtp-form" @submit.prevent="save">
        <el-form-item label="发信开关">
          <el-switch v-model="form.enabled" active-text="启用 SMTP" inactive-text="关闭 SMTP" />
        </el-form-item>
        <el-form-item label="SMTP 主机">
          <el-input v-model="form.host" placeholder="smtp.example.com" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="SMTP 账号">
          <el-input v-model="form.username" autocomplete="off" placeholder="mailer@example.com" />
        </el-form-item>
        <el-form-item :label="meta.passwordConfigured ? 'SMTP 密码（已配置，留空则保持）' : 'SMTP 密码 / 授权码'">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" placeholder="SMTP 授权码" />
        </el-form-item>
        <el-form-item label="发件人">
          <el-input v-model="form.fromAddress" placeholder="可留空，默认使用 SMTP 账号" />
        </el-form-item>
        <el-form-item label="连接选项">
          <el-checkbox v-model="form.smtpAuth">SMTP AUTH</el-checkbox>
          <el-checkbox v-model="form.starttlsEnabled" :disabled="form.port === 465 || form.port === 994">STARTTLS</el-checkbox>
        </el-form-item>
        <el-form-item label="测试收件人（可选）">
          <el-input v-model="form.testTo" placeholder="默认发到已保存的发件人" />
        </el-form-item>
        <el-form-item label="当前管理员密码">
          <el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" placeholder="用于二次确认" />
        </el-form-item>
        <el-form-item>
          <el-button @click="applyExampleHost">填入示例主机</el-button>
          <el-button type="primary" :loading="saving" native-type="submit">保存配置</el-button>
          <el-button type="success" :loading="testing" :disabled="!form.enabled" @click="testMail">发送测试邮件</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </section>
</template>
