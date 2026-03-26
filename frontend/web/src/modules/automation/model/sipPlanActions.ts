/** 定投模块动作层，封装创建、跳转、编辑、暂停和停止计划等交互逻辑。 */
import type { FundCard, SipPlan } from '@fundcat/contracts'
import type { Dispatch, SetStateAction } from 'react'
import type {
  AppDataState,
  LocalSipPlanDraft,
  PendingHoldingInput,
  PendingSipInput,
  PendingWatchlistSelection,
  SipCadenceInput,
  SipWeekdayInput,
  WatchlistGroup,
} from '../../../common/appTypes'
import { workspaceApi } from '../../../common/data/workspaceApi'
import { createDefaultWorkspaceInputs } from '../../../common/workspace/state'
import {
  findCurrentSipPlanByFundCode,
  findSipPlanByFundCode,
  mergeLocalSipPlanDrafts,
  resolveSipNextRunAt,
  sipCadenceLabel,
} from '../../../common/utils/sipPlans'

type CreateSipPlanActionsOptions = {
  getData: () => AppDataState
  getLocalSipPlanDrafts: () => LocalSipPlanDraft[]
  getPendingSipAmount: () => string
  getPendingSipCadence: () => SipCadenceInput
  getPendingSipInput: () => PendingSipInput | null
  getPendingSipMonthDay: () => string
  getPendingSipWeekday: () => SipWeekdayInput
  navigateToPath: (path: string) => void
  setActionMessage: Dispatch<SetStateAction<string | null>>
  setData: Dispatch<SetStateAction<AppDataState>>
  setLocalSipPlanDrafts: Dispatch<SetStateAction<LocalSipPlanDraft[]>>
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

export function createSipPlanActions({
  getData,
  getLocalSipPlanDrafts,
  getPendingSipAmount,
  getPendingSipCadence,
  getPendingSipInput,
  getPendingSipMonthDay,
  getPendingSipWeekday,
  navigateToPath,
  setActionMessage,
  setData,
  setLocalSipPlanDrafts,
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
}: CreateSipPlanActionsOptions) {
  function openSipInput(fund: Pick<FundCard, 'code' | 'name'>) {
    const existingPlan = findCurrentSipPlanByFundCode(
      fund.code,
      mergeLocalSipPlanDrafts(getData().sipPlans, getLocalSipPlanDrafts()),
    )
    if (existingPlan) {
      navigateToPath(`/automation/${existingPlan.id}`)
      return
    }
    const defaults = createDefaultWorkspaceInputs()
    setPendingWatchlistSelection(null)
    setPendingWatchlistGroups(defaults.watchlistGroups)
    setPendingHoldingInput(null)
    setPendingHoldingAmount(defaults.holdingAmount)
    setPendingHoldingPnl(defaults.holdingPnl)
    setPendingSipInput({ code: fund.code, name: fund.name })
    setPendingSipCadence(defaults.sipCadence)
    setPendingSipWeekday(defaults.sipWeekday)
    setPendingSipMonthDay(defaults.sipMonthDay)
    setPendingSipAmount(defaults.sipAmount)
  }

  async function confirmAddSip() {
    const pending = getPendingSipInput()
    const selectedFund = getData().selectedFund
    const portfolios = getData().portfolios
    const amount = Number(getPendingSipAmount())

    if (!pending || !selectedFund) return
    if (!portfolios?.length) {
      setActionMessage('当前没有可用组合，暂时无法创建定投计划')
      return
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setActionMessage('定投金额必须大于 0')
      return
    }
    if (
      findCurrentSipPlanByFundCode(
        pending.code,
        mergeLocalSipPlanDrafts(getData().sipPlans, getLocalSipPlanDrafts()),
      )
    ) {
      setActionMessage(`${pending.name} 已设定投计划`)
      return
    }

    const cadence = getPendingSipCadence()
    const nextRunAt = resolveSipNextRunAt(cadence, {
      weekday: getPendingSipWeekday(),
      monthDay: getPendingSipMonthDay(),
    })

    try {
      await workspaceApi.createSip({
        portfolioId: portfolios[0].id,
        fundCode: pending.code,
        amount,
        cadence,
        nextRunAt: nextRunAt.toISOString().slice(0, 19),
      })
      const sipPlans = await workspaceApi.sipPlans()
      setData((current) => ({ ...current, sipPlans }))
      const defaults = createDefaultWorkspaceInputs()
      setPendingSipInput(null)
      setPendingSipCadence(defaults.sipCadence)
      setPendingSipWeekday(defaults.sipWeekday)
      setPendingSipMonthDay(defaults.sipMonthDay)
      setPendingSipAmount(defaults.sipAmount)
      setActionMessage(`已为 ${pending.name} 创建${sipCadenceLabel(cadence)}计划`)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : '创建定投计划失败')
    }
  }

  function updateLocalSipPlan(planId: string, updater: (plan: SipPlan) => SipPlan) {
    const effectiveSipPlans = mergeLocalSipPlanDrafts(getData().sipPlans, getLocalSipPlanDrafts())
    const targetPlan = effectiveSipPlans?.find((plan) => plan.id === planId)
    if (!targetPlan) return null
    const nextPlan = updater(targetPlan)
    setLocalSipPlanDrafts((current) => [...current.filter((draft) => draft.id !== planId), nextPlan])
    return nextPlan
  }

  function handleOpenSipPlan(code: string) {
    const effectivePlans = mergeLocalSipPlanDrafts(getData().sipPlans, getLocalSipPlanDrafts())
    const plan = findCurrentSipPlanByFundCode(code, effectivePlans) ?? findSipPlanByFundCode(code, effectivePlans)
    if (!plan) {
      setActionMessage('当前基金还没有定投计划')
      return
    }
    navigateToPath(`/automation/${plan.id}`)
  }

  function handleEditSipPlan(
    planId: string,
    payload: {
      amount: number
      cadence: SipCadenceInput
      weekday?: SipWeekdayInput
      monthDay?: string
    },
  ) {
    const nextRunAt = resolveSipNextRunAt(payload.cadence, {
      weekday: payload.weekday,
      monthDay: payload.monthDay,
    })
    const updatedPlan = updateLocalSipPlan(planId, (plan) => ({
      ...plan,
      amount: Number(payload.amount.toFixed(2)),
      cadence: payload.cadence,
      nextRunAt: nextRunAt.toISOString().slice(0, 19),
      active: true,
    }))
    if (updatedPlan) {
      setActionMessage(`已更新 ${updatedPlan.fundName} 的定投计划`)
    }
  }

  function handleToggleSipPlan(planId: string) {
    const updatedPlan = updateLocalSipPlan(planId, (plan) => {
      if (plan.active) {
        return {
          ...plan,
          active: false,
        }
      }
      const nextRunAt = resolveSipNextRunAt(
        plan.cadence === 'MONTHLY' ? 'MONTHLY' : plan.cadence === 'DAILY' ? 'DAILY' : 'WEEKLY',
        {
          weekday: String(new Date(plan.nextRunAt).getDay()) as SipWeekdayInput,
          monthDay: String(new Date(plan.nextRunAt).getDate()),
        },
      )
      return {
        ...plan,
        active: true,
        nextRunAt: nextRunAt.toISOString().slice(0, 19),
      }
    })
    if (updatedPlan) {
      setActionMessage(updatedPlan.active ? `已恢复 ${updatedPlan.fundName} 的定投计划` : `已暂停 ${updatedPlan.fundName} 的定投计划`)
    }
  }

  function handleStopSipPlan(planId: string) {
    const updatedPlan = updateLocalSipPlan(planId, (plan) => {
      const stoppedAt = new Date()
      stoppedAt.setDate(stoppedAt.getDate() - 1)
      return {
        ...plan,
        active: false,
        nextRunAt: stoppedAt.toISOString().slice(0, 19),
      }
    })
    if (updatedPlan) {
      setActionMessage(`已停止 ${updatedPlan.fundName} 的定投计划`)
    }
  }

  return {
    confirmAddSip,
    handleEditSipPlan,
    handleOpenSipPlan,
    handleStopSipPlan,
    handleToggleSipPlan,
    openSipInput,
  }
}
