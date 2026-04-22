/** 工作台共享类型定义，承接页面状态、表单输入和本地草稿结构。 */
import type {
  AmountBasis,
  AlertRule,
  DashboardResponse,
  FeatureFlag,
  FundDetail,
  HoldingInsight,
  HoldingsOverviewResponse,
  ImportJob,
  OverviewHeroMetricsResponse,
  OverviewRecentActionsResponse,
  OverviewSipPlanDigestResponse,
  OverviewWatchlistPulseResponse,
  PaperOrder,
  SipExecutionRecord,
  SipPlan,
  WatchlistGroupOption,
  WatchlistItem,
  WeeklyReport,
} from '@fundcat/contracts'

export type PageId = 'overview' | 'funds' | 'holdings' | 'watchlist' | 'sip'
export type WatchlistGroup = string

export type ScreenState =
  | { status: 'loading' }
  | { status: 'auth' }
  | { status: 'ready' }
  | { status: 'error'; message: string }

export type AppDataState = {
  overviewDashboard: DashboardResponse | null
  overviewHeroMetrics: OverviewHeroMetricsResponse | null
  overviewWatchlistPulse: OverviewWatchlistPulseResponse | null
  overviewRecentActions: OverviewRecentActionsResponse | null
  overviewSipPlanDigests: OverviewSipPlanDigestResponse | null
  featureFlags: FeatureFlag[] | null
  funds: import('@fundcat/contracts').FundCard[] | null
  selectedFund: FundDetail | null
  selectedFundHoldingInsight: HoldingInsight | null
  watchlist: WatchlistItem[] | null
  watchlistGroupOptions: WatchlistGroupOption[] | null
  holdingsOverview: HoldingsOverviewResponse | null
  orders: PaperOrder[] | null
  localHoldingHistory: LocalHoldingHistoryItem[]
  sipPlans: SipPlan[] | null
  sipRecordsByPlanId: Record<string, SipExecutionRecord[]>
  reports: WeeklyReport[] | null
  alerts: AlertRule[] | null
  importJobs: ImportJob[] | null
}

export type PendingWatchlistSelection = {
  code: string
  name: string
}

export type PendingHoldingInput = {
  code: string
  name: string
  mode: 'add' | 'edit'
}

export type PendingHoldingAmountBasis = AmountBasis

export type PendingHoldingOperationInput = {
  code: string
  name: string
  operation: 'BUY' | 'SELL'
}

export type PendingHoldingOperationTiming = 'BEFORE_3PM' | 'AFTER_3PM'

export type PendingSipInput = {
  code: string
  name: string
}

export type SipCadenceInput = 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type SipWeekdayInput = '1' | '2' | '3' | '4' | '5' | '6' | '0'

export type LocalSipPlanDraft = SipPlan

export type LocalHoldingHistoryItem = {
  id: string
  fundCode: string
  fundName: string
  historyType: 'BUILD'
  amount: number
  shares: number
  fee: number
  feeRate: number
  status: '已执行'
  tradeDate: string
  executedAt: string
  source: 'HOLDING_SNAPSHOT'
}

export type FundOperationHistoryItem = PaperOrder | LocalHoldingHistoryItem
