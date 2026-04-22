import mysql from 'mysql2/promise'
import { resolveEnv } from './env'

export const E2E_USER = {
  id: 'user-e2e-001',
  username: 'e2e_user',
  password: 'ChangeMe123!',
  displayName: 'E2E Analyst',
} as const

function createDbConfig() {
  return {
    host: resolveEnv('DB_HOST', '127.0.0.1'),
    port: Number(resolveEnv('DB_PORT', '3306')),
    user: resolveEnv('DB_USERNAME', 'fundcat'),
    password: resolveEnv('DB_PASSWORD', 'change-me-app-password'),
    database: resolveEnv('DB_NAME', 'fundcat'),
  }
}

function normalizeDateValue(value: unknown) {
  if (value instanceof Date) {
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  return value
}

function normalizeRow<T extends Record<string, unknown> | null>(row: T): T {
  if (!row) {
    return row
  }
  return Object.fromEntries(
    Object.entries(row).map(([key, value]) => [key, key.toLowerCase().includes('date') ? normalizeDateValue(value) : value]),
  ) as T
}

export async function withDb<T>(run: (connection: mysql.Connection) => Promise<T>) {
  const connection = await mysql.createConnection(createDbConfig())
  try {
    return await run(connection)
  } finally {
    await connection.end()
  }
}

export async function getHoldingSnapshot(fundCode: string) {
  return withDb(async (connection) => {
    const [rows] = await connection.query(
      `
        select fund_code as fundCode, shares, average_cost as averageCost, market_value as marketValue,
               total_pnl as holdingPnl, total_pnl_rate as holdingPnlRate
        from holding_daily_snapshots
        where user_id = ? and fund_code = ?
        order by trade_date desc
        limit 1
      `,
      [E2E_USER.id, fundCode],
    )
    return normalizeRow((rows as Array<Record<string, number | string | Date>>)[0] ?? null)
  })
}

export async function getLatestOperation(fundCode: string) {
  return withDb(async (connection) => {
    const [rows] = await connection.query(
      `
        select operation, source, status, trade_date as tradeDate, amount, shares_delta as sharesDelta,
               nav, fee_rate as feeRate, fee_amount as feeAmount
        from holding_operation_records
        where user_id = ? and fund_code = ?
        order by trade_date desc, created_at desc
        limit 1
      `,
      [E2E_USER.id, fundCode],
    )
    return normalizeRow((rows as Array<Record<string, number | string | Date>>)[0] ?? null)
  })
}

export async function getDailySnapshot(fundCode: string, tradeDate: string) {
  return withDb(async (connection) => {
    const [rows] = await connection.query(
      `
        select trade_date as tradeDate, shares, average_cost as averageCost, nav, market_value as marketValue,
               daily_pnl as dailyPnl, total_pnl as totalPnl, total_pnl_rate as totalPnlRate
        from holding_daily_snapshots
        where user_id = ? and fund_code = ? and trade_date = ?
      `,
      [E2E_USER.id, fundCode, tradeDate],
    )
    return normalizeRow((rows as Array<Record<string, number | string | Date>>)[0] ?? null)
  })
}

export async function getSipPlanByFundCode(fundCode: string) {
  return withDb(async (connection) => {
    const [rows] = await connection.query(
      `
        select id, fund_code as fundCode, amount, cadence, status, next_run_at as nextRunAt, fee_rate as feeRate
        from sip_plans
        where user_id = ? and fund_code = ?
        order by created_at desc
        limit 1
      `,
      [E2E_USER.id, fundCode],
    )
    return normalizeRow((rows as Array<Record<string, number | string | Date>>)[0] ?? null)
  })
}
