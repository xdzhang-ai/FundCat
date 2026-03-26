/** 基金详情页模块类型定义，统一页面和子组件共享的 props 结构。 */
import type { FundCard, FundDetail, PaperOrder, PortfolioSummary, SipPlan } from '@fundcat/contracts'
import type { WatchlistGroup } from '../../../common/appTypes'

export type FundDetailPageProps = {
  portfolios: PortfolioSummary[]
  orders: PaperOrder[]
  sipPlans: SipPlan[]
  funds: FundCard[]
  selectedFund: FundDetail
  isFlagEnabled: (code: string) => boolean
  onBack: () => void
  onQuickAction: (kind: 'watchlist' | 'holding' | 'editHolding' | 'sip' | 'ocr') => void
  watchlistPickerOpen: boolean
  watchlistPickerGroups: WatchlistGroup[]
  watchlistGroupOptions: WatchlistGroup[]
  onToggleWatchlistGroup: (group: WatchlistGroup) => void
  onCancelWatchlistPicker: () => void
  onConfirmWatchlistPicker: () => void
  holdingInputOpen: boolean
  holdingFormMode: 'add' | 'edit'
  holdingAmount: string
  holdingPnl: string
  onHoldingAmountChange: (value: string) => void
  onHoldingPnlChange: (value: string) => void
  onCancelHoldingInput: () => void
  onConfirmHoldingInput: () => void
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
