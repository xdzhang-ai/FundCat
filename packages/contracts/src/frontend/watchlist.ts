/** 自选基金契约，覆盖自选列表、分组和新增/删除动作。 */

/** 自选分组名称，支持用户自定义。 */
export type WatchlistGroupCode = string

/** 用户分组选项。 */
export type WatchlistGroupOption = {
  id: string
  name: WatchlistGroupCode
}

/**
 * 自选列表项。
 * 说明：
 * 1. 保持列表粒度，不返回基金详情大字段。
 * 2. group 由后端直接返回，分组已经是用户维度字段。
 */
export type WatchlistItem = {
  code: string
  name: string
  note: string
  estimatedGrowth: number
  unitNav: number
  estimatedNav: number
  group: WatchlistGroupCode
  held?: boolean
}

/** 新增自选请求。 */
export type CreateWatchlistPayload = {
  fundCode: string
  note: string
  group: WatchlistGroupCode
}

/** 更新自选分组请求。 */
export type UpdateWatchlistGroupsPayload = {
  fundCodes: string[]
  group: WatchlistGroupCode
}

/** 新增自选分组请求。 */
export type CreateWatchlistGroupPayload = {
  name: WatchlistGroupCode
}

/** 自选列表响应。 */
export type WatchlistListResponse = WatchlistItem[]

/** 自选分组选项响应。 */
export type WatchlistGroupListResponse = WatchlistGroupOption[]
