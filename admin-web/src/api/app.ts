import request from './request'

export interface AppItem {
  id: number
  appKey: string
  appSecret: string
  companyCode: string
  companyName: string
  ipWhitelist?: string | null
  qps: number
  enabled: boolean
  createTime: string
}

export interface AppPayload {
  companyCode: string
  companyName: string
  ipWhitelist?: string | null
  qps: number
}

export interface AppUpdatePayload {
  companyName?: string
  ipWhitelist?: string | null
  qps?: number
  enabled?: boolean
}

export const listApps = () => request<AppItem[]>({ url: '/apps', method: 'get' })

export const createApp = (data: AppPayload) => request<AppItem>({ url: '/apps', method: 'post', data })

export const updateApp = (id: number, data: AppUpdatePayload) =>
  request<AppItem>({ url: `/apps/${id}`, method: 'put', data })

export const resetSecret = (id: number) => request<string>({ url: `/apps/${id}/reset-secret`, method: 'post' })

export const revealSecret = (id: number) => request<string>({ url: `/apps/${id}/secret`, method: 'get' })
