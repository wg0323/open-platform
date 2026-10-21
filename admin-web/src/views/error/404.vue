<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.replace('/')
}
</script>

<template>
  <el-result icon="warning" title="页面不存在" sub-title="请检查菜单配置或联系管理员">
    <template #extra>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </template>
  </el-result>
</template>
