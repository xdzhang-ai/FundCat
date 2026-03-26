/** 工作台业务 API 层，按前端场景组织接口调用，屏蔽底层请求细节。 */
import type {
  AlertRule,
  AuthResponse,
  CreateImportJobPayload,
  CreatePaperOrderPayload,
  CreateSipPlanPayload,
  CreateWatchlistPayload,
  DashboardResponse,
  FeatureFlag,
  FundCard,
  FundDetail,
  ImportJob,
  LoginPayload,
  OverviewHeroMetricsResponse,
  OverviewRecentActionsResponse,
  OverviewSipPlanDigestResponse,
  OverviewWatchlistPulseResponse,
  PaperOrder,
  PortfolioSummary,
  SipPlan,
  WatchlistItem,
  WeeklyReport,
} from '@fundcat/contracts'
import { authStorage, getAccessToken } from './authStorage'
import { API_BASE, requestJson, withAuthHeaders } from './http'

async function requestOverviewSlice<T>(path: string, fallback: (dashboard: DashboardResponse) => T) {
  try {
    return await requestJson<T>(path)
  } catch {
    const dashboard = await requestJson<DashboardResponse>('/dashboard')
    return fallback(dashboard)
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
  dashboard: () => requestJson<DashboardResponse>('/dashboard'),
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
  watchlist: () => requestJson<WatchlistItem[]>('/watchlist'),
  addWatchlist: (payload: CreateWatchlistPayload) =>
    requestJson('/watchlist', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  removeWatchlist: (fundCode: string) =>
    requestJson(`/watchlist/${encodeURIComponent(fundCode)}`, {
      method: 'DELETE',
    }),
  portfolios: () => requestJson<PortfolioSummary[]>('/portfolios'),
  orders: () => requestJson<PaperOrder[]>('/orders'),
  createOrder: (payload: CreatePaperOrderPayload) =>
    requestJson('/orders', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  sipPlans: () => requestJson<SipPlan[]>('/sips'),
  createSip: (payload: CreateSipPlanPayload) =>
    requestJson('/sips', {
      method: 'POST',
      body: JSON.stringify(payload),
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
