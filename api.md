# FundCat API 文档

基于当前后端 Swagger/OpenAPI 和实际接口行为整理，供前端联调参考。

在线 Swagger：
- [http://127.0.0.1:8080/swagger-ui.html](http://127.0.0.1:8080/swagger-ui.html)
- [http://127.0.0.1:8080/v3/api-docs](http://127.0.0.1:8080/v3/api-docs)

## 公共约定

### 基础信息
- Base URL：`http://127.0.0.1:8080`
- API 前缀：`/api/v1`
- 鉴权方式：`Authorization: Bearer <accessToken>`
- 数据格式：`Content-Type: application/json`

### 统一响应结构
所有成功响应都会包一层：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

错误响应示例：

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": {
    "errors": {
      "username": "username is invalid"
    }
  }
}
```

### 常用状态码
- `200`：请求成功
- `201`：创建成功
- `204`：删除或登出成功，无响应体
- `400`：参数错误、业务校验失败
- `401`：未登录或令牌无效
- `404`：资源不存在
- `500`：服务端异常

## Auth

### POST `/api/v1/auth/register`
用途：注册并直接返回登录态。

请求示例：

```json
{
  "displayName": "Winter",
  "username": "winter_user",
  "password": "ChangeMe123!"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "fc_at_xxx",
    "refreshToken": "fc_rt_xxx",
    "expiresIn": 14400,
    "profile": {
      "id": "user-demo-001",
      "displayName": "Winter",
      "username": "winter_user",
      "riskMode": "research"
    }
  }
}
```

### POST `/api/v1/auth/login`
用途：用户名密码登录。

请求示例：

```json
{
  "username": "demo_analyst",
  "password": "ChangeMe123!"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "fc_at_4d2b554c-5def-4ef2-afd2-5af7475a52ed",
    "refreshToken": "fc_rt_1b265f43-47b5-42b4-80e4-3309b0ee924e",
    "expiresIn": 14400,
    "profile": {
      "id": "user-demo-001",
      "displayName": "Demo Analyst",
      "username": "demo_analyst",
      "riskMode": "research"
    }
  }
}
```

### POST `/api/v1/auth/refresh`
用途：刷新令牌，换取新的访问令牌。

请求示例：

```json
{
  "refreshToken": "fc_rt_xxx"
}
```

### POST `/api/v1/auth/logout`
用途：退出登录并撤销当前 access token。

请求头示例：

```http
Authorization: Bearer fc_at_xxx
```

响应：`204 No Content`

### GET `/api/v1/auth/me`
用途：获取当前登录用户资料。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "user-demo-001",
    "displayName": "Demo Analyst",
    "username": "demo_analyst",
    "riskMode": "research"
  }
}
```

## Overview / Dashboard

### GET `/api/v1/overview/hero-metrics`
用途：首页 Hero 区域。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "profile": {
      "id": "user-demo-001",
      "displayName": "Demo Analyst",
      "username": "demo_analyst",
      "riskMode": "research"
    },
    "metrics": [
      {
        "label": "组合市值",
        "value": "¥33951.29",
        "delta": "+2377.36",
        "tone": "positive"
      },
      {
        "label": "模拟定投",
        "value": "2",
        "delta": "下个执行窗口已排期",
        "tone": "neutral"
      }
    ]
  }
}
```

### GET `/api/v1/overview/watchlist-pulse`
用途：首页自选卡片列表。

### GET `/api/v1/overview/recent-actions`
用途：首页最近执行动作。

### GET `/api/v1/overview/sip-digests`
用途：首页定投摘要。

### GET `/api/v1/dashboard`
用途：兼容旧页面的聚合接口，返回 profile、heroMetrics、watchlist、portfolios、orders、sipPlans、reports、alerts、importJobs。

## Funds

### GET `/api/v1/funds`
用途：搜索基金；`query` 为空时返回默认优先列表。

查询参数：
- `query`：可选，按基金名称或代码搜索

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "code": "000001",
      "name": "华夏成长优选混合",
      "category": "主动权益",
      "riskLevel": "中高风险",
      "tags": ["AI", "高弹性", "观察池"],
      "benchmark": "沪深300收益率 * 85% + 中债综合 * 15%",
      "unitNav": 1.6234,
      "dayGrowth": 1.28,
      "estimatedNav": 1.6351,
      "estimatedGrowth": 2.01,
      "referenceOnly": true,
      "watchlisted": true,
      "held": true
    }
  ]
}
```

### GET `/api/v1/funds/{code}`
用途：获取基金详情。

路径参数：
- `code`：基金代码，例如 `000001`

响应要点：
- `unitNav / dayGrowth`：确认净值口径
- `estimatedNav / estimatedGrowth`：参考估值口径
- `watchlisted / held`：当前用户状态
- `topHoldings / navHistory / estimateHistory / quarterlyHoldings / industryDistribution`：详情页展示数据

### GET `/api/v1/funds/{code}/user-state`
用途：获取当前用户对这只基金的轻量状态，适合按钮显隐、详情页快捷判断是否已自选或已持仓。

路径参数：
- `code`：基金代码，例如 `000001`

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "code": "000001",
    "watchlisted": true,
    "held": true
  }
}
```

### GET `/api/v1/funds/{code}/holding-insight`
用途：获取当前用户对这只基金的持仓洞察，仅持有时可调用。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "fundCode": "000001",
    "amountHeld": 14587.46,
    "holdingPnl": 1009.01,
    "holdingReturnRate": 7.43,
    "shares": 8984.4209,
    "averageCost": 1.5111,
    "allocation": 42.9658,
    "dayChange": 1.28,
    "todayPnl": 104.38,
    "yesterdayPnl": 88.51,
    "oneYearReturn": 19.36,
    "holdingDays": 122
  }
}
```

## Holdings

### GET `/api/v1/holdings/overview`
用途：获取持仓总览。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalMarketValue": 33951.29,
    "items": [
      {
        "fundCode": "000001",
        "fundName": "华夏成长优选混合",
        "dayGrowth": 1.28,
        "todayPnl": 104.38,
        "marketValue": 14587.46,
        "holdingPnl": 1009.01,
        "allocation": 42.9658
      }
    ]
  }
}
```

### POST `/api/v1/holdings`
用途：新增持仓快照。

请求说明：
- `amountBasis = T_MINUS_1`：按 T-1 净值反推
- `amountBasis = T`：按 T 日净值口径反推

请求示例：

```json
{
  "fundCode": "000001",
  "amountBasis": "T_MINUS_1",
  "amount": 12880.5,
  "holdingPnl": 1180.5
}
```

### PATCH `/api/v1/holdings/{fundCode}`
用途：修改指定基金当前持仓快照。

请求体与新增持仓相同。

### POST `/api/v1/holdings/operations`
用途：补记近 30 天内的手工买入或卖出，并从补记日回补到今天。

请求说明：
- 前端入参仍然只传 `BUY / SELL`
- 后端直接按前端传入的 `tradeDate` 处理，不额外判断 `15:00` 前后
- 会使用该日期可用的最近确认净值立即执行并返回 `已执行`
- 后端会按补记日期当天的真实持仓状态自动归类操作记录：
  - `OPEN_POSITION`：本次买入前没有持仓，属于建仓
  - `BUY`：本次买入前已经有持仓，属于加仓
  - `SELL`：卖出后仍有剩余份额，属于减仓
  - `CLOSE_POSITION`：卖出后份额归零，属于清仓

#### 买入示例

```json
{
  "fundCode": "000001",
  "operation": "BUY",
  "tradeDate": "2026-03-28",
  "amount": 2000,
  "feeRate": 0.0015,
  "note": "补记支付宝买入"
}
```

#### 卖出示例

```json
{
  "fundCode": "000001",
  "operation": "SELL",
  "tradeDate": "2026-03-28",
  "shares": 300.5,
  "feeRate": 0.0015,
  "note": "补记手动卖出"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "op-001",
    "fundCode": "000001",
    "operation": "OPEN_POSITION",
    "source": "MANUAL",
    "status": "已执行",
    "tradeDate": "2026-03-30",
    "amount": 2000,
    "sharesDelta": 1231.5271,
    "nav": 1.6234,
    "feeRate": 0.0015,
    "feeAmount": 3
  }
}
```

## Watchlist / Portfolio

### GET `/api/v1/watchlist`
用途：获取自选列表。

### POST `/api/v1/watchlist`
用途：新增自选。

请求示例：

```json
{
  "fundCode": "000001",
  "note": "重点观察仓位",
  "groups": ["重点观察", "AI"]
}
```

### PATCH `/api/v1/watchlist/groups`
用途：批量替换自选分组。

请求示例：

```json
{
  "fundCodes": ["000001", "519674"],
  "groups": ["核心仓"]
}
```

### DELETE `/api/v1/watchlist/{fundCode}`
用途：删除指定基金自选。

### GET `/api/v1/portfolios`
用途：兼容旧前端的持仓聚合接口。

### GET `/api/v1/orders`
用途：获取最近已执行动作。

查询参数：
- `scope`：当前默认 `recent`

## SIP

### GET `/api/v1/sips`
用途：获取定投计划列表。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "4c55dd9c-cb8b-4278-89ef-87f2aa27ce86",
      "fundCode": "005827",
      "fundName": "中欧价值回报混合",
      "amount": 800,
      "cadence": "WEEKLY",
      "nextRunAt": "2026-04-01T15:54:11",
      "active": true,
      "status": "生效",
      "feeRate": 0.0015
    }
  ]
}
```

### POST `/api/v1/sips`
用途：创建定投计划。

请求示例：

```json
{
  "portfolioId": "portfolio-current",
  "fundCode": "000001",
  "amount": 1000,
  "cadence": "WEEKLY",
  "nextRunAt": "2026-04-06T15:00:00",
  "feeRate": 0.0015
}
```

### GET `/api/v1/sips/{sipPlanId}`
用途：获取定投计划详情。

### GET `/api/v1/sips/{sipPlanId}/records`
用途：获取定投执行记录。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "op-sip-001",
      "sipPlanId": "sip-001",
      "executedOn": "2026-03-30",
      "amount": 1000,
      "status": "确认中",
      "feeRate": 0.0015,
      "feeAmount": 1.5
    }
  ]
}
```

### PATCH `/api/v1/sips/{sipPlanId}`
用途：修改定投计划。

请求示例：

```json
{
  "amount": 1200,
  "cadence": "MONTHLY",
  "weekday": null,
  "monthDay": "15",
  "feeRate": 0.0015
}
```

### POST `/api/v1/sips/{sipPlanId}/pause`
用途：暂停定投。

### POST `/api/v1/sips/{sipPlanId}/resume`
用途：恢复定投。

### POST `/api/v1/sips/{sipPlanId}/stop`
用途：停止定投，停止后不可恢复。

## Import / Reports / Alerts

### GET `/api/v1/import-jobs`
用途：获取 OCR 导入任务。

### POST `/api/v1/import-jobs`
用途：创建 OCR 导入任务占位记录。

请求示例：

```json
{
  "sourcePlatform": "支付宝",
  "fileName": "holdings-2026-03-30.png"
}
```

### GET `/api/v1/reports/weekly`
用途：获取周报列表。

### GET `/api/v1/alerts`
用途：获取提醒规则列表。

## Ops

### GET `/api/v1/ops/summary`
用途：获取功能开关和行情提供方状态。

### GET `/api/v1/ops/feature-flags`
用途：获取功能开关列表。

### PATCH `/api/v1/ops/feature-flags/{code}`
用途：切换功能开关。

请求示例：

```json
{
  "enabled": true
}
```

## 前端特别注意

### 1. 一定要解包 `data`
后端所有成功响应都不是直接返回业务对象，而是：

```json
{
  "code": 0,
  "message": "success",
  "data": { ...真实业务数据... }
}
```

### 2. 定投与买卖记录已经统一
最近动作、手工买卖、定投执行记录底层都来自统一操作记录表，前端应优先依赖这些字段：
- `operation`
- `source`
- `status`
- `tradeDate`
- `amount`
- `shares`
- `nav`
- `feeRate`
- `feeAmount`

其中 `operation` 现在可能是：
- `OPEN_POSITION`：建仓
- `BUY`：加仓
- `SELL`：减仓
- `CLOSE_POSITION`：清仓
- `SIP_BUY`：定投买入

### 3. 持仓快照与补记买卖是两类接口
- `POST/PATCH /holdings`：直接写当前持仓结果
- `POST /holdings/operations`：补记近 30 天内的买卖，并触发回补

### 4. 卖出按份额传值
手工卖出不传金额，只传：
- `shares`
- `tradeDate`
- `feeRate`

### 5. 手工买卖与定投的确认方式不同
- 手工买卖：直接按前端传入日期执行
- 定投：`15:00` 快照后进入 `确认中`
- 定投在晚间拿到确认净值后会变成 `已执行`

### 6. 定投记录状态只有两种
- `确认中`
- `已执行`

### 7. 15:00 定投快照规则
- `15:00` 前后的修改都允许提交
- 是否算入 `T` 日，由后端 `15:00` 快照决定
- `15:00` 后动作归属 `T+1`
