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

  async function ensureWatchlistGroups(force = false) {
    const cached = getData().watchlistGroupOptions
    if (!force && cached) return cached
    const watchlistGroupOptions = await workspaceApi.watchlistGroups()
    setData((current) => ({ ...current, watchlistGroupOptions }))
    return watchlistGroupOptions
  }

  async function ensureHoldingsOverview(force = false) {
    const cached = getData().holdingsOverview
    if (!force && cached) return cached
    const holdingsOverview = await workspaceApi.holdingsOverview()
    setData((current) => ({ ...current, holdingsOverview }))
    return holdingsOverview
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

  async function ensureFundHoldingInsight(code: string, force = false) {
    const cached = getData().selectedFund?.code === code ? getData().selectedFundHoldingInsight : null
    if (!force && cached) return cached
    const selectedFundHoldingInsight = await workspaceApi.fundHoldingInsight(code).catch(() => null)
    setData((current) => ({ ...current, selectedFundHoldingInsight }))
    return selectedFundHoldingInsight
  }

  function clearFundHoldingInsight() {
    setData((current) => ({ ...current, selectedFundHoldingInsight: null }))
  }

  async function ensureSipRecords(sipPlanId: string, force = false) {
    const cached = getData().sipRecordsByPlanId[sipPlanId]
    if (!force && cached) return cached
    const records = await workspaceApi.sipRecords(sipPlanId)
    setData((current) => ({
      ...current,
      sipRecordsByPlanId: {
        ...current.sipRecordsByPlanId,
        [sipPlanId]: records,
      },
    }))
    return records
  }

  return {
    ensureFeatureFlags,
    ensureFundDetail,
    ensureFundHoldingInsight,
    clearFundHoldingInsight,
    ensureFunds,
    ensureHoldingsOverview,
    ensureOrders,
    ensureOverviewDashboard,
    ensureSipRecords,
    ensureSipPlans,
    ensureWatchlist,
    ensureWatchlistGroups,
  }
}
