/** 持仓模块动作层，封装新增/修改持仓以及与基金状态联动的更新逻辑。 */
import type { FundCard, HoldingInsight } from '@fundcat/contracts'
import type { Dispatch, SetStateAction } from 'react'
import type { AppDataState, PendingHoldingInput, PendingSipInput, PendingWatchlistSelection, SipCadenceInput, SipWeekdayInput, WatchlistGroup } from '../../../common/appTypes'
import { workspaceApi } from '../../../common/data/workspaceApi'
import { createDefaultWorkspaceInputs } from '../../../common/workspace/state'

type CreateHoldingActionsOptions = {
  getData: () => AppDataState
  getPendingHoldingAmount: () => string
  getPendingHoldingInput: () => PendingHoldingInput | null
  getPendingHoldingPnl: () => string
  getSelectedFundHoldingInsight: () => HoldingInsight | null
  refreshCurrentPage: (preferredFundCode?: string) => Promise<void>
  setActionMessage: Dispatch<SetStateAction<string | null>>
  setData: Dispatch<SetStateAction<AppDataState>>
  setPendingHoldingAmount: Dispatch<SetStateAction<string>>
  setPendingHoldingInput: Dispatch<SetStateAction<PendingHoldingInput | null>>
  setPendingHoldingPnl: Dispatch<SetStateAction<string>>
  setPendingSipAmount: Dispatch<SetStateAction<string>>
  setPendingSipCadence: Dispatch<SetStateAction<SipCadenceInput>>
  setPendingSipInput: Dispatch<SetStateAction<PendingSipInput | null>>
  setPendingSipMonthDay: Dispatch<SetStateAction<string>>
  setPendingSipWeekday: Dispatch<SetStateAction<SipWeekdayInput>>
  setPendingWatchlistGroups: Dispatch<SetStateAction<WatchlistGroup[]>>
  setPendingWatchlistSelection: Dispatch<SetStateAction<PendingWatchlistSelection | null>>
}

export function createHoldingActions({
  getData,
  getPendingHoldingAmount,
  getPendingHoldingInput,
  getPendingHoldingPnl,
  getSelectedFundHoldingInsight,
  refreshCurrentPage,
  setActionMessage,
  setData,
  setPendingHoldingAmount,
  setPendingHoldingInput,
  setPendingHoldingPnl,
  setPendingSipAmount,
  setPendingSipCadence,
  setPendingSipInput,
  setPendingSipMonthDay,
  setPendingSipWeekday,
  setPendingWatchlistGroups,
  setPendingWatchlistSelection,
}: CreateHoldingActionsOptions) {
  function formatHoldingFieldValue(value: number) {
    return Number.isFinite(value) ? String(Number(value.toFixed(2))) : ''
  }

  function openHoldingInput(fund: Pick<FundCard, 'code' | 'name'>, mode: 'add' | 'edit' = 'add') {
    const defaults = createDefaultWorkspaceInputs()
    setPendingWatchlistSelection(null)
    setPendingWatchlistGroups(defaults.watchlistGroups)
    setPendingSipInput(null)
    setPendingSipCadence(defaults.sipCadence)
    setPendingSipWeekday(defaults.sipWeekday)
    setPendingSipMonthDay(defaults.sipMonthDay)
    setPendingSipAmount(defaults.sipAmount)
    const existingHolding = getSelectedFundHoldingInsight()
    if (mode === 'edit' && !existingHolding) {
      setActionMessage(`${fund.name} 当前还没有持仓可修改`)
      return
    }
    setPendingHoldingInput({ code: fund.code, name: fund.name, mode })
    setPendingHoldingAmount(mode === 'edit' && existingHolding ? formatHoldingFieldValue(existingHolding.amountHeld) : '')
    setPendingHoldingPnl(mode === 'edit' && existingHolding ? formatHoldingFieldValue(existingHolding.holdingPnl) : '')
  }

  async function confirmAddHolding() {
    const pending = getPendingHoldingInput()
    const selectedFund = getData().selectedFund
    const amount = Number(getPendingHoldingAmount())
    const pnl = Number(getPendingHoldingPnl())

    if (!pending || !selectedFund) return
    if (!Number.isFinite(amount) || amount <= 0) {
      setActionMessage('持有金额必须大于 0')
      return
    }
    if (!Number.isFinite(pnl)) {
      setActionMessage('持有收益必须是有效数字')
      return
    }

    const currentPrice = selectedFund.referenceOnly ? selectedFund.estimatedNav : selectedFund.unitNav
    if (!Number.isFinite(currentPrice) || currentPrice <= 0) {
      setActionMessage(`当前基金净值异常，暂时无法${pending.mode === 'edit' ? '修改' : '添加'}持仓`)
      return
    }

    const costBasis = amount - pnl
    if (costBasis <= 0) {
      setActionMessage('持有收益不能大于等于持有金额')
      return
    }

    try {
      const payload = {
        fundCode: pending.code,
        amountBasis: selectedFund.referenceOnly ? ('T' as const) : ('T_MINUS_1' as const),
        amount,
        holdingPnl: pnl,
      }

      if (pending.mode === 'edit') {
        await workspaceApi.updateHolding(pending.code, payload)
      } else {
        await workspaceApi.createHolding(payload)
        const executedAt = new Date().toISOString()
        setData((current) => ({
          ...current,
          localHoldingHistory: [
            {
              id: `holding-build-${pending.code}-${Date.now()}`,
              fundCode: pending.code,
              fundName: pending.name,
              historyType: 'BUILD',
              amount: Number(amount.toFixed(2)),
              shares: Number((amount / currentPrice).toFixed(4)),
              fee: 0,
              feeRate: 0,
              status: '已执行',
              tradeDate: executedAt.slice(0, 10),
              executedAt,
              source: 'HOLDING_SNAPSHOT',
            },
            ...current.localHoldingHistory,
          ],
        }))
      }

      setPendingHoldingInput(null)
      setPendingHoldingAmount('')
      setPendingHoldingPnl('')
      setActionMessage(pending.mode === 'edit' ? `已更新 ${pending.name} 的持仓` : `已将 ${pending.name} 加入持仓`)
      await refreshCurrentPage(pending.code)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : `${pending.mode === 'edit' ? '修改' : '新增'}持仓失败`)
    }
  }

  return {
    confirmAddHolding,
    openHoldingInput,
  }
}
