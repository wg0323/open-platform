<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const force = computed(() => route.query.force === '1')
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需为 8~32 位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function submit() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await changePassword({ ...form })
    ElMessage.success('密码已修改，请重新登录')
    auth.clear()
    router.replace('/login')
  } finally {
    loading.value = false
  }
}

function back() {
  router.back()
}
</script>

<template>
  <div class="password-page">
    <el-card class="password-card">
      <h2>修改密码</h2>
      <el-alert
        v-if="force"
        title="首次登录需修改初始密码后才能使用系统"
        type="warning"
        :closable="false"
        show-icon
        class="tip"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="8~32 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <div class="actions">
          <el-button v-if="!force" @click="back">返回</el-button>
          <el-button type="primary" :loading="loading" @click="submit">确认修改</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.password-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f0f2f5;
}

.password-card {
  width: 420px;
  padding: 8px;
}

.password-card h2 {
  margin: 0 0 16px;
  font-size: 20px;
}

.tip {
  margin-bottom: 16px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
</style>
