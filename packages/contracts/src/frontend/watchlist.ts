/** 自选基金契约，覆盖自选列表、分组和新增/删除动作。 */

/** 自选分组编码，当前前端只使用这几个固定分组。 */
export type WatchlistGroupCode = '全部' | '成长进攻' | '稳健配置' | '行业主题'

/**
 * 自选列表项。
 * 说明：
 * 1. 保持列表粒度，不返回基金详情大字段。
 * 2. groups 用于后端持久化分组，避免前端继续本地拼装。
 */
export type WatchlistItem = {
  code: string
  name: string
  note: string
  estimatedGrowth: number
  unitNav: number
  estimatedNav: number
  groups?: WatchlistGroupCode[]
  held?: boolean
}

/** 新增自选请求。 */
export type CreateWatchlistPayload = {
  fundCode: string
  note: string
  groups?: WatchlistGroupCode[]
}

/** 更新自选分组请求。 */
export type UpdateWatchlistGroupsPayload = {
  fundCodes: string[]
  groups: WatchlistGroupCode[]
}

/** 自选列表响应。 */
export type WatchlistListResponse = WatchlistItem[]
