/** 工作台数据加载层，负责缓存感知的 ensure 方法和页面数据预取。 */
import type { Dispatch, SetStateAction } from 'react'
import type { AppDataState } from '../appTypes'
import type { FundDetail } from '@fundcat/contracts'
import { workspaceApi } from '../data/workspaceApi'
import { buildPartialDashboard } from '../utils/dashboard'

type SetData = Dispatch<SetStateAction<AppDataState>>
type SetFundDetailCache = Dispatch<SetStateAction<Record<string, FundDetail>>>

type CreateWorkspaceEnsureLoadersOptions = {
  getData: () => AppDataState
  getFundDetailCache: () => Record<string, FundDetail>
  setData: SetData
  setFundDetailCache: SetFundDetailCache
}

export function createWorkspaceEnsureLoaders({
  getData,
  getFundDetailCache,
  setData,
  setFundDetailCache,
}: CreateWorkspaceEnsureLoadersOptions) {
  async function ensureOverviewDashboard(force = false) {
    const cachedDashboard = getData().overviewDashboard
    const cachedHeroMetrics = getData().overviewHeroMetrics
    const cachedWatchlistPulse = getData().overviewWatchlistPulse
    const cachedRecentActions = getData().overviewRecentActions
    const cachedSipPlanDigests = getData().overviewSipPlanDigests

    if (!force && cachedDashboard && cachedHeroMetrics && cachedWatchlistPulse && cachedRecentActions && cachedSipPlanDigests) {
      return cachedDashboard
    }

    const [overviewHeroMetrics, overviewWatchlistPulse, overviewRecentActions, overviewSipPlanDigests] = await Promise.all([
      workspaceApi.overviewHeroMetrics(),
      workspaceApi.overviewWatchlistPulse(),
      workspaceApi.overviewRecentActions(),
      workspaceApi.overviewSipPlanDigests(),
    ])

    const overviewDashboard = {
      ...buildPartialDashboard({
        profile: overviewHeroMetrics.profile,
        watchlist: overviewWatchlistPulse.items,
        orders: overviewRecentActions.items,
      }),
      heroMetrics: overviewHeroMetrics.metrics,
    }

    setData((current) => ({
      ...current,
      overviewDashboard,
      overviewHeroMetrics,
      overviewWatchlistPulse,
      overviewRecentActions,
      overviewSipPlanDigests,
    }))
    return overviewDashboard
  }

  async function ensureFeatureFlags(force = false) {
    const cached = getData().featureFlags
    if (!force && cached) return cached
    const featureFlags = await workspaceApi.featureFlags()
    setData((current) => ({ ...current, featureFlags }))
    return featureFlags
  }

  async function ensureFunds(force = false) {
    const cached = getData().funds
    if (!force && cached) return cached
    const funds = await workspaceApi.funds()
    setData((current) => ({ ...current, funds }))
    return funds
  }

  async function ensureWatchlist(force = false) {
    const cached = getData().watchlist
    if (!force && cached) return cached
    const watchlist = await workspaceApi.watchlist()
    setData((current) => ({ ...current, watchlist }))
    return watchlist
  }

  async function ensurePortfolios(force = false) {
    const cached = getData().portfolios
    if (!force && cached) return cached
    const portfolios = await workspaceApi.portfolios()
    setData((current) => ({ ...current, portfolios }))
    return portfolios
  }

  async function ensureOrders(force = false) {
    const cached = getData().orders
    if (!force && cached) return cached
    const orders = await workspaceApi.orders()
    setData((current) => ({ ...current, orders }))
    return orders
  }

  async function ensureSipPlans(force = false) {
    const cached = getData().sipPlans
    if (!force && cached) return cached
    const sipPlans = await workspaceApi.sipPlans()
    setData((current) => ({ ...current, sipPlans }))
    return sipPlans
  }

  async function ensureReports(force = false) {
    const cached = getData().reports
    if (!force && cached) return cached
    const reports = await workspaceApi.weeklyReports()
    setData((current) => ({ ...current, reports }))
    return reports
  }

  async function ensureAlerts(force = false) {
    const cached = getData().alerts
    if (!force && cached) return cached
    const alerts = await workspaceApi.alerts()
    setData((current) => ({ ...current, alerts }))
    return alerts
  }

  async function ensureImportJobs(force = false) {
    const cached = getData().importJobs
    if (!force && cached) return cached
    const importJobs = await workspaceApi.importJobs()
    setData((current) => ({ ...current, importJobs }))
    return importJobs
  }

  async function ensureFundDetail(code: string, force = false) {
    const cached = getFundDetailCache()[code]
    if (!force && cached) {
      setData((current) => ({ ...current, selectedFund: cached }))
      return cached
    }
    const detail = await workspaceApi.fundDetail(code)
    setFundDetailCache((current) => ({ ...current, [code]: detail }))
    setData((current) => ({ ...current, selectedFund: detail }))
    return detail
  }

  return {
    ensureAlerts,
    ensureFeatureFlags,
    ensureFundDetail,
    ensureFunds,
    ensureImportJobs,
    ensureOrders,
    ensureOverviewDashboard,
    ensurePortfolios,
    ensureReports,
    ensureSipPlans,
    ensureWatchlist,
  }
}
