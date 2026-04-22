/** 定投模块动作层，封装创建、跳转、编辑、暂停和停止计划等交互逻辑。 */
import type { FundCard } from '@fundcat/contracts'
import type { Dispatch, SetStateAction } from 'react'
import type {
  AppDataState,
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
  resolveSipNextRunAt,
  resolveSipStatus,
  sipCadenceLabel,
} from '../../../common/utils/sipPlans'

type CreateSipPlanActionsOptions = {
  getData: () => AppDataState
  getPendingSipAmount: () => string
  getPendingSipCadence: () => SipCadenceInput
  getPendingSipInput: () => PendingSipInput | null
  getPendingSipMonthDay: () => string
  getPendingSipWeekday: () => SipWeekdayInput
  navigateToPath: (path: string) => void
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
  setPendingWatchlistGroup: Dispatch<SetStateAction<WatchlistGroup>>
  setPendingWatchlistSelection: Dispatch<SetStateAction<PendingWatchlistSelection | null>>
}

export function createSipPlanActions({
  getData,
  getPendingSipAmount,
  getPendingSipCadence,
  getPendingSipInput,
  getPendingSipMonthDay,
  getPendingSipWeekday,
  navigateToPath,
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
  setPendingWatchlistGroup,
  setPendingWatchlistSelection,
}: CreateSipPlanActionsOptions) {
  function openSipInput(fund: Pick<FundCard, 'code' | 'name'>) {
    const existingPlan = findCurrentSipPlanByFundCode(fund.code, getData().sipPlans)
    if (existingPlan) {
      navigateToPath(`/sip/${existingPlan.id}`)
      return
    }
    const defaults = createDefaultWorkspaceInputs()
    setPendingWatchlistSelection(null)
    setPendingWatchlistGroup(defaults.watchlistGroup)
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
    const amount = Number(getPendingSipAmount())

    if (!pending || !selectedFund) return
    if (!Number.isFinite(amount) || amount <= 0) {
      setActionMessage('定投金额必须大于 0')
      return
    }
    if (findCurrentSipPlanByFundCode(pending.code, getData().sipPlans)) {
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
        fundCode: pending.code,
        amount,
        cadence,
        nextRunAt: nextRunAt.toISOString().slice(0, 19),
        feeRate: 0,
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

  function handleOpenSipPlan(code: string) {
    const plan = findCurrentSipPlanByFundCode(code, getData().sipPlans) ?? findSipPlanByFundCode(code, getData().sipPlans)
    if (!plan) {
      setActionMessage('当前基金还没有定投计划')
      return
    }
    navigateToPath(`/sip/${plan.id}`)
  }

  async function handleEditSipPlan(
    planId: string,
    payload: {
      amount: number
      cadence: SipCadenceInput
      weekday?: SipWeekdayInput
      monthDay?: string
    },
  ) {
    try {
      const updatedPlan = await workspaceApi.updateSip(planId, {
        amount: Number(payload.amount.toFixed(2)),
        cadence: payload.cadence,
        weekday: payload.cadence === 'WEEKLY' ? payload.weekday : undefined,
        monthDay: payload.cadence === 'MONTHLY' ? payload.monthDay : undefined,
        feeRate: 0,
      })
      const [sipPlans, records] = await Promise.all([workspaceApi.sipPlans(), workspaceApi.sipRecords(planId)])
      setData((current) => ({
        ...current,
        sipPlans,
        sipRecordsByPlanId: {
          ...current.sipRecordsByPlanId,
          [planId]: records,
        },
      }))
      setActionMessage(`已更新 ${updatedPlan.fundName} 的定投计划`)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : '更新定投计划失败')
    }
  }

  async function handleToggleSipPlan(planId: string) {
    const plan = getData().sipPlans?.find((item) => item.id === planId)
    if (!plan) return

    try {
      const updatedPlan =
        resolveSipStatus(plan) === '生效'
          ? await workspaceApi.pauseSip(planId)
          : await workspaceApi.resumeSip(planId)
      const [sipPlans, records] = await Promise.all([workspaceApi.sipPlans(), workspaceApi.sipRecords(planId)])
      setData((current) => ({
        ...current,
        sipPlans,
        sipRecordsByPlanId: {
          ...current.sipRecordsByPlanId,
          [planId]: records,
        },
      }))
      setActionMessage(updatedPlan.active ? `已恢复 ${updatedPlan.fundName} 的定投计划` : `已暂停 ${updatedPlan.fundName} 的定投计划`)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : '更新定投状态失败')
    }
  }

  async function handleStopSipPlan(planId: string) {
    try {
      const updatedPlan = await workspaceApi.stopSip(planId)
      const [sipPlans, records] = await Promise.all([workspaceApi.sipPlans(), workspaceApi.sipRecords(planId)])
      setData((current) => ({
        ...current,
        sipPlans,
        sipRecordsByPlanId: {
          ...current.sipRecordsByPlanId,
          [planId]: records,
        },
      }))
      setActionMessage(`已停止 ${updatedPlan.fundName} 的定投计划`)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : '停止定投计划失败')
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
