/** 持仓模块动作层，封装新增/修改持仓以及与基金状态联动的更新逻辑。 */
import type { FundCard, FundDetail, HoldingLot, PortfolioSummary } from '@fundcat/contracts'
import type { Dispatch, SetStateAction } from 'react'
import type {
  AppDataState,
  LocalHoldingDraft,
  PendingHoldingInput,
  PendingSipInput,
  PendingWatchlistSelection,
  SipCadenceInput,
  SipWeekdayInput,
  WatchlistGroup,
} from '../../../common/appTypes'
import { createDefaultWorkspaceInputs } from '../../../common/workspace/state'
import { findHoldingTarget, mergeHoldingDraftsIntoPortfolios } from '../../../common/utils/holdings'

type CreateHoldingActionsOptions = {
  getData: () => AppDataState
  getLocalHoldingDrafts: () => LocalHoldingDraft[]
  getPendingHoldingAmount: () => string
  getPendingHoldingInput: () => PendingHoldingInput | null
  getPendingHoldingPnl: () => string
  setActionMessage: Dispatch<SetStateAction<string | null>>
  setData: Dispatch<SetStateAction<AppDataState>>
  setFundDetailCache: Dispatch<SetStateAction<Record<string, FundDetail>>>
  setFundSuggestions: Dispatch<SetStateAction<FundCard[]>>
  setLocalHoldingDrafts: Dispatch<SetStateAction<LocalHoldingDraft[]>>
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
  getLocalHoldingDrafts,
  getPendingHoldingAmount,
  getPendingHoldingInput,
  getPendingHoldingPnl,
  setActionMessage,
  setData,
  setFundDetailCache,
  setFundSuggestions,
  setLocalHoldingDrafts,
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
  function mergeLocalHoldingDrafts(portfolios: PortfolioSummary[] | null) {
    return mergeHoldingDraftsIntoPortfolios(portfolios, getLocalHoldingDrafts())
  }

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
    const existingHolding = findHoldingTarget(fund.code, mergeLocalHoldingDrafts(getData().portfolios))
    if (mode === 'edit' && !existingHolding) {
      setActionMessage(`${fund.name} 当前还没有持仓可修改`)
      return
    }
    setPendingHoldingInput({ code: fund.code, name: fund.name, mode })
    setPendingHoldingAmount(mode === 'edit' && existingHolding ? formatHoldingFieldValue(existingHolding.holding.currentValue) : '')
    setPendingHoldingPnl(mode === 'edit' && existingHolding ? formatHoldingFieldValue(existingHolding.holding.pnl) : '')
  }

  function confirmAddHolding() {
    const pending = getPendingHoldingInput()
    const selectedFund = getData().selectedFund
    const portfolios = getData().portfolios
    const amount = Number(getPendingHoldingAmount())
    const pnl = Number(getPendingHoldingPnl())

    if (!pending || !selectedFund) return
    if (!portfolios?.length) {
      setActionMessage(`当前没有可用组合，暂时无法${pending.mode === 'edit' ? '修改' : '加入'}持仓`)
      return
    }
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

    const shares = Number((amount / currentPrice).toFixed(4))
    const averageCost = Number((costBasis / shares).toFixed(4))
    const updatedAt = new Date().toISOString()
    const effectivePortfolios = mergeLocalHoldingDrafts(portfolios)
    const existingHolding = findHoldingTarget(pending.code, effectivePortfolios)
    const targetPortfolio = portfolios.find((portfolio) => portfolio.id === existingHolding?.portfolioId) ?? portfolios[0]
    const newHolding: HoldingLot = {
      id: existingHolding?.holding.id ?? `manual-${pending.code}-${Date.now()}`,
      fundCode: pending.code,
      fundName: pending.name,
      shares,
      averageCost,
      currentValue: Number(amount.toFixed(4)),
      pnl: Number(pnl.toFixed(4)),
      allocation: 0,
      source: existingHolding?.holding.source ?? 'manual',
      updatedAt,
    }

    setLocalHoldingDrafts((current) => [
      { ...newHolding, portfolioId: targetPortfolio.id },
      ...current.filter((draft) => !(draft.portfolioId === targetPortfolio.id && draft.fundCode === pending.code)),
    ])
    setData((current) => ({
      ...current,
      funds: current.funds?.map((item) => (item.code === pending.code ? { ...item, held: true } : item)) ?? null,
      selectedFund: current.selectedFund?.code === pending.code ? { ...current.selectedFund, held: true } : current.selectedFund,
    }))
    setFundSuggestions((current) => current.map((item) => (item.code === pending.code ? { ...item, held: true } : item)))
    setFundDetailCache((current) => {
      const detail = current[pending.code]
      if (!detail) return current
      return {
        ...current,
        [pending.code]: {
          ...detail,
          held: true,
        },
      }
    })
    setPendingHoldingInput(null)
    setPendingHoldingAmount('')
    setPendingHoldingPnl('')
    setActionMessage(pending.mode === 'edit' ? `已更新 ${pending.name} 的持仓` : `已将 ${pending.name} 加入持仓`)
  }

  return {
    confirmAddHolding,
    openHoldingInput,
  }
}
