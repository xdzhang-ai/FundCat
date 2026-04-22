import { expect, test } from '@playwright/test'
import { getHoldingSnapshot } from './helpers/db'
import { resetE2EData } from './helpers/reset'
import { login, openFundDetail, openHoldings } from './helpers/ui'

function formatCurrency(value: number) {
  return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

test.beforeEach(async ({ page }) => {
  resetE2EData()
  await login(page)
})

test('无持仓基金可以建仓并在持仓页展示', async ({ page }) => {
  await openFundDetail(page, '000001')
  await page.getByTestId('add-holding-action-button').getByRole('button').click()
  await expect(page.getByTestId('holding-input-panel-add')).toBeVisible()
  await page.getByTestId('holding-input-amount-basis-t-minus-1').click()
  await page.getByTestId('holding-input-amount').fill('5000')
  await page.getByTestId('holding-input-pnl').fill('300')
  await page.getByTestId('holding-input-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已将 华夏成长优选混合 加入持仓')
  await expect(page.getByTestId('holding-status-button')).toContainText('已持仓')
  await expect(page.getByTestId('holding-insight-section')).toBeVisible()

  const holding = await getHoldingSnapshot('000001')
  expect(holding).not.toBeNull()
  await expect(page.getByTestId('holding-metric-持有金额')).toContainText(formatCurrency(Number(holding?.marketValue ?? 0)))

  await openHoldings(page)
  await expect(page.getByTestId('holding-row-000001')).toContainText('华夏成长优选混合')
  await expect(page.getByTestId('holding-row-000001')).toContainText(formatCurrency(Number(holding?.marketValue ?? 0)))
  expect(Number(holding?.shares)).toBeGreaterThan(3000)
})

test('已持仓基金可以修改持仓快照并同步持仓洞察', async ({ page }) => {
  await openFundDetail(page, '005827')
  await page.getByTestId('edit-holding-action-button').click()
  await expect(page.getByTestId('holding-input-panel-edit')).toBeVisible()
  await page.getByTestId('holding-input-amount-basis-t').click()
  await page.getByTestId('holding-input-amount').fill('2600')
  await page.getByTestId('holding-input-pnl').fill('180')
  await page.getByTestId('holding-input-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已更新 中欧价值回报混合 的持仓')

  const holding = await getHoldingSnapshot('005827')
  expect(holding).not.toBeNull()
  await expect(page.getByTestId('holding-metric-持有金额')).toContainText(formatCurrency(Number(holding?.marketValue ?? 0)))
  await expect(page.getByTestId('holding-metric-持有收益')).toContainText(formatCurrency(Number(holding?.holdingPnl ?? 0)))

  await openHoldings(page)
  await expect(page.getByTestId('holding-row-005827')).toContainText(formatCurrency(Number(holding?.marketValue ?? 0)))
  expect(Number(holding?.holdingPnl)).toBeGreaterThan(170)
  expect(Number(holding?.holdingPnl)).toBeLessThan(190)
})
