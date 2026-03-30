/** 持仓与操作记录契约。 */
import type { DateString, IsoDateTimeString } from './shared'

export type AmountBasis = 'T_MINUS_1' | 'T'

/** 兼容完整资产树读取的持仓 lot 结构。 */
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

export type HoldingListItem = {
  fundCode: string
  fundName: string
  dayGrowth: number
  todayPnl: number
  marketValue: number
  holdingPnl: number
  allocation: number
}

export type HoldingsOverviewResponse = {
  totalMarketValue: number
  items: HoldingListItem[]
}

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

export type HoldingUpsertPayload = {
  fundCode: string
  amountBasis: AmountBasis
  amount: number
  holdingPnl: number
}

export type HoldingOperation = {
  id: string
  fundCode: string
  fundName: string
  operation: 'BUY' | 'SELL' | 'OPEN_POSITION' | 'CLOSE_POSITION' | 'SIP_BUY'
  source: 'MANUAL' | 'SIP'
  status: '确认中' | '已执行'
  tradeDate: DateString
  amount: number
  shares: number
  nav: number
  feeRate: number
  feeAmount: number
}

/** 兼容现有前端展示层的最近动作模型。 */
export type PaperOrder = {
  id: string
  fundCode: string
  fundName: string
  orderType: 'BUY' | 'SELL'
  operation: 'BUY' | 'SELL' | 'OPEN_POSITION' | 'CLOSE_POSITION' | 'SIP_BUY'
  source: 'MANUAL' | 'SIP'
  amount: number
  shares: number
  fee: number
  feeRate: number
  status: string
  tradeDate: DateString
  executedAt: IsoDateTimeString
}

export type CreateBuyOperationPayload = {
  fundCode: string
  operation: 'BUY'
  tradeDate: DateString
  amount: number
  feeRate: number
  note?: string
}

export type CreateSellOperationPayload = {
  fundCode: string
  operation: 'SELL'
  tradeDate: DateString
  shares: number
  feeRate: number
  note?: string
}

export type CreateHoldingOperationPayload = CreateBuyOperationPayload | CreateSellOperationPayload

export type RecentOrderListResponse = PaperOrder[]
