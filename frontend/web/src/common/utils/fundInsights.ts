/** 基金展示辅助函数，集中处理金额格式化、区间走势和持仓洞察计算。 */
import type {
  FundDetail,
  PaperOrder,
  SipPlan,
  TrendPoint,
} from '@fundcat/contracts'
import type { TrendMarker } from '../charts/FundTrendChart'

export type FundRangeKey = '7d' | '1m' | '3m' | '6m' | '1y' | '3y'

export function formatAmount(value: number) {
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })
}

export function formatCurrency(value: number) {
  return `¥${formatAmount(value)}`
}

export function formatCompactPercent(value: number | null) {
  if (value == null || Number.isNaN(value)) {
    return '--'
  }
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
}

export function rangeOptions(): Array<{ key: FundRangeKey; label: string }> {
  return [
    { key: '7d', label: '近7日' },
    { key: '1m', label: '1个月' },
    { key: '3m', label: '3个月' },
    { key: '6m', label: '半年' },
    { key: '1y', label: '1年' },
    { key: '3y', label: '3年' },
  ]
}

export function filterRange(series: TrendPoint[], range: FundRangeKey) {
  if (!series.length) return []
  const lastTime = new Date(series.at(-1)!.date).getTime()
  const days = rangeDays(range)
  const cutoff = lastTime - days * 24 * 3600 * 1000
  return series.filter((point) => new Date(point.date).getTime() >= cutoff)
}

export function calculateRangeReturn(series: TrendPoint[]) {
  if (series.length < 2) return 0
  const start = series[0].value
  const end = series.at(-1)!.value
  if (start === 0) return 0
  return ((end - start) / start) * 100
}

export function buildTrendMarkers(orders: PaperOrder[], sipPlans: SipPlan[], detail: FundDetail): Map<string, TrendMarker[]> {
  const series = detail.referenceOnly ? detail.estimateHistory : detail.navHistory
  const seriesDates = series.map((point) => point.date.slice(0, 10))
  const markers = new Map<string, TrendMarker[]>()

  orders
    .filter((order) => order.fundCode === detail.code)
    .forEach((order) => {
      const markerDate = nearestSeriesDate(seriesDates, order.executedAt.slice(0, 10))
      const next = markers.get(markerDate) ?? []
      next.push({
        type: order.orderType,
        title: order.orderType === 'BUY' ? '模拟买入' : '模拟卖出',
        detail: `${formatCurrency(order.amount)} · ${order.executedAt.slice(0, 10)}`,
      })
      markers.set(markerDate, next)
    })

  sipPlans
    .filter((plan) => plan.fundCode === detail.code)
    .forEach((plan) => {
      const markerDate = nearestSeriesDate(seriesDates, plan.nextRunAt.slice(0, 10))
      const next = markers.get(markerDate) ?? []
      next.push({
        type: 'SIP',
        title: '定投点位预留',
        detail: `${plan.cadence} · 下次 ${plan.nextRunAt.slice(0, 10)}`,
      })
      markers.set(markerDate, next)
    })

  return markers
}

function nearestSeriesDate(seriesDates: string[], targetDate: string) {
  if (!seriesDates.length) return targetDate
  if (seriesDates.includes(targetDate)) return targetDate
  const target = new Date(targetDate).getTime()
  return seriesDates.reduce((closest, current) => {
    const currentGap = Math.abs(new Date(current).getTime() - target)
    const closestGap = Math.abs(new Date(closest).getTime() - target)
    return currentGap < closestGap ? current : closest
  }, seriesDates.at(-1)!)
}

function rangeDays(range: FundRangeKey) {
  switch (range) {
    case '7d':
      return 7
    case '1m':
      return 30
    case '3m':
      return 90
    case '6m':
      return 180
    case '1y':
      return 365
    case '3y':
      return 1095
  }
}
