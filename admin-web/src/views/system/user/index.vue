<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  assignRoles,
  createUser,
  deleteUser,
  pageUsers,
  resetUserPassword,
  updateUser,
  type UserItem
} from '@/api/user'
import { listRoles, type RoleItem } from '@/api/role'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const loading = ref(false)
const rows = ref<UserItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const roles = ref<RoleItem[]>([])

const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const submitting = ref(false)
const createForm = reactive({
  username: '',
  password: '',
  nickname: '',
  roleIds: [] as number[]
})

const createRules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度需为 3~32 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '账号只能包含字母、数字与下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需为 8~32 位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字', trigger: 'blur' }
  ],
  roleIds: [{ required: true, type: 'array', min: 1, message: '至少选择一个角色', trigger: 'change' }]
}

const editVisible = ref(false)
const editRow = ref<UserItem | null>(null)
const editForm = reactive({ nickname: '' })

const roleVisible = ref(false)
const roleForm = reactive({ userId: 0, roleIds: [] as number[] })

const passwordVisible = ref(false)
const passwordForm = reactive({ userId: 0, username: '', password: '' })

const enabledRoles = ref<RoleItem[]>([])

async function load() {
  loading.value = true
  try {
    const data = await pageUsers({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined
    })
    rows.value = data.rows
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  roles.value = await listRoles()
  enabledRoles.value = roles.value.filter(role => role.enabled)
}

function search() {
  pageNo.value = 1
  load()
}

function resetSearch() {
  keyword.value = ''
  pageNo.value = 1
  load()
}

function openCreate() {
  createForm.username = ''
  createForm.password = ''
  createForm.nickname = ''
  createForm.roleIds = []
  createVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) {
    return
  }
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    await createUser({
      username: createForm.username,
      password: createForm.password,
      nickname: createForm.nickname || undefined,
      roleIds: createForm.roleIds
    })
    ElMessage.success('已创建，该用户首次登录需修改密码')
    createVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

function openEdit(row: UserItem) {
  editRow.value = row
  editForm.nickname = row.nickname
  editVisible.value = true
}

async function submitEdit() {
  if (!editRow.value) {
    return
  }
  await updateUser(editRow.value.id, { nickname: editForm.nickname })
  ElMessage.success('已保存')
  editVisible.value = false
  await load()
}

async function toggleEnabled(row: UserItem) {
  try {
    await updateUser(row.id, { enabled: !row.enabled })
    ElMessage.success(row.enabled ? '已停用' : '已启用')
  } finally {
    await load()
  }
}

function openRoles(row: UserItem) {
  roleForm.userId = row.id
  roleForm.roleIds = [...row.roleIds]
  roleVisible.value = true
}

async function submitRoles() {
  await assignRoles(roleForm.userId, roleForm.roleIds)
  ElMessage.success('角色已更新')
  roleVisible.value = false
  await load()
}

function openPassword(row: UserItem) {
  passwordForm.userId = row.id
  passwordForm.username = row.username
  passwordForm.password = ''
  passwordVisible.value = true
}

async function submitPassword() {
  if (passwordForm.password.length < 8) {
    ElMessage.warning('密码长度需为 8~32 位')
    return
  }
  await resetUserPassword(passwordForm.userId, passwordForm.password)
  ElMessage.success('密码已重置，该用户下次登录需修改密码')
  passwordVisible.value = false
}

async function remove(row: UserItem) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  await loadRoles()
  await load()
})
</script>

<template>
  <div class="page-card">
    <el-form :inline="true">
      <el-form-item label="关键字">
        <el-input v-model="keyword" placeholder="账号 / 昵称" clearable style="width: 220px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>
  </div>

  <div class="page-card">
    <div class="table-toolbar">
      <span class="title">用户管理</span>
      <el-button v-perm="'system:user:create'" type="primary" @click="openCreate">新建用户</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="账号" width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column label="角色" min-width="170">
        <template #default="{ row }">
          <el-tag v-for="name in row.roleNames" :key="name" size="small" class="role-tag">{{ name }}</el-tag>
          <span v-if="row.roleNames.length === 0" class="muted">未分配</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled"
            :disabled="!auth.hasPerm('system:user:update')"
            @change="toggleEnabled(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="最后登录" width="185">
        <template #default="{ row }">{{ row.lastLoginTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="292">
        <template #default="{ row }">
          <el-button v-perm="'system:user:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-perm="'system:user:assign-role'" link type="primary" @click="openRoles(row)">分配角色</el-button>
          <el-button v-perm="'system:user:reset-password'" link type="primary" @click="openPassword(row)">
            重置密码
          </el-button>
          <el-button v-perm="'system:user:delete'" link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户" />
      </template>
    </el-table>
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="search"
      />
    </div>
  </div>

  <el-dialog v-model="createVisible" title="新建用户" width="560px">
    <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
      <el-form-item label="账号" prop="username">
        <el-input v-model="createForm.username" placeholder="3~32 位字母、数字或下划线" />
      </el-form-item>
      <el-form-item label="初始密码" prop="password">
        <el-input v-model="createForm.password" type="password" show-password placeholder="8~32 位，含字母和数字" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="createForm.nickname" placeholder="留空则使用账号" />
      </el-form-item>
      <el-form-item label="角色" prop="roleIds">
        <el-select v-model="createForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
          <el-option v-for="role in enabledRoles" :key="role.id" :label="role.roleName" :value="role.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitCreate">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="editVisible" title="编辑用户" width="480px">
    <el-form label-width="80px">
      <el-form-item label="账号">
        <el-input :model-value="editRow?.username" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="editForm.nickname" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="roleVisible" title="分配角色" width="480px">
    <el-select v-model="roleForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
      <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
    </el-select>
    <template #footer>
      <el-button @click="roleVisible = false">取消</el-button>
      <el-button type="primary" @click="submitRoles">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="passwordVisible" title="重置密码" width="480px">
    <p class="muted">为「{{ passwordForm.username }}」设置新密码，该用户下次登录需修改密码。</p>
    <el-input v-model="passwordForm.password" type="password" show-password placeholder="8~32 位，含字母和数字" />
    <template #footer>
      <el-button @click="passwordVisible = false">取消</el-button>
      <el-button type="primary" @click="submitPassword">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.title {
  font-size: 16px;
  font-weight: 600;
}

.role-tag {
  margin-right: 6px;
}

.muted {
  color: #909399;
  font-size: 12px;
}
</style>
