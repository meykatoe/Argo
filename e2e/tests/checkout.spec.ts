import { expect, test } from '@playwright/test'
import { cardStock, cleanup, closeDb, orderStatus, seedCard, type Seed } from '../support/db'

let seed: Seed

test.beforeAll(async ({ request }) => {
  const ping = await request.get('/api/ping')
  expect(ping.ok(), '後端沒有啟動').toBeTruthy()
  seed = await seedCard()
})

test.afterAll(async () => {
  if (seed) await cleanup(seed)
  await closeDb()
})

test('瀏覽、加入購物車、結帳、模擬付款後顯示已付款', async ({ page }) => {
  // 瀏覽：用系列篩出自己的卡
  await page.goto(`/cards?setId=${seed.setId}`)
  await page.getByRole('link', { name: seed.cardName }).first().click()
  await expect(page.getByRole('heading', { name: seed.cardName })).toBeVisible()

  // 加入購物車
  await page.getByRole('button', { name: '加入購物車' }).click()
  await page.getByRole('link', { name: '購物車' }).click()
  await expect(page.getByRole('link', { name: seed.cardName })).toBeVisible()
  await page.getByRole('link', { name: '前往結帳' }).click()

  // 結帳：訪客身分填資料
  await page.locator('#c-name').fill('E2E 測試')
  await page.locator('#c-email').fill(seed.email)
  await page.locator('#c-phone').fill('0912345678')
  await page.locator('#c-postal').fill('100')
  await page.locator('#c-city').selectOption('台北市')
  await page.locator('#c-address').fill('中正區測試路 1 號')
  await page.getByRole('button', { name: '送出訂單' }).click()

  // 訂單成立後進入付款
  await expect(page).toHaveURL(/\/orders\/[A-Z0-9-]+/)
  await page.getByRole('button', { name: /前往付款/ }).click()
  await expect(page).toHaveURL(/\/pay\/fake/)
  await page.getByRole('button', { name: '模擬付款成功' }).click()

  // 結果頁顯示已付款
  await expect(page).toHaveURL(/\/pay\/result/)
  await expect(page.getByText('付款成功，謝謝你的購買')).toBeVisible({ timeout: 15_000 })

  // 資料庫狀態與庫存
  expect(await orderStatus(seed.email)).toBe('PAID')
  expect(await cardStock(seed.cardId)).toBe(2)
})
