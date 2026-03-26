/** 定投详情页模型层，负责计划状态、执行记录和定投周期描述的纯逻辑。 */
import type { SipPlan } from '@fundcat/contracts'

export type SipRecord = {
  id: string
  executedOn: string
  amount: number
  status: '已执行' | '已暂停' | '已停止'
}

export type SipCadenceInput = 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type SipWeekdayInput = '1' | '2' | '3' | '4' | '5' | '6' | '0'

export function buildSipRecords(plan: SipPlan, status: ReturnType<typeof resolveSipStatus>): SipRecord[] {
  const baseDate = new Date(plan.nextRunAt)
  const dayStep = cadenceDayStep(plan.cadence)
  const now = Date.now()

  return Array.from({ length: 6 }, (_, index) => {
    const executedAt = new Date(baseDate)
    executedAt.setDate(executedAt.getDate() - dayStep * (index + 1))
    return {
      id: `${plan.id}-${index}`,
      executedOn: executedAt.toISOString().slice(0, 10),
      amount: plan.amount,
      status: executedAt.getTime() <= now ? '已执行' : status === '暂停' ? '已暂停' : '已停止',
    }
  })
}

function cadenceDayStep(cadence: string) {
  switch (cadence) {
    case 'DAILY':
      return 1
    case 'WEEKLY':
      return 7
    case 'BIWEEKLY':
      return 14
    case 'MONTHLY':
      return 30
    default:
      return 7
  }
}

export function resolveSipStatus(plan: SipPlan) {
  if (plan.active) {
    return '生效' as const
  }
  return new Date(plan.nextRunAt).getTime() < Date.now() ? ('停止' as const) : ('暂停' as const)
}

export function normalizeCadence(cadence: string): SipCadenceInput {
  if (cadence === 'DAILY') return 'DAILY'
  if (cadence === 'MONTHLY') return 'MONTHLY'
  return 'WEEKLY'
}

export function deriveWeekday(nextRunAt: string): SipWeekdayInput {
  return String(new Date(nextRunAt).getDay()) as SipWeekdayInput
}

export function deriveMonthDay(nextRunAt: string) {
  return new Date(nextRunAt).getDate()
}

export function describeCadence(plan: SipPlan) {
  if (plan.cadence === 'DAILY') {
    return '每日定投'
  }
  if (plan.cadence === 'MONTHLY') {
    return `每月 ${new Date(plan.nextRunAt).getDate()} 号定投`
  }
  const weekdayMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${plan.cadence === 'BIWEEKLY' ? '双周' : '每周'} ${weekdayMap[new Date(plan.nextRunAt).getDay()]} 定投`
}

export function statusToneClass(status: ReturnType<typeof resolveSipStatus>) {
  switch (status) {
    case '生效':
      return 'border-emerald-400/25 bg-emerald-400/10 text-emerald-200'
    case '暂停':
      return 'border-white/10 bg-white/5 text-slate-300'
    case '停止':
      return 'border-red-400/25 bg-red-500/10 text-red-200'
  }
}

export function recordToneClass(status: SipRecord['status']) {
  switch (status) {
    case '已执行':
      return 'text-emerald-300'
    case '已暂停':
      return 'text-slate-300'
    case '已停止':
      return 'text-red-300'
  }
}
