/** 面向后台研发的接口说明，强调按页面语义做细粒度拆分，避免无效大查询。 */

/**
 * 设计原则：
 * 1. 优先按“页面里的最小语义块”拆接口，而不是按数据库表拆接口。
 * 2. 列表页和详情页分离，避免列表接口携带重字段。
 * 3. 能由后端一次聚合好的表格数据，不让前端用多接口再拼。
 * 4. 旧的 dashboard 大聚合接口仅保留兼容，不建议继续扩张。
 */
export const frontendApiContracts = {
  auth: [
    {
      method: 'POST',
      path: '/api/v1/auth/login',
      request: 'LoginPayload',
      response: 'AuthResponse',
      note: '登录接口，返回令牌和当前用户资料。',
    },
    {
      method: 'POST',
      path: '/api/v1/auth/logout',
      request: 'void',
      response: 'void',
      note: '注销当前会话。',
    },
  ],
  overview: [
    {
      method: 'GET',
      path: '/api/v1/overview/hero-metrics',
      request: 'void',
      response: 'OverviewHeroMetricsResponse',
      note: '仅返回首页顶部指标，适合高频加载。',
    },
    {
      method: 'GET',
      path: '/api/v1/overview/watchlist-pulse',
      request: 'void',
      response: 'OverviewWatchlistPulseResponse',
      note: '仅返回首页今日关注，避免把完整自选页数据带进首页。',
    },
    {
      method: 'GET',
      path: '/api/v1/overview/recent-actions',
      request: 'void',
      response: 'OverviewRecentActionsResponse',
      note: '仅返回最近动作列表，建议限制条数。',
    },
    {
      method: 'GET',
      path: '/api/v1/overview/sip-digests',
      request: 'void',
      response: 'OverviewSipPlanDigestResponse',
      note: '仅返回首页定投摘要，不直接返回完整执行记录。',
    },
    {
      method: 'GET',
      path: '/api/v1/dashboard',
      request: 'void',
      response: 'DashboardResponse',
      note: '旧聚合接口，仅用于兼容现有前端，不建议再继续加字段。',
    },
  ],
  funds: [
    {
      method: 'GET',
      path: '/api/v1/funds?query=keyword',
      request: 'void',
      response: 'FundSearchResponse',
      note: '基金中心列表和搜索候选接口。',
    },
    {
      method: 'GET',
      path: '/api/v1/funds/{fundCode}',
      request: 'void',
      response: 'FundDetail',
      note: '基金详情接口，允许返回走势图和持仓分布等重字段。',
    },
    {
      method: 'GET',
      path: '/api/v1/funds/{fundCode}/holding-insight',
      request: 'void',
      response: 'HoldingInsight',
      note: '基金详情页的持仓洞察建议拆成独立接口，避免每次详情都联查组合数据。',
    },
  ],
  watchlist: [
    {
      method: 'GET',
      path: '/api/v1/watchlist',
      request: 'void',
      response: 'WatchlistListResponse',
      note: '自选列表接口，返回分组和已持仓状态即可。',
    },
    {
      method: 'GET',
      path: '/api/v1/watchlist/groups',
      request: 'void',
      response: 'WatchlistGroupListResponse',
      note: '获取当前用户可用的自选分组选项。',
    },
    {
      method: 'POST',
      path: '/api/v1/watchlist/groups',
      request: 'CreateWatchlistGroupPayload',
      response: 'WatchlistGroupOption',
      note: '新增自选分组，按用户维度校验重名。',
    },
    {
      method: 'POST',
      path: '/api/v1/watchlist',
      request: 'CreateWatchlistPayload',
      response: 'WatchlistItem',
      note: '新增自选，允许一次写入 note 和 group。',
    },
    {
      method: 'PATCH',
      path: '/api/v1/watchlist/groups',
      request: 'UpdateWatchlistGroupsPayload',
      response: 'WatchlistListResponse',
      note: '批量更新自选分组，避免逐条写数据库。',
    },
    {
      method: 'DELETE',
      path: '/api/v1/watchlist/{fundCode}',
      request: 'void',
      response: 'void',
      note: '移出自选。',
    },
  ],
  holdings: [
    {
      method: 'GET',
      path: '/api/v1/holdings/overview',
      request: 'void',
      response: 'HoldingsOverviewResponse',
      note: '持仓页主接口，建议由后端直接返回表格行和组合总市值。',
    },
    {
      method: 'POST',
      path: '/api/v1/holdings',
      request: 'HoldingUpsertPayload',
      response: 'void',
      note: '新增持仓快照，前端需明确传入 T-1 或 T 口径。',
    },
    {
      method: 'PATCH',
      path: '/api/v1/holdings/{fundCode}',
      request: 'HoldingUpsertPayload',
      response: 'void',
      note: '修改持仓快照。',
    },
    {
      method: 'POST',
      path: '/api/v1/holdings/operations',
      request: 'CreateHoldingOperationPayload',
      response: 'HoldingOperation',
      note: '手工买入或卖出，支持近 30 天补记。',
    },
    {
      method: 'GET',
      path: '/api/v1/orders?scope=recent',
      request: 'void',
      response: 'RecentOrderListResponse',
      note: '最近动作建议限制条数，避免全量订单扫描。',
    },
  ],
  sipPlans: [
    {
      method: 'GET',
      path: '/api/v1/sips',
      request: 'void',
      response: 'SipPlan[]',
      note: '定投计划列表接口，列表页使用。',
    },
    {
      method: 'POST',
      path: '/api/v1/sips',
      request: 'CreateSipPlanPayload',
      response: 'SipPlan',
      note: '创建定投计划。',
    },
    {
      method: 'GET',
      path: '/api/v1/sips/{sipPlanId}',
      request: 'void',
      response: 'SipPlan',
      note: '定投计划详情基础信息。',
    },
    {
      method: 'GET',
      path: '/api/v1/sips/{sipPlanId}/records',
      request: 'void',
      response: 'SipExecutionRecord[]',
      note: '执行记录接口，和计划基础信息拆开，便于分页或限量。',
    },
    {
      method: 'PATCH',
      path: '/api/v1/sips/{sipPlanId}',
      request: 'UpdateSipPlanPayload',
      response: 'SipPlan',
      note: '编辑计划，仅开放金额和周期相关字段。',
    },
    {
      method: 'POST',
      path: '/api/v1/sips/{sipPlanId}/pause',
      request: 'void',
      response: 'SipPlan',
      note: '暂停计划动作接口。',
    },
    {
      method: 'POST',
      path: '/api/v1/sips/{sipPlanId}/resume',
      request: 'void',
      response: 'SipPlan',
      note: '恢复计划动作接口。',
    },
    {
      method: 'POST',
      path: '/api/v1/sips/{sipPlanId}/stop',
      request: 'void',
      response: 'SipPlan',
      note: '停止计划动作接口。停止后不应再允许恢复。',
    },
  ],
} as const
