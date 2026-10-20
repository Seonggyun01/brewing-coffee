import { test, expect } from '@playwright/test'
const snapshot = {
  scope: '서비스 전체 · 앱 내 집계 · UTC 기준',
  delayed: false,
  items: [
    {
      site: 'Groq',
      model: 'qwen/qwen3.8-27b',
      current: 2881,
      max: 200000,
      unit: '토큰',
      period: '일간',
      color: '#F55036',
    },
    {
      site: 'OpenRouter',
      model: 'nvidia/nemotron-3-embed-1b:free',
      current: 22,
      max: 50,
      unit: '회',
      period: '일간',
      color: '#7C3AED',
    },
    {
      site: 'Google Vision',
      model: 'Text Detection',
      current: 6,
      max: 1000,
      unit: '건',
      period: '월간',
      color: '#4285F4',
    },
  ],
}
test('public usage panel displays counters, moves, persists and stays inside mobile viewport', async ({
  page,
}) => {
  await page.route('**/api/backend/front/api-usage', (route) => route.fulfill({ json: snapshot }))
  await page.goto('/login')
  await page.getByRole('button', { name: 'API 사용량 펼치기' }).click()
  const panel = page.getByRole('complementary', { name: '서비스 API 사용량' })
  await expect(panel.getByText('2,881 / 200,000 토큰')).toBeVisible()
  await expect(panel.getByRole('meter')).toHaveCount(3)
  await expect(panel.getByRole('meter').nth(1).locator('span')).toHaveCSS(
    'background-color',
    'rgb(124, 58, 237)',
  )
  const header = panel.locator('.api-usage__title')
  const before = (await panel.boundingBox())!
  const bounds = (await header.boundingBox())!
  await page.mouse.move(bounds.x + 30, bounds.y + 8)
  await page.mouse.down()
  await page.mouse.move(bounds.x + 250, bounds.y - 160, { steps: 8 })
  await page.mouse.up()
  const after = (await panel.boundingBox())!
  expect(after.x).toBeGreaterThan(before.x + 100)
  await page.reload()
  await expect(page.getByRole('button', { name: 'API 사용량 펼치기' })).toBeVisible()
  expect((await panel.boundingBox())!.x).toBeCloseTo(after.x, 0)
  await page.getByRole('button', { name: 'API 사용량 펼치기' }).click()
  await page.setViewportSize({ width: 375, height: 667 })
  await expect
    .poll(async () => {
      const b = (await panel.boundingBox())!
      return b.x >= 16 && b.x + b.width <= 360 && b.y + b.height <= 652
    })
    .toBe(true)
  await page.screenshot({ path: '/private/tmp/blank-api-usage-mobile.png' })
})
test('failed usage fetch shows a delay instead of fabricated zero counters', async ({ page }) => {
  await page.route('**/api/backend/front/api-usage', (route) =>
    route.fulfill({ status: 503, body: '{}' }),
  )
  await page.goto('/login')
  await page.getByRole('button', { name: 'API 사용량 펼치기' }).click()
  await expect(page.getByRole('status').filter({ hasText: '갱신 지연' })).toBeVisible()
  await expect(page.getByRole('meter')).toHaveCount(0)
})
