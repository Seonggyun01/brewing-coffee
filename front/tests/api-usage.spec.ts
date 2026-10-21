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

test('usage percentages use green through 50, orange below 80 and red from 80', async ({
  page,
}) => {
  const values = structuredClone(snapshot)
  values.items[0]!.current = 100000
  values.items[1]!.current = 39
  values.items[2]!.current = 800
  await page.route('**/api/backend/front/api-usage', (route) => route.fulfill({ json: values }))
  await page.goto('/login')
  const warning = page.getByRole('alertdialog', { name: 'AI API 사용량 경고' })
  await expect(warning).toBeVisible()
  await expect(warning.getByText('Google Vision · 80%')).toBeVisible()
  await warning.getByRole('button', { name: '확인', exact: true }).click()
  await page.getByRole('button', { name: 'API 사용량 펼치기' }).click()
  const rows = page.locator('.api-usage__row')
  for (const [index, percent, color] of [
    [0, '50%', 'rgb(21, 128, 61)'],
    [1, '78%', 'rgb(180, 83, 9)'],
    [2, '80%', 'rgb(180, 35, 24)'],
  ] as const) {
    await expect(rows.nth(index).locator('.api-usage__percentage')).toHaveText(percent)
    await expect(rows.nth(index).locator('.api-usage__percentage')).toHaveCSS('color', color)
    await expect(rows.nth(index).locator('.api-usage__numbers > span')).toHaveCSS('color', color)
  }
  await page.setViewportSize({ width: 375, height: 667 })
  await page.screenshot({ path: '/private/tmp/blank-api-usage-colors.png' })
})

test('80 percent warning appears on polling, stays dismissed after reload, and resets next UTC day', async ({
  page,
}) => {
  await page.clock.install()
  let current = 39
  let updatedAt = '2026-10-21T23:59:00Z'
  await page.route('**/api/backend/front/api-usage', (route) =>
    route.fulfill({
      json: { ...snapshot, updatedAt, items: [{ ...snapshot.items[1], current }] },
    }),
  )
  await page.goto('/login')
  const warning = page.getByRole('alertdialog', { name: 'AI API 사용량 경고' })
  await expect(warning).not.toBeVisible()
  current = 40
  await page.clock.fastForward(30000)
  await expect(warning).toBeVisible()
  await expect(warning.getByText('OpenRouter · 80%')).toBeVisible()
  await page.screenshot({ path: '/private/tmp/blank-api-usage-warning.png' })
  await warning.getByRole('button', { name: '확인', exact: true }).click()
  await page.clock.fastForward(30000)
  await expect(warning).not.toBeVisible()
  await page.reload()
  await page.getByRole('button', { name: 'API 사용량 펼치기' }).click()
  await expect(page.locator('.api-usage__percentage')).toHaveText('80%')
  await expect(warning).not.toBeVisible()
  updatedAt = '2026-10-22T00:00:01Z'
  await page.clock.fastForward(30000)
  await expect(warning).toBeVisible()
})

test('disabled quotas and delayed counters do not trigger misleading warnings', async ({
  page,
}) => {
  await page.route('**/api/backend/front/api-usage', (route) =>
    route.fulfill({
      json: {
        ...snapshot,
        delayed: true,
        items: [
          { ...snapshot.items[0], current: 170000 },
          { ...snapshot.items[1], max: 0 },
        ],
      },
    }),
  )
  await page.goto('/login')
  await page.getByRole('button', { name: 'API 사용량 펼치기' }).click()
  await expect(page.getByRole('status').filter({ hasText: '갱신 지연' })).toBeVisible()
  await expect(page.getByRole('alertdialog')).not.toBeVisible()
  await expect(page.getByText('사용 중지', { exact: true })).toBeVisible()
  await expect(page.getByText('85%', { exact: true })).toBeVisible()
})
