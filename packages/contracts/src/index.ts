export type TrendPoint = {
  date: string
  value: number
}

export type QuarterlyHolding = {
  name: string
  symbol: string
  dailyChange: number
  positionRatio: number
  previousChange: number | null
  changeLabel?: string
}

export type IndustryExposure = {
  name: string
  weight: number
}

export type TopHolding = {
  name: string
  symbol: string
  dailyChange: number
}

export type UserProfile = {
  id: string
  displayName: string
  username: string
  riskMode: 'research'
}

export type FeatureFlag = {
  code: string
  name: string
  enabled: boolean
  environment: string
  description: string
  riskLevel: 'low' | 'medium' | 'high'
}

export type FundCard = {
  code: string
  name: string
  category: string
  riskLevel: string
  tags: string[]
  benchmark: string
  unitNav: number
  dayGrowth: number
  estimatedNav: number
  estimatedGrowth: number
  referenceOnly: boolean
  watchlisted: boolean
  held: boolean
}

export type FundDetail = FundCard & {
  managementFee: number
  custodyFee: number
  purchaseFee: number
  assetSize: number
  stockRatio: number
  bondRatio: number
  topHoldings: TopHolding[]
  navHistory: TrendPoint[]
  estimateHistory: TrendPoint[]
  comparisonLabels: string[]
  quarterlyHoldings: QuarterlyHolding[]
  industryDistribution: IndustryExposure[]
}

export type WatchlistItem = {
  code: string
  name: string
  note: string
  estimatedGrowth: number
  unitNav: number
  estimatedNav: number
}

export type HoldingLot = {
  id: string
  fundCode: string
  fundName: string
  shares: number
  averageCost: number
  currentValue: number
  pnl: number
  allocation: number
  source: string
  updatedAt: string
}

export type PortfolioSummary = {
  id: string
  name: string
  broker: string
  currency: string
  marketValue: number
  totalPnl: number
  cash: number
  holdings: HoldingLot[]
}

export type PaperOrder = {
  id: string
  fundCode: string
  fundName: string
  orderType: 'BUY' | 'SELL'
  amount: number
  shares: number
  fee: number
  status: string
  executedAt: string
}

export type SipPlan = {
  id: string
  fundCode: string
  fundName: string
  amount: number
  cadence: string
  nextRunAt: string
  active: boolean
}

export type WeeklyReport = {
  id: string
  weekLabel: string
  summary: string
  returnRate: number
  bestFundCode: string
  riskNote: string
}

export type AlertRule = {
  id: string
  fundCode: string
  ruleType: string
  thresholdValue: number
  enabled: boolean
  channel: string
}

export type ImportJob = {
  id: string
  sourcePlatform: string
  status: string
  fileName: string
  recognizedHoldings: number
  createdAt: string
}

export type DashboardResponse = {
  profile: UserProfile
  heroMetrics: Array<{
    label: string
    value: string
    delta: string
    tone: 'positive' | 'negative' | 'neutral'
  }>
  featureFlags: FeatureFlag[]
  watchlist: WatchlistItem[]
  portfolios: PortfolioSummary[]
  orders: PaperOrder[]
  sipPlans: SipPlan[]
  reports: WeeklyReport[]
  alerts: AlertRule[]
  importJobs: ImportJob[]
}

export type AuthTokens = {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export type AuthResponse = AuthTokens & {
  profile: UserProfile
}

export type LoginPayload = {
  username: string
  password: string
}

export type RegisterPayload = LoginPayload & {
  displayName: string
}

export type CreateWatchlistPayload = {
  fundCode: string
  note: string
}

export type CreatePaperOrderPayload = {
  portfolioId: string
  fundCode: string
  orderType: 'BUY' | 'SELL'
  amount: number
  shares: number
  fee: number
  note: string
}

export type CreateSipPlanPayload = {
  portfolioId: string
  fundCode: string
  amount: number
  cadence: string
  nextRunAt: string
}

export type CreateImportJobPayload = {
  sourcePlatform: string
  fileName: string
}
