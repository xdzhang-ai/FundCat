/** 定投计划契约。 */
import type { DateString, IsoDateTimeString } from './shared'

export type SipCadence = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'BIWEEKLY'
export type SipStatus = '生效' | '暂停' | '停止'
export type SipExecutionStatus = '确认中' | '已执行'

export type SipPlan = {
  id: string
  fundCode: string
  fundName: string
  amount: number
  cadence: SipCadence | string
  nextRunAt: IsoDateTimeString
  active: boolean
  status: SipStatus
  feeRate: number
}

export type CreateSipPlanPayload = {
  portfolioId?: string
  fundCode: string
  amount: number
  cadence: SipCadence | string
  nextRunAt: IsoDateTimeString
  feeRate: number
}

export type UpdateSipPlanPayload = {
  amount: number
  cadence: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'BIWEEKLY'
  weekday?: '0' | '1' | '2' | '3' | '4' | '5' | '6'
  monthDay?: string
  feeRate: number
}

export type SipExecutionRecord = {
  id: string
  sipPlanId: string
  executedOn: DateString
  amount: number
  status: SipExecutionStatus
  feeRate: number
  feeAmount: number
}

export type SipPlanDetailResponse = {
  plan: SipPlan
  records: SipExecutionRecord[]
}

export type SipPlanDigest = {
  id: string
  fundCode: string
  fundName: string
  amount: number
  cadenceLabel: string
  nextRunOn?: DateString
  status: SipStatus
}

export type WeeklyReport = {
  id: string
  weekLabel: string
  summary: string
  returnRate: number
  bestFundCode: string
  riskNote: string
}

export type AlertRule = {
  id: string
  fundCode: string
  ruleType: string
  thresholdValue: number
  enabled: boolean
  channel: string
}

export type ImportJob = {
  id: string
  sourcePlatform: string
  status: string
  fileName: string
  recognizedHoldings: number
  createdAt: IsoDateTimeString
}

export type CreateImportJobPayload = {
  sourcePlatform: string
  fileName: string
}
