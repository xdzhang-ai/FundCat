import crypto from 'node:crypto'
import mysql from 'mysql2/promise'
import { dbConfig, e2eUser } from './db-config.mjs'

const connection = await mysql.createConnection(dbConfig)
const e2eFundCodes = ['000001', '005827', '004997', '161725', '519674']

const today = new Date()
today.setHours(0, 0, 0, 0)

const sipPlanId = 'sip-e2e-519674'
const portfolioId = 'portfolio-e2e-core'
const missingSnapshotDate = offsetDate(-9)
const createdAtBase = dateTimeOffset(-20, 9, 30)

const fundSeeds = [
  {
    code: '000001',
    name: '华夏成长优选混合',
    unitNav: 1.625,
    accumulatedNav: 3.21,
    estimatedNav: 1.6312,
    estimatedGrowth: 0.38,
    topHoldings: '寒武纪|中芯国际|北方华创|兆易创新',
  },
  {
    code: '005827',
    name: '中欧价值回报混合',
    unitNav: 2.2,
    accumulatedNav: 4.09,
    estimatedNav: 2.2065,
    estimatedGrowth: 0.3,
    topHoldings: '宁德时代|迈瑞医疗|贵州茅台|美的集团',
  },
  {
    code: '004997',
    name: '汇添富互联网核心资产',
    unitNav: 1.2741,
    accumulatedNav: 2.1044,
    estimatedNav: 1.2816,
    estimatedGrowth: 0.59,
    topHoldings: '腾讯控股|美团-W|快手-W|阿里巴巴',
  },
  {
    code: '161725',
    name: '招商中证白酒指数',
    unitNav: 1.02,
    accumulatedNav: 1.31,
    estimatedNav: 1.025,
    estimatedGrowth: 0.49,
    topHoldings: '贵州茅台|山西汾酒|泸州老窖|五粮液',
  },
  {
    code: '519674',
    name: '银河创新成长混合',
    unitNav: 1.452,
    accumulatedNav: 2.9388,
    estimatedNav: 1.4592,
    estimatedGrowth: 0.5,
    topHoldings: '中际旭创|新易盛|沪电股份|胜宏科技',
  },
]

const baselineHoldings = [
  {
    fundCode: '005827',
    fundName: '中欧价值回报混合',
    shares: 1000,
    averageCost: 2.0,
  },
  {
    fundCode: '004997',
    fundName: '汇添富互联网核心资产',
    shares: 392.4339,
    averageCost: 1.2486,
  },
  {
    fundCode: '161725',
    fundName: '招商中证白酒指数',
    shares: 300,
    averageCost: 0.95,
  },
  {
    fundCode: '519674',
    fundName: '银河创新成长混合',
    shares: 260,
    averageCost: 1.35,
  },
]

const watchlists = [
  { id: 'watchlist-e2e-519674', fundCode: '519674', note: '已有定投的观察仓', group: '行业主题' },
  { id: 'watchlist-e2e-005827', fundCode: '005827', note: '稳健底仓', group: '稳健配置' },
]

const operationSeeds = [
  {
    id: 'op-e2e-005827-buy',
    fundCode: '005827',
    operation: 'BUY',
    source: 'MANUAL',
    status: '已执行',
    tradeDate: offsetDate(-12),
    amount: 840,
    sharesDelta: 400,
    nav: 2.1,
    feeRate: 0,
    feeAmount: 0,
    sipPlanId: null,
    note: '基线加仓',
    createdAt: dateTimeOffset(-12, 14, 10),
  },
  {
    id: 'op-e2e-004997-sell',
    fundCode: '004997',
    operation: 'SELL',
    source: 'MANUAL',
    status: '已执行',
    tradeDate: offsetDate(-15),
    amount: 120,
    sharesDelta: -90,
    nav: 1.3333,
    feeRate: 0,
    feeAmount: 0,
    sipPlanId: null,
    note: '基线减仓',
    createdAt: dateTimeOffset(-15, 10, 5),
  },
  {
    id: 'op-e2e-sip-executed',
    fundCode: '519674',
    operation: 'SIP_BUY',
    source: 'SIP',
    status: '已执行',
    tradeDate: offsetDate(-7),
    amount: 500,
    sharesDelta: 348.4321,
    nav: 1.435,
    feeRate: 0.001,
    feeAmount: 0.5,
    sipPlanId,
    note: '上期定投',
    createdAt: dateTimeOffset(-7, 15, 0),
  },
  {
    id: 'op-e2e-sip-pending',
    fundCode: '519674',
    operation: 'SIP_BUY',
    source: 'SIP',
    status: '确认中',
    tradeDate: offsetDate(0),
    amount: 500,
    sharesDelta: 0,
    nav: 0,
    feeRate: 0.001,
    feeAmount: 0,
    sipPlanId,
    note: '本期待确认',
    createdAt: dateTimeOffset(0, 15, 0),
  },
]

function uuid() {
  return crypto.randomUUID()
}

function offsetDate(offsetDays) {
  const value = new Date(today)
  value.setDate(value.getDate() + offsetDays)
  return formatDate(value)
}

function dateTimeOffset(offsetDays, hours, minutes) {
  const value = new Date(today)
  value.setDate(value.getDate() + offsetDays)
  value.setHours(hours, minutes, 0, 0)
  return formatDateTime(value)
}

function formatDate(value) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateTime(value) {
  return `${formatDate(value)} ${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}:${String(value.getSeconds()).padStart(2, '0')}`
}

function round(value, digits) {
  return Number(value.toFixed(digits))
}

function navFor(fundCode, dayOffset) {
  switch (fundCode) {
    case '005827':
      return round(2.2 - dayOffset * 0.008, 4)
    case '004997':
      return round(1.2741 - dayOffset * 0.0041, 4)
    case '161725':
      return round(1.02 - dayOffset * 0.0015, 4)
    case '519674':
      return round(1.452 - dayOffset * 0.0052, 4)
    case '000001':
      return round(1.625 - dayOffset * 0.0065, 4)
    default:
      return 1
  }
}

function buildDailySnapshots() {
  const rows = []
  for (const holding of baselineHoldings) {
    for (let i = 30; i >= 0; i -= 1) {
      const tradeDate = offsetDate(-i)
      if (holding.fundCode === '004997' && tradeDate === missingSnapshotDate) {
        continue
      }

      const nav = navFor(holding.fundCode, i)
      const previousNav = navFor(holding.fundCode, Math.min(i + 1, 30))
      const marketValue = round(holding.shares * nav, 2)
      const totalCost = holding.shares * holding.averageCost
      rows.push({
        id: `${holding.fundCode}-${tradeDate}`,
        userId: e2eUser.id,
        fundCode: holding.fundCode,
        tradeDate,
        shares: holding.shares,
        averageCost: holding.averageCost,
        nav,
        marketValue,
        dailyPnl: round(holding.shares * (nav - previousNav), 2),
        totalPnl: round(marketValue - totalCost, 2),
        totalPnlRate: totalCost === 0 ? 0 : round(((marketValue - totalCost) / totalCost) * 100, 4),
        updatedAt: `${tradeDate} 23:30:00`,
      })
    }
  }
  return rows
}

function currentHoldingSnapshot(holding) {
  const nav = navFor(holding.fundCode, 0)
  const marketValue = round(holding.shares * nav, 2)
  const totalCost = holding.shares * holding.averageCost
  return {
    id: `holding-${holding.fundCode}`,
    userId: e2eUser.id,
    fundCode: holding.fundCode,
    fundName: holding.fundName,
    shares: holding.shares,
    averageCost: holding.averageCost,
    marketValue,
    holdingPnl: round(marketValue - totalCost, 2),
    holdingPnlRate: totalCost === 0 ? 0 : round(((marketValue - totalCost) / totalCost) * 100, 4),
    updatedAt: dateTimeOffset(0, 14, 55),
  }
}

async function insertFundSeed(seed) {
  await connection.query(
    `
      insert into funds (code, name, tags, top_holdings, created_at)
      values (?, ?, ?, ?, ?)
      on duplicate key update
        name = values(name),
        tags = values(tags),
        top_holdings = values(top_holdings)
    `,
    [seed.code, seed.name, `${seed.name}|E2E`, seed.topHoldings, createdAtBase],
  )

  await connection.query(
    `
      insert into fund_nav_growth (id, fund_code, nav_date, week_growth, month_growth, year_growth, updated_at)
      values (?, ?, ?, 2.34, 5.67, 12.34, ?)
      on duplicate key update
        updated_at = values(updated_at)
    `,
    [`growth-${seed.code}`, seed.code, offsetDate(0), dateTimeOffset(0, 14, 50)],
  )

  await connection.query(
    `
      insert into fund_intraday_estimates (id, fund_code, estimated_at, estimated_nav, estimated_growth, reference_only, sentiment)
      values (?, ?, ?, ?, ?, true, 'e2e baseline')
      on duplicate key update
        estimated_at = values(estimated_at),
        estimated_nav = values(estimated_nav),
        estimated_growth = values(estimated_growth)
    `,
    [`estimate-${seed.code}`, seed.code, dateTimeOffset(0, 14, 56), seed.estimatedNav, seed.estimatedGrowth],
  )

  for (let i = 30; i >= 0; i -= 1) {
    const tradeDate = offsetDate(-i)
    const unitNav = navFor(seed.code, i)
    await connection.query(
      `
        insert into fund_nav_history (id, fund_code, nav_date, unit_nav, accumulated_nav, day_growth)
        values (?, ?, ?, ?, ?, ?)
        on duplicate key update
          unit_nav = values(unit_nav),
          accumulated_nav = values(accumulated_nav),
          day_growth = values(day_growth)
      `,
      [`nav-${seed.code}-${tradeDate}`, seed.code, tradeDate, unitNav, round(seed.accumulatedNav + (30 - i) * 0.01, 4), i === 0 ? seed.estimatedGrowth : 0],
    )
  }
}

try {
  await connection.beginTransaction()

  await connection.query('delete from holding_daily_snapshots where user_id = ?', [e2eUser.id])
  await connection.query('delete from holding_operation_records where user_id = ?', [e2eUser.id])
  await connection.query('delete from watchlist_items where user_id = ?', [e2eUser.id])
  await connection.query('delete from watchlist_groups where user_id = ?', [e2eUser.id])
  await connection.query('delete from sip_plans where user_id = ?', [e2eUser.id])
  await connection.query('delete from auth_users where id = ?', [e2eUser.id])
  await connection.query(`delete from fund_nav_history where fund_code in (${e2eFundCodes.map(() => '?').join(', ')})`, e2eFundCodes)
  await connection.query(`delete from fund_nav_growth where fund_code in (${e2eFundCodes.map(() => '?').join(', ')})`, e2eFundCodes)
  await connection.query(`delete from fund_intraday_estimates where fund_code in (${e2eFundCodes.map(() => '?').join(', ')})`, e2eFundCodes)

  await connection.query(
    `
      insert into auth_users (id, display_name, username, password_hash, risk_mode, created_at, updated_at)
      values (?, ?, ?, ?, 'research', ?, ?)
    `,
    [e2eUser.id, e2eUser.displayName, e2eUser.username, e2eUser.passwordHash, createdAtBase, dateTimeOffset(0, 14, 40)],
  )

  for (const fundSeed of fundSeeds) {
    await insertFundSeed(fundSeed)
  }

  for (const group of ['全部', ...new Set(watchlists.map((item) => item.group))]) {
    await connection.query(
      `
        insert into watchlist_groups (id, user_id, group_name, created_at)
        values (?, ?, ?, ?)
        on duplicate key update
          group_name = values(group_name)
      `,
      [`${e2eUser.id}:${group}`, e2eUser.id, group, dateTimeOffset(-6, 9, 40)],
    )
  }

  for (const watchlist of watchlists) {
    await connection.query(
      `
        insert into watchlist_items (id, user_id, fund_code, note, group_id, created_at)
        values (?, ?, ?, ?, ?, ?)
      `,
      [watchlist.id, e2eUser.id, watchlist.fundCode, watchlist.note, `${e2eUser.id}:${watchlist.group}`, dateTimeOffset(-6, 9, 45)],
    )
  }

  for (const snapshot of buildDailySnapshots()) {
    await connection.query(
      `
        insert into holding_daily_snapshots (id, user_id, fund_code, trade_date, shares, average_cost, nav, market_value, daily_pnl, total_pnl, total_pnl_rate, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `,
      [snapshot.id, snapshot.userId, snapshot.fundCode, snapshot.tradeDate, snapshot.shares, snapshot.averageCost, snapshot.nav, snapshot.marketValue, snapshot.dailyPnl, snapshot.totalPnl, snapshot.totalPnlRate, snapshot.updatedAt],
    )
  }

  await connection.query(
    `
      insert into sip_plans (id, portfolio_id, user_id, fund_code, fund_name, amount, cadence, next_run_at, active, status, fee_rate, created_at, updated_at)
      values (?, ?, ?, '519674', '银河创新成长混合', 500, 'WEEKLY', ?, true, '生效', 0.001, ?, ?)
    `,
    [sipPlanId, portfolioId, e2eUser.id, dateTimeOffset(2, 9, 30), dateTimeOffset(-20, 9, 30), dateTimeOffset(0, 14, 45)],
  )

  for (const operation of operationSeeds) {
    await connection.query(
      `
        insert into holding_operation_records (
          id, user_id, fund_code, operation, source, status, trade_date, amount, shares_delta, nav, fee_rate, fee_amount, sip_plan_id, note, created_at, updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `,
      [
        operation.id,
        e2eUser.id,
        operation.fundCode,
        operation.operation,
        operation.source,
        operation.status,
        operation.tradeDate,
        operation.amount,
        operation.sharesDelta,
        operation.nav,
        operation.feeRate,
        operation.feeAmount,
        operation.sipPlanId,
        operation.note,
        operation.createdAt,
        operation.createdAt,
      ],
    )
  }

  await connection.commit()
  console.log(`Seeded E2E database for ${e2eUser.username}; missing snapshot date for 004997 is ${missingSnapshotDate}`)
} catch (error) {
  await connection.rollback()
  throw error
} finally {
  await connection.end()
}
