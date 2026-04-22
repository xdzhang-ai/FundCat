import mysql from 'mysql2/promise'
import { dbConfig, e2eUser } from './db-config.mjs'

const connection = await mysql.createConnection(dbConfig)
const e2eFundCodes = ['000001', '005827', '004997', '161725', '519674']

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

  await connection.commit()
} catch (error) {
  await connection.rollback()
  throw error
} finally {
  await connection.end()
}
