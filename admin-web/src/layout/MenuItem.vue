<script setup lang="ts">
import { computed } from 'vue'
import type { MenuNode } from '@/stores/auth'

defineOptions({ name: 'MenuItem' })

const props = defineProps<{ menu: MenuNode }>()

const children = computed(() => (props.menu.children || []).filter(item => item.menuType !== 2))
</script>

<template>
  <el-sub-menu v-if="children.length > 0" :index="menu.path || String(menu.id)">
    <template #title>
      <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
      <span>{{ menu.menuName }}</span>
    </template>
    <MenuItem v-for="child in children" :key="child.id" :menu="child" />
  </el-sub-menu>
  <el-menu-item v-else-if="menu.path" :index="menu.path">
    <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
    <template #title>{{ menu.menuName }}</template>
  </el-menu-item>
</template>
