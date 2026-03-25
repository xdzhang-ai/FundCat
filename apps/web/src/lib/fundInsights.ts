import type {
  DashboardResponse,
  FundCard,
  FundDetail,
  HoldingLot,
  TrendPoint,
} from '@fundcat/contracts'
import type { TrendMarker } from '../components/charts/FundTrendChart'

export type FundRangeKey = '7d' | '1m' | '3m' | '6m' | '1y' | '3y'

export type AggregatedHolding = {
  fundCode: string
  fundName: string
  shares: number
  averageCost: number
  currentValue: number
  pnl: number
  allocation: number
  lots: HoldingLot[]
  portfolioNames: string[]
}

export type HoldingInsight = {
  amountHeld: number
  shares: number
  allocation: number
  holdingPnl: number
  holdingReturnRate: number
  averageCost: number
  todayPnl: number | null
  yesterdayPnl: number
  holdingDays: number
  oneYearReturn: number
  dayChange: number | null
}

export function formatCurrency(value: number) {
  return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: value >= 1000 ? 2 : 4, maximumFractionDigits: 4 })}`
}

export function formatCompactPercent(value: number | null) {
  if (value == null || Number.isNaN(value)) {
    return '--'
  }
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
}

export function aggregateHoldings(dashboard: DashboardResponse, funds: FundCard[]) {
  const fundMap = new Map(funds.map((fund) => [fund.code, fund]))
  const totalValue = dashboard.portfolios
    .flatMap((portfolio) => portfolio.holdings)
    .reduce((sum, holding) => sum + resolveHoldingValue(holding, fundMap.get(holding.fundCode)), 0)

  const grouped = new Map<string, AggregatedHolding>()

  dashboard.portfolios.forEach((portfolio) => {
    portfolio.holdings.forEach((holding) => {
      const currentFund = fundMap.get(holding.fundCode)
      const currentValue = resolveHoldingValue(holding, currentFund)
      const costBasis = holding.shares * holding.averageCost
      const existing = grouped.get(holding.fundCode)
      if (!existing) {
        grouped.set(holding.fundCode, {
          fundCode: holding.fundCode,
          fundName: holding.fundName,
          shares: holding.shares,
          averageCost: holding.averageCost,
          currentValue,
          pnl: currentValue - costBasis,
          allocation: 0,
          lots: [holding],
          portfolioNames: [portfolio.name],
        })
        return
      }
      const nextShares = existing.shares + holding.shares
      const nextCostBasis = (existing.averageCost * existing.shares) + costBasis
      existing.shares = nextShares
      existing.averageCost = nextShares === 0 ? 0 : nextCostBasis / nextShares
      existing.currentValue += currentValue
      existing.pnl = existing.currentValue - nextCostBasis
      existing.lots.push(holding)
      if (!existing.portfolioNames.includes(portfolio.name)) {
        existing.portfolioNames.push(portfolio.name)
      }
    })
  })

  return Array.from(grouped.values())
    .map((holding) => ({
      ...holding,
      allocation: totalValue === 0 ? 0 : holding.currentValue / totalValue,
    }))
    .sort((left, right) => right.currentValue - left.currentValue)
}

function resolveHoldingValue(holding: HoldingLot, fund?: FundCard) {
  const currentNav = fund?.estimatedNav ?? (holding.shares === 0 ? 0 : holding.currentValue / holding.shares)
  return holding.shares * currentNav
}

export function buildHoldingInsight(
  dashboard: DashboardResponse,
  funds: FundCard[],
  detail: FundDetail,
): HoldingInsight | null {
  const currentHolding = aggregateHoldings(dashboard, funds).find((holding) => holding.fundCode === detail.code)
  if (!currentHolding) {
    return null
  }

  const holdingReturnRate =
    currentHolding.shares === 0 || currentHolding.averageCost === 0
      ? 0
      : ((detail.estimatedNav - currentHolding.averageCost) / currentHolding.averageCost) * 100
  const baseSeries = detail.navHistory
  const previousPoint = baseSeries.at(-2)
  const currentConfirmedNav = detail.unitNav
  const previousNav = previousPoint?.value ?? currentConfirmedNav
  const yesterdayPnl = currentHolding.shares * (currentConfirmedNav - previousNav)
  const todayPnl = detail.referenceOnly ? currentHolding.shares * (detail.estimatedNav - currentConfirmedNav) : null
  const firstUpdatedAt = currentHolding.lots
    .map((lot) => new Date(lot.updatedAt))
    .filter((date) => !Number.isNaN(date.getTime()))
    .sort((left, right) => left.getTime() - right.getTime())[0]
  const today = new Date()
  const holdingDays = firstUpdatedAt
    ? Math.max(1, Math.floor((today.getTime() - firstUpdatedAt.getTime()) / (24 * 3600 * 1000)))
    : 0
  const yearRange = filterRange(detail.referenceOnly ? detail.estimateHistory : detail.navHistory, '1y')
  const oneYearReturn = calculateRangeReturn(yearRange)

  return {
    amountHeld: currentHolding.currentValue,
    shares: currentHolding.shares,
    allocation: currentHolding.allocation * 100,
    holdingPnl: currentHolding.pnl,
    holdingReturnRate,
    averageCost: currentHolding.averageCost,
    todayPnl,
    yesterdayPnl,
    holdingDays,
    oneYearReturn,
    dayChange: detail.referenceOnly ? detail.estimatedGrowth : null,
  }
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

export function buildTrendMarkers(dashboard: DashboardResponse, detail: FundDetail): Map<string, TrendMarker[]> {
  const series = detail.referenceOnly ? detail.estimateHistory : detail.navHistory
  const seriesDates = series.map((point) => point.date.slice(0, 10))
  const markers = new Map<string, TrendMarker[]>()

  dashboard.orders
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

  dashboard.sipPlans
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
