import request from './request'
import type { MenuNode, UserInfo } from '@/stores/auth'

export interface CaptchaVo {
  captchaId: string
  image: string
}

export interface LoginPayload {
  captchaId: string
  captchaCode: string
  username: string
  password: string
}

export interface LoginResult {
  token: string
  mustChangePassword: boolean
  user: UserInfo
}

export interface MeResult {
  user: UserInfo
  perms: string[]
  menus: MenuNode[]
  mustChangePassword: boolean
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export const fetchCaptcha = () => request<CaptchaVo>({ url: '/auth/captcha', method: 'get' })

export const login = (data: LoginPayload) => request<LoginResult>({ url: '/auth/login', method: 'post', data })

export const logout = () => request<void>({ url: '/auth/logout', method: 'post' })

export const fetchMe = () => request<MeResult>({ url: '/auth/me', method: 'get' })

export const changePassword = (data: ChangePasswordPayload) =>
  request<void>({ url: '/auth/password', method: 'put', data })
