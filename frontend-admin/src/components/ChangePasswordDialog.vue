<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '../services/auth'
import { readApiError } from '../services/http'

const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ changed: [] }>()
const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

function validateNewPassword(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!value) return callback(new Error('请输入新密码'))
  if (value.length < 12 || value.length > 64) return callback(new Error('新密码长度须为 12–64 个字符'))
  if (new TextEncoder().encode(value).length > 72) return callback(new Error('新密码的 UTF-8 编码不能超过 72 字节'))
  if (!/[a-z]/.test(value) || !/[A-Z]/.test(value) || !/\d/.test(value) || !/[^A-Za-z0-9\s]/.test(value)) {
    return callback(new Error('须同时包含大写字母、小写字母、数字和特殊字符'))
  }
  if (value === form.currentPassword) return callback(new Error('新密码不能与当前密码相同'))
  callback()
}

function validateConfirmation(_rule: unknown, value: string, callback: (error?: Error) => void) {
  callback(value === form.newPassword ? undefined : new Error('两次输入的新密码不一致'))
}

const rules: FormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: ['blur', 'change'] }],
  confirmPassword: [{ validator: validateConfirmation, trigger: ['blur', 'change'] }],
}

function reset() {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await authApi.changePassword(form.currentPassword, form.newPassword)
    visible.value = false
    emit('changed')
  } catch (error) {
    const message = readApiError(error, '密码修改失败')
    ElMessage.error(message === 'invalid credentials' ? '当前密码错误' : message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="修改登录密码" width="520px" :close-on-click-modal="false" destroy-on-close @closed="reset">
    <el-alert
      title="修改成功后，所有设备上的 Refresh Token 都会失效，当前管理端也会退出并要求重新登录。"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="password-form" @submit.prevent="submit">
      <el-form-item label="当前密码" prop="currentPassword">
        <el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="submit" />
      </el-form-item>
      <p class="password-hint">至少 12 位，同时包含大写字母、小写字母、数字和特殊字符。</p>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确认修改</el-button>
    </template>
  </el-dialog>
</template>
