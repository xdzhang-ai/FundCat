/** 持仓买卖补记动作层，封装详情页买入/卖出入口与 /holdings/operations 提交逻辑。 */
import type { FundCard } from '@fundcat/contracts'
import type { Dispatch, SetStateAction } from 'react'
import type {
  AppDataState,
  HoldingOperationTimingInput,
  PendingHoldingInput,
  PendingHoldingOperationInput,
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
  getPendingHoldingOperationTiming: () => HoldingOperationTimingInput
  getPendingHoldingOperationFeeRate: () => string
  refreshCurrentPage: (preferredFundCode?: string) => Promise<void>
  setActionMessage: Dispatch<SetStateAction<string | null>>
  setPendingHoldingAmount: Dispatch<SetStateAction<string>>
  setPendingHoldingInput: Dispatch<SetStateAction<PendingHoldingInput | null>>
  setPendingHoldingOperationAmount: Dispatch<SetStateAction<string>>
  setPendingHoldingOperationFeeRate: Dispatch<SetStateAction<string>>
  setPendingHoldingOperationInput: Dispatch<SetStateAction<PendingHoldingOperationInput | null>>
  setPendingHoldingOperationShares: Dispatch<SetStateAction<string>>
  setPendingHoldingOperationTiming: Dispatch<SetStateAction<HoldingOperationTimingInput>>
  setPendingHoldingOperationTradeDate: Dispatch<SetStateAction<string>>
  setPendingHoldingPnl: Dispatch<SetStateAction<string>>
  setPendingSipAmount: Dispatch<SetStateAction<string>>
  setPendingSipCadence: Dispatch<SetStateAction<SipCadenceInput>>
  setPendingSipInput: Dispatch<SetStateAction<PendingSipInput | null>>
  setPendingSipMonthDay: Dispatch<SetStateAction<string>>
  setPendingSipWeekday: Dispatch<SetStateAction<SipWeekdayInput>>
  setPendingWatchlistGroups: Dispatch<SetStateAction<WatchlistGroup[]>>
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

function formatLocalDateString(date: Date) {
  const timezoneOffset = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 10)
}

function isBeforeThreePmOnTradeDate(tradeDate: string) {
  const now = new Date()
  const today = formatLocalDateString(now)
  return tradeDate === today && now.getHours() < 15
}

function toApiTradeDate(tradeDate: string, timing: HoldingOperationTimingInput) {
  if (timing === 'BEFORE_3PM') return tradeDate

  const selected = new Date(`${tradeDate}T00:00:00`)
  selected.setDate(selected.getDate() + 1)
  return formatLocalDateString(selected)
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

export function createHoldingOperationActions({
  getData,
  getPendingHoldingOperationAmount,
  getPendingHoldingOperationFeeRate,
  getPendingHoldingOperationInput,
  getPendingHoldingOperationShares,
  getPendingHoldingOperationTiming,
  getPendingHoldingOperationTradeDate,
  refreshCurrentPage,
  setActionMessage,
  setPendingHoldingAmount,
  setPendingHoldingInput,
  setPendingHoldingOperationAmount,
  setPendingHoldingOperationFeeRate,
  setPendingHoldingOperationInput,
  setPendingHoldingOperationShares,
  setPendingHoldingOperationTiming,
  setPendingHoldingOperationTradeDate,
  setPendingHoldingPnl,
  setPendingSipAmount,
  setPendingSipCadence,
  setPendingSipInput,
  setPendingSipMonthDay,
  setPendingSipWeekday,
  setPendingWatchlistGroups,
  setPendingWatchlistSelection,
}: CreateHoldingOperationActionsOptions) {
  function openHoldingOperationInput(fund: Pick<FundCard, 'code' | 'name' | 'held'>, operation: 'BUY' | 'SELL') {
    const defaults = createDefaultWorkspaceInputs()
    setPendingWatchlistSelection(null)
    setPendingWatchlistGroups(defaults.watchlistGroups)
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
    const timing = getPendingHoldingOperationTiming()
    const feeRatePercent = Number(getPendingHoldingOperationFeeRate() || '0')

    if (!pending || !selectedFund) return
    if (!isTradeDateWithinLastThirtyDays(tradeDate)) {
      setActionMessage('补记日期需在近 30 天内，且不能晚于今天')
      return
    }
    if (timing === 'AFTER_3PM' && isBeforeThreePmOnTradeDate(tradeDate)) {
      setActionMessage('当前还未到 15:00，今天的补记日期不能选择 15:00 后')
      return
    }
    if (!Number.isFinite(feeRatePercent) || feeRatePercent < 0) {
      setActionMessage('手续费率必须是大于等于 0 的数字')
      return
    }

    const apiTradeDate = toApiTradeDate(tradeDate, timing)
    const feeRate = Number((feeRatePercent / 100).toFixed(6))

    try {
      if (pending.operation === 'BUY') {
        const amount = Number(getPendingHoldingOperationAmount())
        if (!Number.isFinite(amount) || amount <= 0) {
          setActionMessage('买入金额必须大于 0')
          return
        }

        await workspaceApi.createHoldingOperation({
          fundCode: pending.code,
          operation: 'BUY',
          tradeDate: apiTradeDate,
          amount,
          feeRate,
        })
      } else {
        const shares = Number(getPendingHoldingOperationShares())
        if (!Number.isFinite(shares) || shares <= 0) {
          setActionMessage('卖出份额必须大于 0')
          return
        }

        await workspaceApi.createHoldingOperation({
          fundCode: pending.code,
          operation: 'SELL',
          tradeDate: apiTradeDate,
          shares,
          feeRate,
        })
      }

      setPendingHoldingOperationInput(null)
      setPendingHoldingOperationAmount('')
      setPendingHoldingOperationShares('')
      setPendingHoldingOperationTiming('AFTER_3PM')
      setPendingHoldingOperationFeeRate('0')
      setActionMessage(`已补记 ${pending.name} 的${pending.operation === 'BUY' ? '买入' : '卖出'}记录`)
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
