import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import { useAuthStore, type MenuNode } from '@/stores/auth'
import Layout from '@/layout/index.vue'

const modules = import.meta.glob('../views/**/*.vue')

function resolveComponent(component?: string | null): () => Promise<unknown> {
  const loader = component ? modules[`../views/${component}.vue`] : undefined
  if (!loader) {
    console.warn(`[router] 未找到菜单组件: ${component}`)
    return () => import('../views/error/404.vue')
  }
  return loader
}

function flattenMenus(menus: MenuNode[]): MenuNode[] {
  const result: MenuNode[] = []
  menus.forEach(menu => {
    if (menu.menuType === 1 && menu.path) {
      result.push(menu)
    }
    if (menu.children && menu.children.length > 0) {
      result.push(...flattenMenus(menu.children))
    }
  })
  return result
}

export function registerDynamicRoutes(menus: MenuNode[]) {
  if (router.hasRoute('layout')) {
    router.removeRoute('layout')
  }
  const leaves = flattenMenus(menus)
  const children = leaves.map(menu => ({
    path: (menu.path as string).replace(/^\//, ''),
    name: `menu-${menu.id}`,
    component: resolveComponent(menu.component),
    meta: { title: menu.menuName, icon: menu.icon, perms: menu.perms }
  })) as unknown as RouteRecordRaw[]
  router.addRoute({
    path: '/',
    name: 'layout',
    component: Layout,
    redirect: leaves.length > 0 ? (leaves[0].path as string) : '/login',
    children
  })
}

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/login/index.vue') },
  { path: '/password', name: 'password', component: () => import('@/views/password/index.vue') },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/error/404.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async to => {
  NProgress.start()
  const auth = useAuthStore()
  if (to.path === '/login') {
    return true
  }
  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!auth.loaded) {
    try {
      await auth.loadProfile()
    } catch {
      auth.clear()
      return { path: '/login' }
    }
    registerDynamicRoutes(auth.menus)
    if (auth.mustChangePassword && to.path !== '/password') {
      return { path: '/password', query: { force: '1' } }
    }
    return { path: to.path, query: to.query, hash: to.hash, replace: true }
  }
  if (auth.mustChangePassword && to.path !== '/password') {
    return { path: '/password', query: { force: '1' } }
  }
  return true
})

router.afterEach(() => {
  NProgress.done()
})

export default router
