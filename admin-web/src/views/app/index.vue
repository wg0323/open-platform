<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createApp,
  listApps,
  resetSecret,
  revealSecret,
  updateApp,
  type AppItem
} from '@/api/app'

const loading = ref(false)
const apps = ref<AppItem[]>([])
const keyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新建接入方')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  companyCode: '',
  companyName: '',
  qps: 10,
  ipWhitelist: ''
})

const rules: FormRules = {
  companyCode: [{ required: true, message: '请输入企业编码', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  qps: [{ required: true, message: '请输入 QPS', trigger: 'blur' }]
}

const secretDialogVisible = ref(false)
const secretValue = ref('')

const filtered = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) {
    return apps.value
  }
  return apps.value.filter(
    item =>
      item.appKey.toLowerCase().includes(value) ||
      item.companyCode.toLowerCase().includes(value) ||
      item.companyName.toLowerCase().includes(value)
  )
})

async function load() {
  loading.value = true
  try {
    apps.value = await listApps()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新建接入方'
  form.companyCode = ''
  form.companyName = ''
  form.qps = 10
  form.ipWhitelist = ''
  dialogVisible.value = true
}

function openEdit(row: AppItem) {
  editingId.value = row.id
  dialogTitle.value = '编辑接入方'
  form.companyCode = row.companyCode
  form.companyName = row.companyName
  form.qps = row.qps
  form.ipWhitelist = row.ipWhitelist || ''
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    const ipWhitelist = form.ipWhitelist.trim() || null
    if (editingId.value) {
      await updateApp(editingId.value, {
        companyName: form.companyName,
        qps: form.qps,
        ipWhitelist
      })
      ElMessage.success('已保存')
    } else {
      const created = await createApp({
        companyCode: form.companyCode,
        companyName: form.companyName,
        qps: form.qps,
        ipWhitelist
      })
      secretValue.value = created.appSecret
      secretDialogVisible.value = true
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function toggle(row: AppItem) {
  await updateApp(row.id, { enabled: !row.enabled })
  ElMessage.success(row.enabled ? '已禁用' : '已启用')
  await load()
}

async function handleReveal(row: AppItem) {
  secretValue.value = await revealSecret(row.id)
  secretDialogVisible.value = true
}

async function handleReset(row: AppItem) {
  await ElMessageBox.confirm('重置后旧密钥立即失效，确定继续？', '提示', { type: 'warning' })
  secretValue.value = await resetSecret(row.id)
  secretDialogVisible.value = true
  await load()
}

async function copySecret() {
  try {
    await navigator.clipboard.writeText(secretValue.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = secretValue.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <el-form :inline="true">
      <el-form-item label="关键字">
        <el-input v-model="keyword" placeholder="企业名称 / 编码 / appKey" clearable style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">刷新</el-button>
        <el-button @click="keyword = ''">重置</el-button>
      </el-form-item>
    </el-form>
  </div>

  <div class="page-card">
    <div class="table-toolbar">
      <span class="title">接入方管理</span>
      <el-button v-perm="'app:create'" type="primary" @click="openCreate">新建接入方</el-button>
    </div>
    <el-table v-loading="loading" :data="filtered" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="appKey" label="appKey" width="150" />
      <el-table-column prop="appSecret" label="密钥" width="130">
        <template #default="{ row }">
          <span class="mono">{{ row.appSecret }}</span>
        </template>
      </el-table-column>
      <el-table-column label="企业" min-width="180">
        <template #default="{ row }">
          {{ row.companyName }}
          <span class="muted">（{{ row.companyCode }}）</span>
        </template>
      </el-table-column>
      <el-table-column prop="qps" label="QPS" width="70" />
      <el-table-column label="IP 白名单" min-width="130">
        <template #default="{ row }">{{ row.ipWhitelist || '不限' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="262">
        <template #default="{ row }">
          <el-button v-perm="'app:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-perm="'app:update'" link type="primary" @click="toggle(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-button v-perm="'app:reveal-secret'" link type="primary" @click="handleReveal(row)">查看密钥</el-button>
          <el-button v-perm="'app:reset-secret'" link type="danger" @click="handleReset(row)">重置密钥</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无接入方" />
      </template>
    </el-table>
    <p class="muted tip">新接入方的 appSecret 只在新建或重置时明文返回一次，请通过安全渠道交付。</p>
  </div>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="企业编码" prop="companyCode">
        <el-input v-model="form.companyCode" :disabled="editingId !== null" placeholder="如 11011210" />
      </el-form-item>
      <el-form-item label="企业名称" prop="companyName">
        <el-input v-model="form.companyName" placeholder="如 演示单车企业" />
      </el-form-item>
      <el-form-item label="QPS" prop="qps">
        <el-input-number v-model="form.qps" :min="1" :max="10000" />
      </el-form-item>
      <el-form-item label="IP 白名单">
        <el-input
          v-model="form.ipWhitelist"
          type="textarea"
          :rows="2"
          placeholder="逗号分隔，支持 CIDR，留空表示不限"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="secretDialogVisible" title="appSecret" width="520px">
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="密钥仅显示一次，请立即复制并通过安全渠道交付"
    />
    <div class="secret-value">{{ secretValue }}</div>
    <template #footer>
      <el-button type="primary" @click="copySecret">复制密钥</el-button>
      <el-button @click="secretDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.title {
  font-size: 16px;
  font-weight: 600;
}

.mono {
  font-family: Consolas, Monaco, monospace;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.tip {
  margin: 12px 0 0;
}
</style>
