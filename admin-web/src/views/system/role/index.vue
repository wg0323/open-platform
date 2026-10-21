<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance, type FormRules } from 'element-plus'
import {
  assignMenus,
  createRole,
  deleteRole,
  listRoles,
  updateRole,
  type RoleItem
} from '@/api/role'
import { fetchMenuTree } from '@/api/menu'
import type { MenuNode } from '@/stores/auth'

const loading = ref(false)
const rows = ref<RoleItem[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新建角色')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({ roleCode: '', roleName: '', remark: '', enabled: true })

const rules: FormRules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '需以大写字母开头，只含大写字母、数字与下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const drawerVisible = ref(false)
const menuTree = ref<MenuNode[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const assigning = ref<RoleItem | null>(null)
const assignSaving = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await listRoles()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新建角色'
  form.roleCode = ''
  form.roleName = ''
  form.remark = ''
  form.enabled = true
  dialogVisible.value = true
}

function openEdit(row: RoleItem) {
  editingId.value = row.id
  dialogTitle.value = '编辑角色'
  form.roleCode = row.roleCode
  form.roleName = row.roleName
  form.remark = row.remark || ''
  form.enabled = row.enabled
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
    if (editingId.value) {
      await updateRole(editingId.value, {
        roleName: form.roleName,
        remark: form.remark,
        enabled: form.enabled
      })
      ElMessage.success('已保存')
    } else {
      await createRole({ roleCode: form.roleCode, roleName: form.roleName, remark: form.remark })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function remove(row: RoleItem) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  await load()
}

async function openAssign(row: RoleItem) {
  assigning.value = row
  menuTree.value = await fetchMenuTree()
  drawerVisible.value = true
  await nextTick()
  treeRef.value?.setCheckedKeys([], false)
  row.menuIds.forEach(id => treeRef.value?.setChecked(id, true, false))
}

async function saveAssign() {
  if (!assigning.value || !treeRef.value) {
    return
  }
  assignSaving.value = true
  try {
    const keys = [
      ...treeRef.value.getCheckedKeys(),
      ...treeRef.value.getHalfCheckedKeys()
    ]
    const menuIds = Array.from(new Set(keys.map(key => Number(key))))
    await assignMenus(assigning.value.id, menuIds)
    ElMessage.success('权限已保存')
    drawerVisible.value = false
    await load()
  } finally {
    assignSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="table-toolbar">
      <span class="title">角色管理</span>
      <el-button v-perm="'system:role:create'" type="primary" @click="openCreate">新建角色</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="roleCode" label="角色编码" width="170">
        <template #default="{ row }">
          <span class="mono">{{ row.roleCode }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="roleName" label="角色名称" min-width="130" />
      <el-table-column prop="remark" label="备注" min-width="180" />
      <el-table-column prop="userCount" label="用户数" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button v-perm="'system:role:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-perm="'system:role:assign-menu'" link type="primary" @click="openAssign(row)">
            分配权限
          </el-button>
          <el-button
            v-perm="'system:role:delete'"
            link
            type="danger"
            :disabled="row.roleCode === 'SUPER_ADMIN'"
            :title="row.roleCode === 'SUPER_ADMIN' ? '超级管理员角色不可删除' : ''"
            @click="remove(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无角色" />
      </template>
    </el-table>
  </div>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="角色编码" prop="roleCode">
        <el-input v-model="form.roleCode" :disabled="editingId !== null" placeholder="如 VIEWER" />
      </el-form-item>
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="form.roleName" placeholder="如 只读查看" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item v-if="editingId !== null" label="启用状态">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="drawerVisible" :title="`分配权限 - ${assigning?.roleName || ''}`" size="480px">
    <el-tree
      ref="treeRef"
      :data="menuTree"
      node-key="id"
      show-checkbox
      default-expand-all
      :props="{ label: 'menuName', children: 'children' }"
    />
    <template #footer>
      <el-button @click="drawerVisible = false">取消</el-button>
      <el-button type="primary" :loading="assignSaving" @click="saveAssign">保存</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.title {
  font-size: 16px;
  font-weight: 600;
}

.mono {
  font-family: Consolas, Monaco, monospace;
}
</style>
