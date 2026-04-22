import { expect, type Page } from '@playwright/test'
import { E2E_USER } from './db'

export async function login(page: Page) {
  await page.goto('/')
  if (await page.getByTestId('auth-form').isVisible()) {
    await page.getByTestId('auth-username').fill(E2E_USER.username)
    await page.getByTestId('auth-password').fill(E2E_USER.password)
    await page.getByTestId('auth-submit').click()
    await expect(page.getByTestId('auth-form')).toBeHidden()
  }
  await expect(page.getByRole('link', { name: '基金中心' })).toBeVisible()
}

export async function openFundDetail(page: Page, fundCode: string) {
  await page.goto('/funds')
  await expect(page.getByTestId('fund-search-input')).toBeVisible()
  await page.getByTestId('fund-search-input').fill(fundCode)
  await expect(page.getByTestId(`fund-search-select-${fundCode}`)).toBeVisible()
  await page.getByTestId(`fund-search-select-${fundCode}`).click()
  await expect(page.getByTestId(`fund-detail-page-${fundCode}`)).toBeVisible()
}

export async function openHoldings(page: Page) {
  await page.goto('/holdings')
  await expect(page.getByTestId('holdings-table')).toBeVisible()
}

export async function openSipDetail(page: Page, planId: string) {
  await page.goto(`/automation/${planId}`)
  await expect(page.getByTestId(`sip-plan-detail-${planId}`)).toBeVisible()
}
