/** 持仓买卖补记动作层，封装详情页买入/卖出入口与 /holdings/operations 提交逻辑。 */
import type { FundCard } from '@fundcat/contracts'
import type { Dispatch, SetStateAction } from 'react'
import type {
  AppDataState,
  PendingHoldingInput,
  PendingHoldingOperationInput,
  PendingHoldingOperationTiming,
  PendingSipInput,
  PendingWatchlistSelection,
  SipCadenceInput,
  SipWeekdayInput,
  WatchlistGroup,
} from '../../../common/appTypes'
import { workspaceApi } from '../../../common/data/workspaceApi'
import { createDefaultWorkspaceInputs } from '../../../common/workspace/state'

type CreateHoldingOperationActionsOptions = {
  getData: () => AppDataState
  getPendingHoldingOperationAmount: () => string
  getPendingHoldingOperationInput: () => PendingHoldingOperationInput | null
  getPendingHoldingOperationShares: () => string
  getPendingHoldingOperationTradeDate: () => string
  getPendingHoldingOperationFeeRate: () => string
  getPendingHoldingOperationTiming: () => PendingHoldingOperationTiming
  refreshCurrentPage: (preferredFundCode?: string) => Promise<void>
  setActionMessage: Dispatch<SetStateAction<string | null>>
  setPendingHoldingAmount: Dispatch<SetStateAction<string>>
  setPendingHoldingInput: Dispatch<SetStateAction<PendingHoldingInput | null>>
  setPendingHoldingOperationAmount: Dispatch<SetStateAction<string>>
  setPendingHoldingOperationFeeRate: Dispatch<SetStateAction<string>>
  setPendingHoldingOperationInput: Dispatch<SetStateAction<PendingHoldingOperationInput | null>>
  setPendingHoldingOperationShares: Dispatch<SetStateAction<string>>
  setPendingHoldingOperationTradeDate: Dispatch<SetStateAction<string>>
  setPendingHoldingOperationTiming: Dispatch<SetStateAction<PendingHoldingOperationTiming>>
  setPendingHoldingPnl: Dispatch<SetStateAction<string>>
  setPendingSipAmount: Dispatch<SetStateAction<string>>
  setPendingSipCadence: Dispatch<SetStateAction<SipCadenceInput>>
  setPendingSipInput: Dispatch<SetStateAction<PendingSipInput | null>>
  setPendingSipMonthDay: Dispatch<SetStateAction<string>>
  setPendingSipWeekday: Dispatch<SetStateAction<SipWeekdayInput>>
  setPendingWatchlistGroup: Dispatch<SetStateAction<WatchlistGroup>>
  setPendingWatchlistSelection: Dispatch<SetStateAction<PendingWatchlistSelection | null>>
}

function isTradeDateWithinLastThirtyDays(tradeDate: string) {
  if (!tradeDate) return false

  const selected = new Date(`${tradeDate}T00:00:00`)
  if (Number.isNaN(selected.getTime())) return false

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const earliest = new Date(today)
  earliest.setDate(today.getDate() - 30)

  return selected >= earliest && selected <= today
}

function normalizeFeeRateInput(value: string) {
  if (!value) return ''

  const sanitized = value.replace(/[^\d.]/g, '')
  const [integerPart = '0', decimalPart = ''] = sanitized.split('.')
  const normalizedInteger = integerPart.replace(/^0+(?=\d)/, '') || '0'
  if (!sanitized.includes('.')) {
    return normalizedInteger
  }

  return `${normalizedInteger}.${decimalPart.slice(0, 2)}`
}

function nextBusinessDate(baseDate: string) {
  const date = new Date(`${baseDate}T00:00:00`)
  do {
    date.setDate(date.getDate() + 1)
  } while (date.getDay() === 0 || date.getDay() === 6)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function createHoldingOperationActions({
  getData,
  getPendingHoldingOperationAmount,
  getPendingHoldingOperationFeeRate,
  getPendingHoldingOperationInput,
  getPendingHoldingOperationShares,
  getPendingHoldingOperationTradeDate,
  getPendingHoldingOperationTiming,
  refreshCurrentPage,
  setActionMessage,
  setPendingHoldingAmount,
  setPendingHoldingInput,
  setPendingHoldingOperationAmount,
  setPendingHoldingOperationFeeRate,
  setPendingHoldingOperationInput,
  setPendingHoldingOperationShares,
  setPendingHoldingOperationTradeDate,
  setPendingHoldingOperationTiming,
  setPendingHoldingPnl,
  setPendingSipAmount,
  setPendingSipCadence,
  setPendingSipInput,
  setPendingSipMonthDay,
  setPendingSipWeekday,
  setPendingWatchlistGroup,
  setPendingWatchlistSelection,
}: CreateHoldingOperationActionsOptions) {
  function openHoldingOperationInput(fund: Pick<FundCard, 'code' | 'name' | 'held'>, operation: 'BUY' | 'SELL') {
    const defaults = createDefaultWorkspaceInputs()
    setPendingWatchlistSelection(null)
    setPendingWatchlistGroup(defaults.watchlistGroup)
    setPendingHoldingInput(null)
    setPendingHoldingAmount(defaults.holdingAmount)
    setPendingHoldingPnl(defaults.holdingPnl)
    setPendingSipInput(null)
    setPendingSipCadence(defaults.sipCadence)
    setPendingSipWeekday(defaults.sipWeekday)
    setPendingSipMonthDay(defaults.sipMonthDay)
    setPendingSipAmount(defaults.sipAmount)

    if (operation === 'SELL' && !fund.held) {
      setActionMessage(`${fund.name} 当前还没有持仓可卖出`)
      return
    }

    setPendingHoldingOperationInput({ code: fund.code, name: fund.name, operation })
    setPendingHoldingOperationAmount(defaults.holdingOperationAmount)
    setPendingHoldingOperationShares(defaults.holdingOperationShares)
    setPendingHoldingOperationTradeDate(defaults.holdingOperationTradeDate)
    setPendingHoldingOperationTiming(defaults.holdingOperationTiming)
    setPendingHoldingOperationFeeRate(defaults.holdingOperationFeeRate)
  }

  function handleHoldingOperationFeeRateChange(value: string) {
    setPendingHoldingOperationFeeRate(normalizeFeeRateInput(value))
  }

  async function confirmHoldingOperation() {
    const pending = getPendingHoldingOperationInput()
    const selectedFund = getData().selectedFund
    const tradeDate = getPendingHoldingOperationTradeDate()
    const effectiveTradeDate = getPendingHoldingOperationTiming() === 'AFTER_3PM' ? nextBusinessDate(tradeDate) : tradeDate
    const timing = getPendingHoldingOperationTiming()
    const feeRatePercent = Number(getPendingHoldingOperationFeeRate() || '0')

    if (!pending || !selectedFund) return
    if (!isTradeDateWithinLastThirtyDays(tradeDate)) {
      setActionMessage('补记日期需在近 30 天内，且不能晚于今天')
      return
    }
    if (!Number.isFinite(feeRatePercent) || feeRatePercent < 0) {
      setActionMessage('手续费率必须是大于等于 0 的数字')
      return
    }
    const feeRate = Number((feeRatePercent / 100).toFixed(6))

    try {
      const amount = Number(getPendingHoldingOperationAmount())
      const shares = Number(getPendingHoldingOperationShares())
      if (pending.operation === 'BUY' && (!Number.isFinite(amount) || amount <= 0)) {
        setActionMessage('买入金额必须大于 0')
        return
      }
      if (pending.operation === 'SELL' && (!Number.isFinite(shares) || shares <= 0)) {
        setActionMessage('卖出份额必须大于 0')
        return
      }

      const response =
        pending.operation === 'BUY'
          ? await workspaceApi.createHoldingOperation({
              fundCode: pending.code,
              operation: 'BUY',
              tradeDate: effectiveTradeDate,
              amount,
              feeRate,
            })
          : await workspaceApi.createHoldingOperation({
              fundCode: pending.code,
              operation: 'SELL',
              tradeDate: effectiveTradeDate,
              shares: Number(getPendingHoldingOperationShares()),
              feeRate,
            })

      setPendingHoldingOperationInput(null)
      setPendingHoldingOperationAmount('')
      setPendingHoldingOperationShares('')
      setPendingHoldingOperationTiming('BEFORE_3PM')
      setPendingHoldingOperationFeeRate('0')
      if (response.status === '确认中') {
        const actionLabel = pending.operation === 'BUY' ? '买入' : '卖出'
        const suffix = timing === 'AFTER_3PM' ? `，将按 ${effectiveTradeDate} 净值确认` : ''
        setActionMessage(`已提交 ${pending.name} 的待确认${actionLabel}记录${suffix}`)
      } else {
        setActionMessage(`已补记 ${pending.name} 的${pending.operation === 'BUY' ? '买入' : '卖出'}记录`)
      }
      await refreshCurrentPage(pending.code)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : `补记${pending.operation === 'BUY' ? '买入' : '卖出'}失败`)
    }
  }

  return {
    confirmHoldingOperation,
    handleHoldingOperationFeeRateChange,
    openHoldingOperationInput,
  }
}
