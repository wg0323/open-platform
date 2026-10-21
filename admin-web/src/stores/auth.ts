import { defineStore } from 'pinia'
import { fetchMe, login as loginApi, logout as logoutApi, type LoginPayload } from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'

export interface MenuNode {
  id: number
  parentId: number
  menuName: string
  menuType: number
  path?: string | null
  component?: string | null
  perms?: string | null
  icon?: string | null
  sortNo: number
  children?: MenuNode[]
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  roleCodes: string[]
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as UserInfo | null,
    perms: [] as string[],
    menus: [] as MenuNode[],
    mustChangePassword: false,
    loaded: false
  }),
  actions: {
    async signIn(payload: LoginPayload) {
      const result = await loginApi(payload)
      this.token = result.token
      this.user = result.user
      this.mustChangePassword = result.mustChangePassword
      localStorage.setItem(TOKEN_KEY, result.token)
      return result
    },
    async loadProfile() {
      const data = await fetchMe()
      this.user = data.user
      this.perms = data.perms
      this.menus = data.menus
      this.mustChangePassword = data.mustChangePassword
      this.loaded = true
    },
    hasPerm(perm?: string | null) {
      if (!perm) {
        return true
      }
      return this.perms.includes(perm)
    },
    async signOut() {
      try {
        await logoutApi()
      } finally {
        this.clear()
      }
    },
    clear() {
      this.token = ''
      this.user = null
      this.perms = []
      this.menus = []
      this.mustChangePassword = false
      this.loaded = false
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
