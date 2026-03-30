/** 仪表盘契约，优先表达前端页面真实需要的摘要数据，而不是数据库全量聚合。 */
import type { FeatureFlag, MetricTone, UserProfile } from './shared'
import type { RecentOrderListResponse, PortfolioSummary } from './holdings'
import type { SipPlan, SipPlanDigest, AlertRule, ImportJob, WeeklyReport } from './sipPlans'
import type { WatchlistItem } from './watchlist'

/** 顶部 hero 指标卡。 */
export type OverviewHeroMetric = {
  label: string
  value: string
  delta: string
  tone: MetricTone
}

/** 仪表盘顶部指标接口响应。 */
export type OverviewHeroMetricsResponse = {
  profile: UserProfile
  metrics: OverviewHeroMetric[]
}

/** 仪表盘今日关注接口响应。 */
export type OverviewWatchlistPulseResponse = {
  items: WatchlistItem[]
}

/** 仪表盘最近动作接口响应。 */
export type OverviewRecentActionsResponse = {
  items: RecentOrderListResponse
}

/** 仪表盘定投摘要接口响应。 */
export type OverviewSipPlanDigestResponse = {
  items: SipPlanDigest[]
}

/**
 * 仪表盘聚合响应。
 * 说明：
 * 1. 该类型用于兼容当前前端现状。
 * 2. 新接口设计不建议继续把所有模块数据都塞进一个 dashboard 响应里。
 */
export type DashboardResponse = {
  profile: UserProfile
  heroMetrics: OverviewHeroMetric[]
  featureFlags: FeatureFlag[]
  watchlist: WatchlistItem[]
  portfolios: PortfolioSummary[]
  orders: RecentOrderListResponse
  sipPlans: SipPlan[]
  reports: WeeklyReport[]
  alerts: AlertRule[]
  importJobs: ImportJob[]
}
