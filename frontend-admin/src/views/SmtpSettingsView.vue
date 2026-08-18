<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { readApiError } from '../services/http'
import { systemApi, type SmtpSetting } from '../services/system'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const form = reactive({
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

function apply163Defaults() {
  form.host = 'smtp.163.com'
  form.port = 994
  form.smtpAuth = true
  form.starttlsEnabled = false
  if (form.username && !form.username.includes('@')) {
    form.username = `${form.username}@163.com`
  }
  if (!form.fromAddress.trim() && form.username.includes('@')) {
    form.fromAddress = form.username.trim()
  }
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
        ElMessage.success('SMTP 配置已保存。163 请用授权码、端口 994，然后发送测试邮件确认')
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
        <p>保存后立即用于商家开通和管理员手动建号邮件。密码不会回显；留空表示保持现有密码。</p>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never">
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
        title="网易 163/126 发信说明"
        description="请使用客户端授权码，不是网页登录密码。主机填 smtp.163.com，端口填 994（SSL）。本机 Docker 下 465 常握手失败，25 常被拦截。发件人必须与 163 账号完全一致，不要勾选 STARTTLS。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form label-position="top" class="smtp-form" @submit.prevent="save">
        <el-form-item label="SMTP 主机">
          <el-input v-model="form.host" placeholder="smtp.163.com" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="SMTP 账号">
          <el-input v-model="form.username" autocomplete="off" placeholder="name@163.com" />
        </el-form-item>
        <el-form-item :label="meta.passwordConfigured ? 'SMTP 密码（已配置，留空则保持）' : 'SMTP 密码 / 授权码'">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" placeholder="163 请填授权码" />
        </el-form-item>
        <el-form-item label="发件人">
          <el-input v-model="form.fromAddress" placeholder="必须与 163 账号相同，可留空自动使用账号" />
        </el-form-item>
        <el-form-item label="连接选项">
          <el-checkbox v-model="form.smtpAuth">SMTP AUTH</el-checkbox>
          <el-checkbox v-model="form.starttlsEnabled" :disabled="form.port === 465 || form.port === 994">STARTTLS（465/994 使用 SSL，不要勾选）</el-checkbox>
        </el-form-item>
        <el-form-item label="测试收件人（可选）">
          <el-input v-model="form.testTo" placeholder="默认发到上面的 163 账号" />
        </el-form-item>
        <el-form-item label="当前管理员密码">
          <el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" placeholder="用于二次确认" />
        </el-form-item>
        <el-form-item>
          <el-button @click="apply163Defaults">填入 163 默认值</el-button>
          <el-button type="primary" :loading="saving" native-type="submit">保存配置</el-button>
          <el-button type="success" :loading="testing" @click="testMail">发送测试邮件</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </section>
</template>
