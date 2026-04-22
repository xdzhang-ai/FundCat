import { expect, test } from '@playwright/test'
import { getDailySnapshot, getHoldingSnapshot, getLatestOperation } from './helpers/db'
import { resetE2EData } from './helpers/reset'
import { login, openFundDetail, openHoldings } from './helpers/ui'

function currentLocalDate(offsetDays = 0) {
  const value = new Date()
  value.setHours(0, 0, 0, 0)
  value.setDate(value.getDate() + offsetDays)
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function nextBusinessDate(baseDate: string) {
  const value = new Date(`${baseDate}T00:00:00`)
  do {
    value.setDate(value.getDate() + 1)
  } while (value.getDay() === 0 || value.getDay() === 6)
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

test.beforeEach(async ({ page }) => {
  resetE2EData()
  await login(page)
})

test('当天买入会先进入确认中，页面持仓结果保持不变', async ({ page }) => {
  const beforeHolding = await getHoldingSnapshot('005827')

  await openFundDetail(page, '005827')
  await page.getByTestId('buy-action-button').getByRole('button').click()
  await page.getByTestId('holding-operation-trade-date').fill(currentLocalDate())
  await page.getByTestId('holding-operation-amount').fill('880')
  await page.getByTestId('holding-operation-fee-rate').fill('0.5')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已提交 中欧价值回报混合 的待确认买入记录')
  await expect(page.getByTestId('fund-operation-history')).toContainText('确认中')

  const operation = await getLatestOperation('005827')
  expect(operation).not.toBeNull()
  expect(operation?.status).toBe('确认中')
  expect(operation?.operation).toBe('BUY')
  expect(operation?.tradeDate).toBe(currentLocalDate())

  const afterHolding = await getHoldingSnapshot('005827')
  expect(afterHolding).toEqual(beforeHolding)
})

test('当天 15:00 后买入会换算到下一交易日并进入确认中', async ({ page }) => {
  const today = currentLocalDate()
  const expectedTradeDate = nextBusinessDate(today)
  const beforeHolding = await getHoldingSnapshot('005827')

  await openFundDetail(page, '005827')
  await page.getByTestId('buy-action-button').getByRole('button').click()
  await page.getByTestId('holding-operation-trade-date').fill(today)
  await page.getByTestId('holding-operation-timing-after-3pm').click()
  await page.getByTestId('holding-operation-amount').fill('660')
  await page.getByTestId('holding-operation-fee-rate').fill('0')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText(`将按 ${expectedTradeDate} 净值确认`)

  const operation = await getLatestOperation('005827')
  expect(operation?.status).toBe('确认中')
  expect(operation?.tradeDate).toBe(expectedTradeDate)

  const afterHolding = await getHoldingSnapshot('005827')
  expect(afterHolding).toEqual(beforeHolding)
})

test('当天卖出会先进入确认中，清仓状态不会提前生效', async ({ page }) => {
  const beforeHolding = await getHoldingSnapshot('161725')

  await openFundDetail(page, '161725')
  await page.getByTestId('sell-action-button').click()
  await page.getByTestId('holding-operation-trade-date').fill(currentLocalDate())
  await page.getByTestId('holding-operation-shares').fill('300')
  await page.getByTestId('holding-operation-fee-rate').fill('0')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已提交 招商中证白酒指数 的待确认卖出记录')
  await expect(page.getByTestId('fund-operation-history')).toContainText('确认中')
  await expect(page.getByTestId('sell-action-button')).toBeVisible()

  const operation = await getLatestOperation('161725')
  expect(operation?.status).toBe('确认中')
  expect(operation?.operation).toBe('SELL')

  const afterHolding = await getHoldingSnapshot('161725')
  expect(afterHolding).toEqual(beforeHolding)
})

test('已持仓基金补记买入后，页面和数据库都按买入结果更新', async ({ page }) => {
  await openFundDetail(page, '005827')
  await page.getByTestId('buy-action-button').getByRole('button').click()
  await page.getByTestId('holding-operation-trade-date').fill(currentLocalDate(-1))
  await page.getByTestId('holding-operation-amount').fill('880')
  await page.getByTestId('holding-operation-fee-rate').fill('0.5')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已补记 中欧价值回报混合 的买入记录')
  await expect(page.getByTestId('fund-operation-history')).toContainText('买入')

  await page.goto('/')
  await expect(page.locator('[data-testid^="overview-recent-action-"]')).not.toHaveCount(0)
  await expect(page.locator('[data-testid^="overview-recent-action-"]').first()).toContainText('中欧价值回报混合')

  const operation = await getLatestOperation('005827')
  expect(operation).not.toBeNull()
  expect(operation?.operation).toBe('BUY')
  expect(Number(operation?.feeAmount)).toBeCloseTo(4.4, 2)

  const holding = await getHoldingSnapshot('005827')
  expect(Number(holding?.shares)).toBeGreaterThan(1000)
})

test('已持仓基金补记卖出后会减仓，且最近动作显示卖出', async ({ page }) => {
  await openFundDetail(page, '005827')
  await page.getByTestId('sell-action-button').click()
  await page.getByTestId('holding-operation-trade-date').fill(currentLocalDate(-1))
  await page.getByTestId('holding-operation-shares').fill('100')
  await page.getByTestId('holding-operation-fee-rate').fill('1')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已补记 中欧价值回报混合 的卖出记录')
  await expect(page.getByTestId('fund-operation-history')).toContainText('卖出')

  const operation = await getLatestOperation('005827')
  expect(operation?.operation).toBe('SELL')
  expect(Number(operation?.sharesDelta)).toBeCloseTo(-100, 4)
  expect(Number(operation?.feeAmount)).toBeGreaterThan(0)

  const holding = await getHoldingSnapshot('005827')
  expect(Number(holding?.shares)).toBeLessThan(1000)
})

test('卖出全部份额后会清仓并回到加持仓状态', async ({ page }) => {
  await openFundDetail(page, '161725')
  await page.getByTestId('sell-action-button').click()
  await page.getByTestId('holding-operation-trade-date').fill(currentLocalDate(-1))
  await page.getByTestId('holding-operation-shares').fill('300')
  await page.getByTestId('holding-operation-fee-rate').fill('0')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已补记 招商中证白酒指数 的卖出记录')
  await expect(page.getByTestId('add-holding-action-button')).toBeVisible()
  await expect(page.getByTestId('fund-operation-history')).toContainText('清仓')

  const operation = await getLatestOperation('161725')
  expect(operation?.operation).toBe('CLOSE_POSITION')

  const holding = await getHoldingSnapshot('161725')
  expect(holding).toBeNull()
})

test('历史补买会回补当天快照并刷新今天持仓结果', async ({ page }) => {
  const tradeDate = currentLocalDate(-5)
  await openFundDetail(page, '005827')
  await page.getByTestId('buy-action-button').getByRole('button').click()
  await page.getByTestId('holding-operation-trade-date').fill(tradeDate)
  await page.getByTestId('holding-operation-amount').fill('500')
  await page.getByTestId('holding-operation-fee-rate').fill('0')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已补记 中欧价值回报混合 的买入记录')

  const operation = await getLatestOperation('005827')
  expect(operation?.tradeDate).toBe(tradeDate)
  expect(operation?.operation).toBe('BUY')

  const snapshot = await getDailySnapshot('005827', tradeDate)
  expect(snapshot).not.toBeNull()
  expect(Number(snapshot?.shares)).toBeGreaterThan(1000)

  const holding = await getHoldingSnapshot('005827')
  expect(Number(holding?.shares)).toBeGreaterThan(1000)
})

test('历史补卖在缺当天快照时仍能成功回补并补齐该日快照', async ({ page }) => {
  const tradeDate = currentLocalDate(-9)
  await openFundDetail(page, '004997')
  await page.getByTestId('sell-action-button').click()
  await page.getByTestId('holding-operation-trade-date').fill(tradeDate)
  await page.getByTestId('holding-operation-shares').fill('100')
  await page.getByTestId('holding-operation-fee-rate').fill('0')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已补记 汇添富互联网核心资产 的卖出记录')

  const operation = await getLatestOperation('004997')
  expect(operation?.tradeDate).toBe(tradeDate)
  expect(operation?.operation).toBe('SELL')

  const snapshot = await getDailySnapshot('004997', tradeDate)
  expect(snapshot).not.toBeNull()

  const holding = await getHoldingSnapshot('004997')
  expect(Number(holding?.shares)).toBeLessThan(392.4339)
})

test('卖出份额超出当前可卖份额时会给出错误提示', async ({ page }) => {
  await openFundDetail(page, '005827')
  await page.getByTestId('sell-action-button').click()
  await page.getByTestId('holding-operation-trade-date').fill(currentLocalDate())
  await page.getByTestId('holding-operation-shares').fill('9999')
  await page.getByTestId('holding-operation-fee-rate').fill('0')
  await page.getByTestId('holding-operation-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('卖出份额不能超过当前持有份额')
})

test('买入和卖出的非法输入会在前端直接拦截', async ({ page }) => {
  await openFundDetail(page, '005827')
  await page.getByTestId('buy-action-button').getByRole('button').click()
  await page.getByTestId('holding-operation-trade-date').fill(currentLocalDate())
  await page.getByTestId('holding-operation-amount').fill('0')
  await page.getByTestId('holding-operation-confirm').click()
  await expect(page.getByTestId('action-message')).toContainText('买入金额必须大于 0')

  await page.getByTestId('holding-operation-cancel').click()
  await page.getByTestId('sell-action-button').click()
  await page.getByTestId('holding-operation-trade-date').fill(currentLocalDate())
  await page.getByTestId('holding-operation-shares').fill('0')
  await page.getByTestId('holding-operation-confirm').click()
  await expect(page.getByTestId('action-message')).toContainText('卖出份额必须大于 0')
})
