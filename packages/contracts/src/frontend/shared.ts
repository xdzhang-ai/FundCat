/** 前端共享基础契约，定义跨模块复用的基础值对象和枚举。 */

/** ISO 8601 时间字符串，统一用于接口的时间字段传输。 */
export type IsoDateTimeString = string

/** 仅包含日期的字符串，格式建议为 YYYY-MM-DD。 */
export type DateString = string

/** 图表点位，前端当前只关心时间和数值本身。 */
export type TrendPoint = {
  date: IsoDateTimeString
  value: number
}

/** 后台统一成功/失败包裹结构。 */
export type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

/** 当前登录用户的最小资料。 */
export type UserProfile = {
  id: string
  displayName: string
  username: string
  riskMode: 'research'
}

/** Feature Flag 风险等级，用于前台和后台统一展示语义。 */
export type FeatureFlagRiskLevel = 'low' | 'medium' | 'high'

/** Feature Flag 最小展示模型。 */
export type FeatureFlag = {
  code: string
  name: string
  enabled: boolean
  environment: string
  description: string
  riskLevel: FeatureFlagRiskLevel
}

/** 概览指标色调，避免前后端重复约定颜色语义。 */
export type MetricTone = 'positive' | 'negative' | 'neutral'
