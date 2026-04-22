import { expect, test } from '@playwright/test'
import { getSipPlanByFundCode } from './helpers/db'
import { resetE2EData } from './helpers/reset'
import { login, openFundDetail, openSipDetail } from './helpers/ui'

test.beforeEach(async ({ page }) => {
  resetE2EData()
  await login(page)
})

test('已有定投计划可以查看详情与执行记录', async ({ page }) => {
  const plan = await getSipPlanByFundCode('519674')
  expect(plan).not.toBeNull()

  await openSipDetail(page, String(plan?.id))
  await expect(page.getByTestId('sip-plan-detail-sip-e2e-519674')).toContainText('银河创新成长混合')
  await expect(page.getByTestId('sip-record-list')).toContainText('确认中')
  await expect(page.getByTestId('sip-record-list')).toContainText('已执行')
})

test('无计划基金可以创建定投并在详情页看到新计划', async ({ page }) => {
  await openFundDetail(page, '000001')
  await page.getByTestId('sip-action-button').click()
  await expect(page.getByTestId('sip-input-panel')).toBeVisible()
  await page.getByTestId('sip-input-amount').fill('500')
  await page.getByTestId('sip-input-confirm').click()

  await expect(page.getByTestId('action-message')).toContainText('已为 华夏成长优选混合 创建每日定投计划')
  await expect(page.getByTestId('sip-action-button')).toContainText('已设定投')

  const plan = await getSipPlanByFundCode('000001')
  expect(plan).not.toBeNull()

  await openSipDetail(page, String(plan?.id))
  await expect(page.getByTestId(`sip-plan-detail-${plan?.id}`)).toContainText('华夏成长优选混合')
})

test('定投详情页可以暂停、恢复和停止计划', async ({ page }) => {
  const plan = await getSipPlanByFundCode('519674')
  expect(plan).not.toBeNull()

  await openSipDetail(page, String(plan?.id))
  await page.getByRole('button', { name: '暂停计划' }).click()
  await expect(page.getByTestId(`sip-plan-detail-${plan?.id}`)).toContainText('状态：暂停')

  await page.getByRole('button', { name: '恢复计划' }).click()
  await expect(page.getByTestId(`sip-plan-detail-${plan?.id}`)).toContainText('状态：生效')

  await page.getByRole('button', { name: '停止计划' }).click()
  await expect(page.getByTestId(`sip-plan-detail-${plan?.id}`)).toContainText('状态：停止')
})
