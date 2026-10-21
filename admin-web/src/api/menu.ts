import request from './request'
import type { MenuNode } from '@/stores/auth'

export const fetchMenuTree = () => request<MenuNode[]>({ url: '/menus/tree', method: 'get' })
