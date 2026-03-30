/** 工作台默认状态工厂，用于初始化数据容器、凭证和弹层输入。 */
import type { AppDataState } from '../appTypes'
import {
  createDefaultWatchlistGroups,
  defaultPassword,
  defaultSipCadence,
  defaultSipMonthDay,
  defaultSipWeekday,
  defaultUsername,
} from './constants'

function currentLocalDateString() {
  const now = new Date()
  const timezoneOffset = now.getTimezoneOffset() * 60 * 1000
  return new Date(now.getTime() - timezoneOffset).toISOString().slice(0, 10)
}

export function createEmptyAppData(): AppDataState {
  return {
    overviewDashboard: null,
    overviewHeroMetrics: null,
    overviewWatchlistPulse: null,
    overviewRecentActions: null,
    overviewSipPlanDigests: null,
    featureFlags: null,
    funds: null,
    selectedFund: null,
    selectedFundHoldingInsight: null,
    watchlist: null,
    portfolios: null,
    holdingsOverview: null,
    orders: null,
    localHoldingHistory: [],
    sipPlans: null,
    sipRecordsByPlanId: {},
    reports: null,
    alerts: null,
    importJobs: null,
  }
}

export function createDefaultCredentials() {
  return {
    username: defaultUsername,
    password: defaultPassword,
  }
}

export function createDefaultWorkspaceInputs() {
  return {
    watchlistGroups: createDefaultWatchlistGroups(),
    holdingAmount: '',
    holdingPnl: '',
    holdingOperationAmount: '',
    holdingOperationShares: '',
    holdingOperationTradeDate: currentLocalDateString(),
    holdingOperationTiming: 'AFTER_3PM' as const,
    holdingOperationFeeRate: '0',
    sipCadence: defaultSipCadence,
    sipWeekday: defaultSipWeekday,
    sipMonthDay: defaultSipMonthDay,
    sipAmount: '',
  }
}
