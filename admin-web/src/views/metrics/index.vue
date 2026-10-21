<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { listApps } from '@/api/app'
import { fetchMetricHistory, fetchMetrics, type MetricHistoryItem, type MetricItem } from '@/api/metrics'
import { useAuthStore } from '@/stores/auth'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

type MetricKey = 'requests' | 'errorRate' | 'p99Millis'

const METRIC_OPTIONS: Array<{ value: MetricKey; label: string }> = [
  { value: 'requests', label: '请求数' },
  { value: 'errorRate', label: '错误率' },
  { value: 'p99Millis', label: 'P99（ms）' }
]

const loading = ref(false)
const rows = ref<MetricItem[]>([])
const autoRefresh = ref(false)
const historyLoading = ref(false)
const history = ref<MetricHistoryItem[]>([])
const appKeys = ref<string[]>([])
const appKey = ref('')
const rangeHours = ref(1)
const metricKey = ref<MetricKey>('requests')
const chartRef = ref<HTMLDivElement>()
const authStore = useAuthStore()
let chart: ECharts | undefined
let timer: number | undefined

const appKeyOptions = computed(() =>
  appKeys.value.length > 0 ? appKeys.value : rows.value.map(item => item.appKey)
)

const totalRequests = computed(() => rows.value.reduce((sum, item) => sum + item.requests, 0))
const totalQps = computed(() => rows.value.reduce((sum, item) => sum + item.qps, 0))
const totalLimited = computed(() => rows.value.reduce((sum, item) => sum + item.limited, 0))
const avgP99 = computed(() => {
  if (rows.value.length === 0) {
    return 0
  }
  const sum = rows.value.reduce((total, item) => total + item.p99Millis, 0)
  return Math.round(sum / rows.value.length)
})

async function load() {
  loading.value = true
  try {
    rows.value = await fetchMetrics()
  } finally {
    loading.value = false
  }
}

async function loadAppKeys() {
  if (!authStore.hasPerm('app:list')) {
    return
  }
  appKeys.value = (await listApps()).map(item => item.appKey)
  if (!appKey.value && appKeys.value.length > 0) {
    appKey.value = appKeys.value[0]
  }
}

function toggleAuto(value: boolean) {
  if (value) {
    timer = window.setInterval(() => {
      load()
      loadHistory()
    }, 10000)
  } else if (timer) {
    window.clearInterval(timer)
    timer = undefined
  }
}

function formatDateTime(date: Date) {
  const pad = (value: number) => (value < 10 ? `0${value}` : `${value}`)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
    + `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function loadHistory() {
  if (!appKey.value) {
    return
  }
  historyLoading.value = true
  try {
    const end = new Date()
    const start = new Date(end.getTime() - rangeHours.value * 3600 * 1000)
    history.value = await fetchMetricHistory({
      appKey: appKey.value,
      start: formatDateTime(start),
      end: formatDateTime(end)
    })
    renderChart()
  } finally {
    historyLoading.value = false
  }
}

function renderChart() {
  if (!chart) {
    return
  }
  const isRate = metricKey.value === 'errorRate'
  const labels = history.value.map(item => item.statMinute.replace('T', ' ').slice(5, 16))
  const values = history.value.map(item =>
    isRate ? Number((item.errorRate * 100).toFixed(2)) : item[metricKey.value]
  )
  const seriesName = METRIC_OPTIONS.find(item => item.value === metricKey.value)?.label
  chart.setOption({
    grid: { left: 56, right: 64, top: 32, bottom: 36 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', boundaryGap: false, data: labels, axisLabel: { hideOverlap: true } },
    yAxis: { type: 'value', name: isRate ? '%' : '' },
    series: [{
      type: 'line',
      name: seriesName,
      data: values,
      showSymbol: false,
      areaStyle: { opacity: 0.12 }
    }]
  }, true)
}

function resizeChart() {
  chart?.resize()
}

watch(rows, value => {
  if (!appKey.value && value.length > 0) {
    appKey.value = value[0].appKey
  }
})

watch([appKey, rangeHours], loadHistory)

watch(metricKey, renderChart)

function percent(value: number) {
  return `${(value * 100).toFixed(2)}%`
}

onMounted(() => {
  load()
  loadAppKeys()
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
  }
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <div class="page-card">
    <div class="table-toolbar">
      <span class="title">指标看板</span>
      <div class="actions">
        <el-switch v-model="autoRefresh" active-text="自动刷新" @change="toggleAuto" />
        <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">平均 P99（ms）</div>
          <div class="stat-value">{{ avgP99 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">窗口内请求数</div>
          <div class="stat-value">{{ totalRequests }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">合计 QPS</div>
          <div class="stat-value">{{ totalQps.toFixed(2) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">限流拒绝</div>
          <div class="stat-value">{{ totalLimited }}</div>
        </div>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="appKey" label="appKey" min-width="170" />
      <el-table-column prop="requests" label="请求数" width="110" />
      <el-table-column label="QPS" width="110">
        <template #default="{ row }">{{ row.qps.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="错误率" width="110">
        <template #default="{ row }">{{ percent(row.errorRate) }}</template>
      </el-table-column>
      <el-table-column prop="limited" label="限流拒绝" width="110" />
      <el-table-column prop="p99Millis" label="P99（ms）" width="120" />
      <el-table-column prop="idempotentHits" label="幂等命中" width="120" />
      <template #empty>
        <el-empty description="窗口内暂无请求" />
      </template>
    </el-table>

    <div class="history-card">
      <div class="table-toolbar">
        <span class="title">历史趋势</span>
        <div class="actions">
          <el-select v-model="appKey" placeholder="选择接入方" style="width: 180px">
            <el-option v-for="key in appKeyOptions" :key="key" :label="key" :value="key" />
          </el-select>
          <el-radio-group v-model="rangeHours">
            <el-radio-button :value="1">近 1 小时</el-radio-button>
            <el-radio-button :value="6">近 6 小时</el-radio-button>
            <el-radio-button :value="24">近 24 小时</el-radio-button>
            <el-radio-button :value="168">近 7 天</el-radio-button>
          </el-radio-group>
          <el-select v-model="metricKey" style="width: 130px">
            <el-option v-for="item in METRIC_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
      </div>
      <div v-loading="historyLoading" class="chart-wrap">
        <div ref="chartRef" class="chart"></div>
        <div v-if="!historyLoading && history.length === 0" class="chart-empty">暂无历史数据</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.title {
  font-size: 16px;
  font-weight: 600;
}

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stats {
  margin-bottom: 18px;
}

.stat-card {
  padding: 16px 18px;
  background: #f7f9fc;
  border-radius: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.history-card {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #ebeef5;
}

.chart-wrap {
  position: relative;
  height: 320px;
}

.chart {
  width: 100%;
  height: 100%;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
}
</style>
