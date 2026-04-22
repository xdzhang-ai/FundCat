/** 基金中心与基金详情契约，面向列表页和详情页两个不同粒度场景。 */
import type { TrendPoint } from './shared'

/** 季报持仓股票摘要，按前端表格最小展示字段组织。 */
export type QuarterlyHolding = {
  name: string
  symbol: string
  dailyChange: number
  positionRatio: number
  previousChange: number | null
  changeLabel?: string
}

/** 行业分布条目。 */
export type IndustryExposure = {
  name: string
  weight: number
}

/** 基金主要持仓股票条目。 */
export type TopHolding = {
  name: string
  symbol: string
  dailyChange: number
}

/**
 * 基金列表项。
 * 说明：
 * 1. 这是基金中心、自选页状态补齐、持仓页关联涨跌时共同依赖的最小列表模型。
 * 2. 列表接口不应直接返回详情页的大对象，避免无效字段放大查询成本。
 */
export type FundCard = {
  code: string
  name: string
  tags: string[]
  unitNav: number
  dayGrowth: number
  estimatedNav: number
  estimatedGrowth: number
  referenceOnly: boolean
  watchlisted: boolean
  held: boolean
}

/**
 * 基金详情模型。
 * 说明：
 * 1. 详情接口只在基金详情页加载，允许携带走势图、行业分布等重字段。
 * 2. 若未来历史曲线过长，建议再把 navHistory / estimateHistory 拆成独立接口。
 */
export type FundDetail = FundCard & {
  topHoldings: TopHolding[]
  navHistory: TrendPoint[]
  estimateHistory: TrendPoint[]
  comparisonLabels: string[]
  quarterlyHoldings: QuarterlyHolding[]
  industryDistribution: IndustryExposure[]
}

/** 基金搜索结果响应。 */
export type FundSearchResponse = FundCard[]
