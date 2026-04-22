/** 定投详情页模型层，负责计划状态、执行记录和定投周期描述的纯逻辑。 */
import type { SipExecutionRecord, SipPlan } from '@fundcat/contracts'

export type SipCadenceInput = 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type SipWeekdayInput = '1' | '2' | '3' | '4' | '5' | '6' | '0'

export function resolveSipStatus(plan: SipPlan) {
  if (plan.status === '生效' || plan.status === '暂停' || plan.status === '停止') {
    return plan.status
  }
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

export function recordToneClass(status: SipExecutionRecord['status']) {
  switch (status) {
    case '已执行':
      return 'text-emerald-300'
    case '确认中':
      return 'text-sky-300'
  }
}
