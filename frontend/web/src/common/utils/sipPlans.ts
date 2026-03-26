/** 定投领域纯函数，处理计划状态、下次执行时间和本地草稿合并逻辑。 */
import type { SipPlan } from '@fundcat/contracts'
import type { LocalSipPlanDraft, SipCadenceInput, SipWeekdayInput } from '../appTypes'

export const LOCAL_SIP_PLAN_DRAFTS_KEY = 'fundcat.local-sip-plan-drafts'

export function sipCadenceLabel(cadence: SipCadenceInput) {
  switch (cadence) {
    case 'DAILY':
      return '每日定投'
    case 'WEEKLY':
      return '周定投'
    case 'MONTHLY':
      return '月定投'
  }
}

export function nextWeeklyRunAt(weekday: SipWeekdayInput) {
  const now = new Date()
  const targetDay = Number(weekday)
  const nextRunAt = new Date(now)
  let offset = (targetDay - now.getDay() + 7) % 7
  if (offset === 0) {
    offset = 7
  }
  nextRunAt.setDate(now.getDate() + offset)
  return nextRunAt
}

export function nextMonthlyRunAt(dayOfMonth: number) {
  const now = new Date()
  const nextRunAt = new Date(now)
  if (now.getDate() < dayOfMonth) {
    nextRunAt.setDate(dayOfMonth)
    return nextRunAt
  }
  nextRunAt.setMonth(now.getMonth() + 1, dayOfMonth)
  return nextRunAt
}

export function resolveSipNextRunAt(cadence: SipCadenceInput, options?: { weekday?: SipWeekdayInput; monthDay?: string }) {
  switch (cadence) {
    case 'DAILY': {
      const nextRunAt = new Date()
      nextRunAt.setDate(nextRunAt.getDate() + 1)
      return nextRunAt
    }
    case 'WEEKLY':
      return nextWeeklyRunAt(options?.weekday ?? '1')
    case 'MONTHLY':
      return nextMonthlyRunAt(Number(options?.monthDay ?? '1'))
  }
}

export function mergeLocalSipPlanDrafts(plans: SipPlan[] | null, drafts: LocalSipPlanDraft[]) {
  if (!plans) return plans
  if (drafts.length === 0) return plans
  const planMap = new Map(plans.map((plan) => [plan.id, plan]))
  drafts.forEach((draft) => {
    planMap.set(draft.id, draft)
  })
  return Array.from(planMap.values())
}

export function findSipPlanByFundCode(code: string, plans: SipPlan[] | null) {
  return plans?.find((plan) => plan.fundCode === code) ?? null
}

export function resolveSipStatus(plan: SipPlan) {
  if (plan.active) {
    return '生效' as const
  }
  return new Date(plan.nextRunAt).getTime() < Date.now() ? ('停止' as const) : ('暂停' as const)
}

export function findCurrentSipPlanByFundCode(code: string, plans: SipPlan[] | null) {
  return plans?.find((plan) => plan.fundCode === code && resolveSipStatus(plan) !== '停止') ?? null
}

export function loadLocalSipPlanDrafts() {
  try {
    const raw = window.localStorage.getItem(LOCAL_SIP_PLAN_DRAFTS_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? (parsed as LocalSipPlanDraft[]) : []
  } catch {
    return []
  }
}
