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
    watchlist: null,
    portfolios: null,
    orders: null,
    sipPlans: null,
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
    sipCadence: defaultSipCadence,
    sipWeekday: defaultSipWeekday,
    sipMonthDay: defaultSipMonthDay,
    sipAmount: '',
  }
}
