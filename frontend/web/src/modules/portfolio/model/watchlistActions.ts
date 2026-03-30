/** 自选模块动作层，处理加自选、移除、自选分组和相关弹层状态。 */
import type { FundCard, FundDetail } from '@fundcat/contracts'
import type { Dispatch, SetStateAction } from 'react'
import { workspaceApi } from '../../../common/data/workspaceApi'
import type {
  AppDataState,
  PendingHoldingInput,
  PendingSipInput,
  PendingWatchlistSelection,
  SipCadenceInput,
  SipWeekdayInput,
  WatchlistGroup,
} from '../../../common/appTypes'
import { createDefaultWorkspaceInputs, createEmptyAppData } from '../../../common/workspace/state'

type SetData = Dispatch<SetStateAction<AppDataState>>
type SetFundSuggestions = Dispatch<SetStateAction<FundCard[]>>
type SetFundDetailCache = Dispatch<SetStateAction<Record<string, FundDetail>>>

type CreateWatchlistActionsOptions = {
  getData: () => AppDataState
  getPendingWatchlistSelection: () => PendingWatchlistSelection | null
  getPendingWatchlistGroups: () => WatchlistGroup[]
  getSelectedCode: () => string
  refreshCurrentPage: (preferredFundCode?: string) => Promise<void>
  setActionMessage: Dispatch<SetStateAction<string | null>>
  setData: SetData
  setFundDetailCache: SetFundDetailCache
  setFundSuggestions: SetFundSuggestions
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
  setWatchlistGroups: Dispatch<SetStateAction<Record<string, WatchlistGroup[]>>>
}

export function createWatchlistActions({
  getData,
  getPendingWatchlistGroups,
  getPendingWatchlistSelection,
  getSelectedCode,
  refreshCurrentPage,
  setActionMessage,
  setData,
  setFundDetailCache,
  setFundSuggestions,
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
  setWatchlistGroups,
}: CreateWatchlistActionsOptions) {
  function hydrateWatchlistGroups(watchlist: AppDataState['watchlist']): Record<string, WatchlistGroup[]> {
    return Object.fromEntries((watchlist ?? []).map((item) => [item.code, item.groups?.length ? item.groups : ['全部']]))
  }

  function normalizePersistedGroups(groups: WatchlistGroup[]) {
    return groups.filter((group) => group !== '全部')
  }

  function openWatchlistPicker(fund: Pick<FundCard, 'code' | 'name' | 'watchlisted'>) {
    if (fund.watchlisted) {
      setActionMessage(`${fund.name} 已在自选基金中`)
      return
    }
    const defaults = createDefaultWorkspaceInputs()
    setPendingHoldingInput(null)
    setPendingHoldingAmount(defaults.holdingAmount)
    setPendingHoldingPnl(defaults.holdingPnl)
    setPendingSipInput(null)
    setPendingSipCadence(defaults.sipCadence)
    setPendingSipWeekday(defaults.sipWeekday)
    setPendingSipMonthDay(defaults.sipMonthDay)
    setPendingSipAmount(defaults.sipAmount)
    setPendingWatchlistSelection({ code: fund.code, name: fund.name })
    setPendingWatchlistGroups(defaults.watchlistGroups)
  }

  function togglePendingWatchlistGroup(group: WatchlistGroup) {
    if (group === '全部') {
      return
    }
    setPendingWatchlistGroups((current) => {
      if (current.includes(group)) {
        return current.filter((item) => item !== group)
      }
      if (current.length >= 2) {
        return current
      }
      return [...current, group]
    })
  }

  function assignWatchlistGroup(codes: string[], group: Exclude<WatchlistGroup, '全部'>) {
    if (codes.length === 0) return
    const currentWatchlist = getData().watchlist ?? []
    const hasActualChange = codes.some((code) => {
      const currentGroups = currentWatchlist.find((item) => item.code === code)?.groups ?? []
      return currentGroups.length !== 1 || currentGroups[0] !== group
    })
    if (!hasActualChange) {
      return
    }
    void workspaceApi
      .updateWatchlistGroups({
        fundCodes: codes,
        groups: [group],
      })
      .then((watchlist) => {
        setData((current) => ({ ...current, watchlist }))
        setWatchlistGroups(hydrateWatchlistGroups(watchlist))
      })
      .catch((error) => {
        setActionMessage(error instanceof Error ? error.message : '更新自选分组失败')
      })
  }

  async function confirmAddWatchlist() {
    const pending = getPendingWatchlistSelection()
    if (!pending) return

    try {
      const note = `新增于 ${new Date().toLocaleDateString('zh-CN')}`
      await workspaceApi.addWatchlist({
        fundCode: pending.code,
        note,
        groups: normalizePersistedGroups(getPendingWatchlistGroups()),
      })

      setActionMessage(`已将 ${pending.name} 加入自选`)
      setFundSuggestions((current) => current.map((item) => (item.code === pending.code ? { ...item, watchlisted: true } : item)))
      setFundDetailCache((current) => {
        const detail = current[pending.code]
        if (!detail) return current
        return {
          ...current,
          [pending.code]: {
            ...detail,
            watchlisted: true,
          },
        }
      })
      setData((current) => ({
        ...current,
        funds: current.funds?.map((item) => (item.code === pending.code ? { ...item, watchlisted: true } : item)) ?? null,
        selectedFund: current.selectedFund?.code === pending.code ? { ...current.selectedFund, watchlisted: true } : current.selectedFund,
      }))

      const watchlist = await workspaceApi.watchlist()
      setData((current) => ({ ...current, watchlist }))
      setWatchlistGroups(hydrateWatchlistGroups(watchlist))
      setPendingWatchlistSelection(null)
      setPendingWatchlistGroups(createDefaultWorkspaceInputs().watchlistGroups)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : '加入自选失败')
    }
  }

  function handleAddWatchlistFromFunds(fund: FundCard) {
    openWatchlistPicker(fund)
  }

  async function handleRemoveWatchlist(code: string) {
    try {
      await workspaceApi.removeWatchlist(code)
      setActionMessage(`已将 ${code} 移出自选`)
      await refreshCurrentPage(getSelectedCode() || code)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : '移出自选失败')
    }
  }

  function resetWorkspaceStateAfterLogout() {
    const defaults = createDefaultWorkspaceInputs()
    setData(createEmptyAppData())
    setWatchlistGroups({})
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
  }

  return {
    assignWatchlistGroup,
    confirmAddWatchlist,
    handleAddWatchlistFromFunds,
    handleRemoveWatchlist,
    openWatchlistPicker,
    resetWorkspaceStateAfterLogout,
    togglePendingWatchlistGroup,
  }
}
