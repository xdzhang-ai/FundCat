/** 持仓与模拟订单契约，区分组合概览、持仓列表和基金持仓洞察三类语义。 */
import type { IsoDateTimeString } from './shared'

/** 底层持仓 lot 模型，当前仍兼容原有 dashboard 聚合口径。 */
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
  updatedAt: IsoDateTimeString
}

/**
 * 组合摘要。
 * 说明：
 * 1. 该模型适合概览页或需要完整组合树的场景。
 * 2. 若只渲染持仓表格，建议优先走 HoldingListItem，减少无效嵌套数据。
 */
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

/**
 * 持仓页行项目。
 * 说明：
 * 1. 这是更适合持仓页表格的细粒度接口模型。
 * 2. 后端可以直接预聚合 todayPnl / dayGrowth，避免前端二次推导和多次 join。
 */
export type HoldingListItem = {
  fundCode: string
  fundName: string
  dayGrowth: number
  todayPnl: number
  marketValue: number
  holdingPnl: number
}

/** 持仓页概览响应，推荐给持仓页使用。 */
export type HoldingsOverviewResponse = {
  totalMarketValue: number
  items: HoldingListItem[]
}

/**
 * 基金详情页里的持仓洞察。
 * 说明：
 * 该契约是基金详情页专属，不建议从大盘接口中顺手附带，以免把组合聚合压力放到每次详情查询上。
 */
export type HoldingInsight = {
  fundCode: string
  amountHeld: number
  holdingPnl: number
  holdingReturnRate: number
  shares: number
  averageCost: number
  allocation: number
  dayChange: number | null
  todayPnl: number | null
  yesterdayPnl: number
  oneYearReturn: number
  holdingDays: number
}

/** 模拟订单。 */
export type PaperOrder = {
  id: string
  fundCode: string
  fundName: string
  orderType: 'BUY' | 'SELL'
  amount: number
  shares: number
  fee: number
  status: string
  executedAt: IsoDateTimeString
}

/** 创建模拟订单请求。 */
export type CreatePaperOrderPayload = {
  portfolioId: string
  fundCode: string
  orderType: 'BUY' | 'SELL'
  amount: number
  shares: number
  fee: number
  note: string
}

/** 概览页最近动作响应，通常只取最近几条记录。 */
export type RecentOrderListResponse = PaperOrder[]
