/** 路由与仪表盘辅助函数，负责页面识别、回跳策略和局部 dashboard 拼装。 */
import type {
  AlertRule,
  DashboardResponse,
  FeatureFlag,
  PaperOrder,
  PortfolioSummary,
  SipPlan,
  WatchlistItem,
  WeeklyReport,
} from '@fundcat/contracts'
import { matchPath } from 'react-router-dom'
import type { PageId } from '../appTypes'

export function currentPageId(pathname: string): PageId {
  if (pathname.startsWith('/funds')) return 'funds'
  if (pathname.startsWith('/holdings')) return 'holdings'
  if (pathname.startsWith('/portfolio')) return 'portfolio'
  if (pathname.startsWith('/automation')) return 'automation'
  return 'overview'
}

export function currentFundCode(pathname: string) {
  return matchPath('/funds/:fundCode', pathname)?.params.fundCode ?? null
}

export function currentSipPlanId(pathname: string) {
  return matchPath('/automation/:sipPlanId', pathname)?.params.sipPlanId ?? null
}

export function detailBackTarget(state: unknown) {
  if (!state || typeof state !== 'object' || !('from' in state)) {
    return '/funds'
  }
  return typeof state.from === 'string' ? state.from : '/funds'
}

export function defaultFundCodeFromDashboard(dashboard: DashboardResponse) {
  return dashboard.watchlist[0]?.code ?? '000001'
}

export function defaultFundCodeFromWatchlist(items: WatchlistItem[]) {
  return items[0]?.code ?? '000001'
}

export function buildPartialDashboard({
  profile,
  featureFlags = [],
  watchlist = [],
  portfolios = [],
  orders = [],
  sipPlans = [],
  reports = [],
  alerts = [],
  importJobs = [],
}: {
  profile?: DashboardResponse['profile']
  featureFlags?: FeatureFlag[]
  watchlist?: WatchlistItem[]
  portfolios?: PortfolioSummary[]
  orders?: PaperOrder[]
  sipPlans?: SipPlan[]
  reports?: WeeklyReport[]
  alerts?: AlertRule[]
  importJobs?: import('@fundcat/contracts').ImportJob[]
}): DashboardResponse {
  return {
    profile: profile ?? {
      id: '',
      displayName: '',
      username: '',
      riskMode: 'research',
    },
    heroMetrics: [],
    featureFlags,
    watchlist,
    portfolios,
    orders,
    sipPlans,
    reports,
    alerts,
    importJobs,
  }
}
