import request from './request'

export interface RoleItem {
  id: number
  roleCode: string
  roleName: string
  remark?: string | null
  enabled: boolean
  userCount: number
  menuIds: number[]
}

export interface RoleCreatePayload {
  roleCode: string
  roleName: string
  remark?: string
}

export const listRoles = () => request<RoleItem[]>({ url: '/roles', method: 'get' })

export const createRole = (data: RoleCreatePayload) => request<void>({ url: '/roles', method: 'post', data })

export const updateRole = (id: number, data: { roleName?: string; remark?: string; enabled?: boolean }) =>
  request<void>({ url: `/roles/${id}`, method: 'put', data })

export const deleteRole = (id: number) => request<void>({ url: `/roles/${id}`, method: 'delete' })

export const assignMenus = (id: number, menuIds: number[]) =>
  request<void>({ url: `/roles/${id}/menus`, method: 'put', data: { menuIds } })
