import request from './request'

export interface MetricItem {
  appKey: string
  requests: number
  errors: number
  qps: number
  errorRate: number
  limited: number
  idempotentHits: number
  p99Millis: number
}

export interface MetricHistoryItem {
  statMinute: string
  requests: number
  errors: number
  errorRate: number
  limited: number
  idempotentHits: number
  p99Millis: number
}

export interface MetricHistoryQuery {
  appKey: string
  start: string
  end: string
}

export const fetchMetrics = () => request<MetricItem[]>({ url: '/metrics', method: 'get' })

export const fetchMetricHistory = (params: MetricHistoryQuery) =>
  request<MetricHistoryItem[]>({ url: '/metrics/history', method: 'get', params })
