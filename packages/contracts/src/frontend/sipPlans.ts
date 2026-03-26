/** 定投计划契约，覆盖计划列表、计划详情、编辑动作和执行记录。 */
import type { DateString, IsoDateTimeString } from './shared'

/** 定投周期枚举。 */
export type SipCadence = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'BIWEEKLY'

/** 定投状态枚举。 */
export type SipStatus = '生效' | '暂停' | '停止'

/**
 * 定投计划。
 * 说明：
 * 1. 保留现有 active + nextRunAt 兼容前端现状。
 * 2. 同时补充 status，便于后端直接给出最终状态，减少前端反推。
 */
export type SipPlan = {
  id: string
  fundCode: string
  fundName: string
  amount: number
  cadence: SipCadence | string
  nextRunAt: IsoDateTimeString
  active: boolean
  status?: SipStatus
}

/** 创建定投计划请求。 */
export type CreateSipPlanPayload = {
  portfolioId: string
  fundCode: string
  amount: number
  cadence: SipCadence | string
  nextRunAt: IsoDateTimeString
}

/** 编辑定投计划请求，仅允许修改当前前端真实开放的字段。 */
export type UpdateSipPlanPayload = {
  amount: number
  cadence: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  weekday?: '0' | '1' | '2' | '3' | '4' | '5' | '6'
  monthDay?: string
}

/** 定投执行记录。 */
export type SipExecutionRecord = {
  id: string
  sipPlanId: string
  executedOn: DateString
  amount: number
  status: '已执行' | '已暂停' | '已停止'
}

/** 定投计划详情页响应，建议由计划基础信息和执行记录组成。 */
export type SipPlanDetailResponse = {
  plan: SipPlan
  records: SipExecutionRecord[]
}

/** 概览页上的定投摘要卡片项。 */
export type SipPlanDigest = {
  id: string
  fundCode: string
  fundName: string
  amount: number
  cadenceLabel: string
  nextRunOn?: DateString
  status: SipStatus
}

/** 周报。 */
export type WeeklyReport = {
  id: string
  weekLabel: string
  summary: string
  returnRate: number
  bestFundCode: string
  riskNote: string
}

/** 提醒规则。 */
export type AlertRule = {
  id: string
  fundCode: string
  ruleType: string
  thresholdValue: number
  enabled: boolean
  channel: string
}

/** OCR / 导入任务。 */
export type ImportJob = {
  id: string
  sourcePlatform: string
  status: string
  fileName: string
  recognizedHoldings: number
  createdAt: IsoDateTimeString
}

/** 创建导入任务请求。 */
export type CreateImportJobPayload = {
  sourcePlatform: string
  fileName: string
}
