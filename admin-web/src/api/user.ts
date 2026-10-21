import request from './request'

export interface UserItem {
  id: number
  username: string
  nickname: string
  enabled: boolean
  mustChangePassword: boolean
  lastLoginTime?: string | null
  createTime: string
  roleIds: number[]
  roleNames: string[]
}

export interface UserPage {
  total: number
  rows: UserItem[]
}

export interface UserCreatePayload {
  username: string
  password: string
  nickname?: string
  roleIds: number[]
}

export const pageUsers = (params: { pageNo: number; pageSize: number; keyword?: string }) =>
  request<UserPage>({ url: '/users', method: 'get', params })

export const createUser = (data: UserCreatePayload) => request<void>({ url: '/users', method: 'post', data })

export const updateUser = (id: number, data: { nickname?: string; enabled?: boolean }) =>
  request<void>({ url: `/users/${id}`, method: 'put', data })

export const deleteUser = (id: number) => request<void>({ url: `/users/${id}`, method: 'delete' })

export const resetUserPassword = (id: number, password: string) =>
  request<void>({ url: `/users/${id}/password`, method: 'put', data: { password } })

export const assignRoles = (id: number, roleIds: number[]) =>
  request<void>({ url: `/users/${id}/roles`, method: 'put', data: { roleIds } })
