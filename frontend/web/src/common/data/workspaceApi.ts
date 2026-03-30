/** 工作台业务 API 层，按前端场景组织接口调用，屏蔽底层请求细节。 */
import type {
  AlertRule,
  AuthResponse,
  CreateImportJobPayload,
  CreateHoldingOperationPayload,
  CreateSipPlanPayload,
  CreateWatchlistPayload,
  DashboardResponse,
  FeatureFlag,
  FundCard,
  FundDetail,
  HoldingInsight,
  HoldingOperation,
  HoldingsOverviewResponse,
  HoldingUpsertPayload,
  ImportJob,
  LoginPayload,
  OverviewHeroMetricsResponse,
  OverviewRecentActionsResponse,
  OverviewSipPlanDigestResponse,
  OverviewWatchlistPulseResponse,
  PaperOrder,
  PortfolioSummary,
  SipPlan,
  SipExecutionRecord,
  UpdateSipPlanPayload,
  UpdateWatchlistGroupsPayload,
  WatchlistItem,
  WeeklyReport,
} from '@fundcat/contracts'
import { authStorage, getAccessToken } from './authStorage'
import { API_BASE, requestJson, withAuthHeaders } from './http'

async function requestOverviewSlice<T>(path: string, fallback: (dashboard: DashboardResponse) => T) {
  try {
    return await requestJson<T>(path)
  } catch {
    const dashboard = await workspaceApi.dashboard()
    return fallback(dashboard)
  }
}

function toLegacyOrder(operation: HoldingOperation): PaperOrder {
  const orderType: PaperOrder['orderType'] =
    operation.operation === 'SELL' || operation.operation === 'CLOSE_POSITION'
      ? 'SELL'
      : 'BUY'
  return {
    id: operation.id,
    fundCode: operation.fundCode,
    fundName: operation.fundName,
    orderType,
    operation: operation.operation,
    source: operation.source,
    amount: operation.amount,
    shares: operation.shares,
    fee: operation.feeAmount,
    feeRate: operation.feeRate,
    status: operation.status,
    tradeDate: operation.tradeDate,
    executedAt: `${operation.tradeDate}T15:00:00`,
  }
}

export const workspaceApi = {
  login: async (payload: LoginPayload) => {
    const response = await requestJson<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    authStorage.save(response.accessToken, response.refreshToken)
    return response
  },
  logout: async () => {
    if (!getAccessToken()) return
    await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      headers: withAuthHeaders(),
    }).catch(() => undefined)
  },
  dashboard: async () => {
    const dashboard = await requestJson<DashboardResponse>('/dashboard')
    return {
      ...dashboard,
      orders: (dashboard.orders as unknown as HoldingOperation[]).map(toLegacyOrder),
    }
  },
  overviewHeroMetrics: () =>
    requestOverviewSlice<OverviewHeroMetricsResponse>('/overview/hero-metrics', (dashboard) => ({
      profile: dashboard.profile,
      metrics: dashboard.heroMetrics,
    })),
  overviewWatchlistPulse: () =>
    requestOverviewSlice<OverviewWatchlistPulseResponse>('/overview/watchlist-pulse', (dashboard) => ({
      items: dashboard.watchlist,
    })),
  overviewRecentActions: () =>
    requestOverviewSlice<OverviewRecentActionsResponse>('/overview/recent-actions', (dashboard) => ({
      items: dashboard.orders,
    })),
  overviewSipPlanDigests: () =>
    requestOverviewSlice<OverviewSipPlanDigestResponse>('/overview/sip-digests', (dashboard) => ({
      items: dashboard.sipPlans.map((plan) => ({
        id: plan.id,
        fundCode: plan.fundCode,
        fundName: plan.fundName,
        amount: plan.amount,
        cadenceLabel: String(plan.cadence),
        nextRunOn: plan.active ? plan.nextRunAt.slice(0, 10) : undefined,
        status: plan.active ? '生效' : new Date(plan.nextRunAt).getTime() < Date.now() ? '停止' : '暂停',
      })),
    })),
  featureFlags: () => requestJson<FeatureFlag[]>('/ops/feature-flags'),
  funds: (query = '') =>
    requestJson<FundCard[]>(`/funds${query ? `?query=${encodeURIComponent(query)}` : ''}`),
  fundDetail: (code: string) => requestJson<FundDetail>(`/funds/${code}`),
  fundHoldingInsight: (code: string) => requestJson<HoldingInsight>(`/funds/${encodeURIComponent(code)}/holding-insight`),
  watchlist: () => requestJson<WatchlistItem[]>('/watchlist'),
  addWatchlist: (payload: CreateWatchlistPayload) =>
    requestJson('/watchlist', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  updateWatchlistGroups: (payload: UpdateWatchlistGroupsPayload) =>
    requestJson<WatchlistItem[]>('/watchlist/groups', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  removeWatchlist: (fundCode: string) =>
    requestJson(`/watchlist/${encodeURIComponent(fundCode)}`, {
      method: 'DELETE',
    }),
  holdingsOverview: () => requestJson<HoldingsOverviewResponse>('/holdings/overview'),
  createHolding: (payload: HoldingUpsertPayload) =>
    requestJson('/holdings', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  updateHolding: (fundCode: string, payload: HoldingUpsertPayload) =>
    requestJson(`/holdings/${encodeURIComponent(fundCode)}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  createHoldingOperation: (payload: CreateHoldingOperationPayload) =>
    requestJson<HoldingOperation>('/holdings/operations', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  portfolios: () => requestJson<PortfolioSummary[]>('/portfolios'),
  orders: async () => {
    const operations = await requestJson<HoldingOperation[]>('/orders?scope=recent')
    return operations.map(toLegacyOrder)
  },
  sipPlans: () => requestJson<SipPlan[]>('/sips'),
  sipPlan: (sipPlanId: string) => requestJson<SipPlan>(`/sips/${encodeURIComponent(sipPlanId)}`),
  sipRecords: (sipPlanId: string) => requestJson<SipExecutionRecord[]>(`/sips/${encodeURIComponent(sipPlanId)}/records`),
  createSip: (payload: CreateSipPlanPayload) =>
    requestJson('/sips', {
      method: 'POST',
      body: JSON.stringify({ ...payload, feeRate: payload.feeRate ?? 0 }),
    }),
  updateSip: (sipPlanId: string, payload: UpdateSipPlanPayload) =>
    requestJson<SipPlan>(`/sips/${encodeURIComponent(sipPlanId)}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  pauseSip: (sipPlanId: string) =>
    requestJson<SipPlan>(`/sips/${encodeURIComponent(sipPlanId)}/pause`, {
      method: 'POST',
    }),
  resumeSip: (sipPlanId: string) =>
    requestJson<SipPlan>(`/sips/${encodeURIComponent(sipPlanId)}/resume`, {
      method: 'POST',
    }),
  stopSip: (sipPlanId: string) =>
    requestJson<SipPlan>(`/sips/${encodeURIComponent(sipPlanId)}/stop`, {
      method: 'POST',
    }),
  importJobs: () => requestJson<ImportJob[]>('/import-jobs'),
  createImportJob: (payload: CreateImportJobPayload) =>
    requestJson('/import-jobs', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  weeklyReports: () => requestJson<WeeklyReport[]>('/reports/weekly'),
  alerts: () => requestJson<AlertRule[]>('/alerts'),
}
