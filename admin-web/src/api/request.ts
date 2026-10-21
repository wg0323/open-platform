import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'sbike_admin_token'

export interface ApiResult<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

const instance = axios.create({
  baseURL: '/admin/api',
  timeout: 15000
})

instance.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  response => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== '0') {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || body.code))
    }
    return body.data as unknown as typeof response
  },
  error => {
    const status = error.response?.status
    const body = error.response?.data as ApiResult<unknown> | undefined
    if (body?.code === 'ADM-40301') {
      window.location.href = '/password?force=1'
    } else if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      } else {
        ElMessage.error(body?.message || '登录失败，请重试')
      }
    } else if (status === 403) {
      ElMessage.error('无操作权限')
    } else {
      ElMessage.error(body?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default function request<T>(config: AxiosRequestConfig): Promise<T> {
  return instance.request(config) as unknown as Promise<T>
}
