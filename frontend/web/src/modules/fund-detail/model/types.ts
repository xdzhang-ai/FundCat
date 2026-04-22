/** 基金详情页模块类型定义，统一页面和子组件共享的 props 结构。 */
import type { FundDetail, HoldingInsight, PaperOrder, SipPlan } from '@fundcat/contracts'
import type { FundOperationHistoryItem, PendingHoldingOperationTiming, WatchlistGroup } from '../../../common/appTypes'

export type FundDetailPageProps = {
  orders: PaperOrder[]
  operationHistory: FundOperationHistoryItem[]
  sipPlans: SipPlan[]
  selectedFund: FundDetail
  holdingInsight: HoldingInsight | null
  isFlagEnabled: (code: string) => boolean
  onBack: () => void
  onQuickAction: (kind: 'watchlist' | 'holding' | 'editHolding' | 'sip' | 'buy' | 'sell') => void
  watchlistPickerOpen: boolean
  watchlistPickerGroup: WatchlistGroup
  watchlistGroupOptions: WatchlistGroup[]
  onSelectWatchlistGroup: (group: WatchlistGroup) => void
  onCancelWatchlistPicker: () => void
  onConfirmWatchlistPicker: () => void
  holdingInputOpen: boolean
  holdingFormMode: 'add' | 'edit'
  holdingAmount: string
  holdingPnl: string
  holdingAmountBasis: 'T_MINUS_1' | 'T'
  onHoldingAmountChange: (value: string) => void
  onHoldingPnlChange: (value: string) => void
  onHoldingAmountBasisChange: (value: 'T_MINUS_1' | 'T') => void
  onCancelHoldingInput: () => void
  onConfirmHoldingInput: () => void
  holdingOperationInputOpen: boolean
  holdingOperationType: 'BUY' | 'SELL'
  holdingOperationAmount: string
  holdingOperationFeeRate: string
  holdingOperationShares: string
  holdingOperationTradeDate: string
  holdingOperationTiming: PendingHoldingOperationTiming
  onHoldingOperationAmountChange: (value: string) => void
  onHoldingOperationFeeRateChange: (value: string) => void
  onHoldingOperationSharesChange: (value: string) => void
  onHoldingOperationTradeDateChange: (value: string) => void
  onHoldingOperationTimingChange: (value: PendingHoldingOperationTiming) => void
  onCancelHoldingOperation: () => void
  onConfirmHoldingOperation: () => void
  sipPlanExists: boolean
  sipInputOpen: boolean
  sipCadence: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  sipWeekday: '1' | '2' | '3' | '4' | '5' | '6' | '0'
  sipMonthDay: string
  sipAmount: string
  onSipCadenceChange: (value: 'DAILY' | 'WEEKLY' | 'MONTHLY') => void
  onSipWeekdayChange: (value: '1' | '2' | '3' | '4' | '5' | '6' | '0') => void
  onSipMonthDayChange: (value: string) => void
  onSipAmountChange: (value: string) => void
  onCancelSipInput: () => void
  onConfirmSipInput: () => void
  onOpenSipPlan: () => void
  onJumpToHoldings: () => void
}
