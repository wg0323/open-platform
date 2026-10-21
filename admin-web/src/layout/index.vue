<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import MenuItem from './MenuItem.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)
const avatarText = computed(() => (auth.user?.nickname || auth.user?.username || '?').slice(0, 1))
const breadcrumbs = computed(() => {
  const titles = route.matched
    .filter(item => item.meta && item.meta.title)
    .map(item => item.meta.title as string)
  return ['首页', ...titles]
})

async function handleCommand(command: string | number | object) {
  if (command === 'password') {
    router.push('/password')
    return
  }
  if (command === 'logout') {
    await auth.signOut()
    router.replace('/login')
  }
}
</script>

<template>
  <div class="app-wrapper">
    <aside class="sidebar" :class="{ collapsed: isCollapse }">
      <div class="logo">
        <el-icon v-if="isCollapse" :size="22" color="#ffffff"><Bicycle /></el-icon>
        <span v-else>共享自行车监管平台</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          unique-opened
          router
        >
          <MenuItem v-for="menu in auth.menus" :key="menu.id" :menu="menu" />
        </el-menu>
      </el-scrollbar>
    </aside>

    <div class="main-container">
      <header class="navbar">
        <div class="navbar-left">
          <el-button text class="collapse-btn" @click="isCollapse = !isCollapse">
            <el-icon :size="18"><component :is="isCollapse ? 'Expand' : 'Fold'" /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">{{ item }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="28">{{ avatarText }}</el-avatar>
            <span class="nickname">{{ auth.user?.nickname || auth.user?.username }}</span>
            <el-icon><CaretBottom /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="app-main">
        <router-view />
      </main>
    </div>
  </div>
</template>
