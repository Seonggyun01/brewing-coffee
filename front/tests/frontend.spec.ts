import { test, expect } from '@playwright/test'

const unique = () => `테스트 원두 ${Date.now()}`
test('home is server rendered even with JavaScript disabled', async ({ browser, request }) => {
  const response = await request.get('/')
  expect(response.status()).toBe(200)
  expect(await response.text()).toContain('경험한 산지')
  const context = await browser.newContext({ javaScriptEnabled: false })
  const page = await context.newPage()
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '경험한 산지' })).toBeVisible()
  await expect(page.locator('.world-map path')).toHaveCount(250)
  await context.close()
})
test('responsive pages have no horizontal overflow at 375, 768 and 1440px', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  for (const width of [375, 768, 1440]) {
    await page.setViewportSize({ width, height: 900 })
    for (const path of [
      '/',
      '/login',
      '/coffee-beans',
      '/coffee-beans/new',
      '/brew-records/new',
      '/cafe-filter-coffees/new',
      '/cafes/map',
    ]) {
      await page.goto(path)
      await expect(page.locator('h1')).toBeVisible()
      expect(
        await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        `${path} at ${width}px`,
      ).toBe(true)
    }
  }
  expect(errors).toEqual([])
})
test('bean wizard preserves input, validates required name and creates, edits and deletes', async ({
  page,
}) => {
  const name = unique()
  await page.goto('/coffee-beans/new')
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await expect(page.getByText('원두 이름을 입력해주세요.')).toBeVisible()
  await page.getByLabel('원두 이름').fill(name)
  await page.getByLabel('농장 · 워싱스테이션').fill('ZARZA')
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await page.getByLabel('남은 중량').fill('200')
  await page.getByRole('button', { name: '← 이전 단계' }).click()
  await expect(page.getByLabel('원두 이름')).toHaveValue(name)
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await page.getByRole('button', { name: '확인한 정보 저장' }).click()
  await expect(page).toHaveURL(/\/coffee-beans\/\d+$/)
  await expect(page.getByRole('heading', { name })).toBeVisible()
  await expect(page.getByText('ZARZA', { exact: true })).toBeVisible()
  await expect(page.getByText('로스팅일', { exact: true })).toHaveCount(0)
  await page.getByRole('link', { name: '수정하기' }).click()
  await page.getByLabel('원두 이름').fill(name + ' 수정')
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await expect(page.getByLabel('남은 중량')).toHaveValue('200')
  await page.getByRole('button', { name: '확인한 정보 저장' }).click()
  await expect(page.getByRole('heading', { name: name + ' 수정' })).toBeVisible()
  await page.getByRole('button', { name: '삭제', exact: true }).click()
  await page.getByRole('button', { name: '삭제하기' }).click()
  await expect(page).toHaveURL('/coffee-beans')
})
test('brew uses three steps, defaults today, selects ICE, saves taste and deducts stock', async ({
  page,
  request,
}) => {
  const created = await request.post('/api/backend/front/beans', {
    data: { name: unique(), weight: 200 },
  })
  const { id } = await created.json()
  await page.goto(`/brew-records/new?beanId=${id}`)
  const date = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' }).format(new Date())
  await expect(page.getByLabel('기록 날짜')).toHaveValue(date)
  await page.getByRole('button', { name: 'ICE', exact: true }).click()
  await expect(page.getByRole('button', { name: '✓ ICE', exact: true })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await page.getByLabel('원두량').fill('20')
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await page
    .getByRole('group', { name: '산미', exact: true })
    .getByRole('button', { name: '4', exact: true })
    .click()
  await page.getByRole('spinbutton', { name: '만족도 (1–5) 선택' }).fill('4.5')
  await page.getByRole('button', { name: '확인한 정보 저장' }).click()
  await expect(page).toHaveURL(/\/brew-records\/\d+$/)
  const bean = await (await request.get(`/api/backend/front/beans/${id}`)).json()
  expect(bean.weight).toBe(180)
  await expect(page.getByRole('dialog').locator('.taste-profile svg')).toBeVisible()
})
test('cafe wizard has no recipe fields and saves optional taste', async ({ page }) => {
  await page.goto('/cafe-filter-coffees/new')
  await page.getByLabel('원두 이름').fill(unique())
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await expect(page.getByLabel('카페 검색')).toBeVisible()
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await expect(page.locator('.taste-profile')).toBeVisible()
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await expect(page.getByLabel('원두량')).toHaveCount(0)
  await page.getByRole('button', { name: '확인한 정보 저장' }).click()
  await expect(page).toHaveURL(/\/cafe-filter-coffees\/\d+$/)
})
test('flavor wheel opens children; demo is noninteractive', async ({ page }) => {
  await page.goto('/coffee-beans/new')
  await page.getByRole('button', { name: '과일 하위 향미 펼치기' }).click()
  await page.getByRole('button', { name: '시트러스 하위 향미 펼치기' }).click()
  await page.getByRole('button', { name: '레몬', exact: true }).click()
  await expect(page.getByRole('button', { name: '레몬', exact: true })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
  await page.goto('/')
  const demo = page.locator('.native-demo')
  await expect(demo).toBeVisible()
  await expect(page.locator('#home-demo video')).toHaveCount(0)
  expect(await demo.evaluate((el) => getComputedStyle(el).pointerEvents)).toBe('none')
  await expect(page.getByRole('link', { name: 'GitHub · Seonggyun01 ↗' })).toHaveAttribute(
    'href',
    'https://github.com/Seonggyun01',
  )
})
test('AI candidates include details, use text flavors and defer weight and price to step two', async ({
  page,
}) => {
  await page.route('**/api/backend/front/extraction', (route) =>
    route.fulfill({
      json: {
        form: {
          name: 'AI 후보 원두',
          farm: 'ZARZA',
          variety: 'PACAMARA',
          altitude: '1900m',
          weight: 200,
          price: 18000,
          flavorNotes: ['CHERRY'],
          customFlavorNotesText: '밀키스',
          purchasedDate: '2026-01-01',
        },
        warnings: [],
      },
    }),
  )
  await page.route('**/api/backend/coffee-bean-card-extractions/**/events', (route) =>
    route.fulfill({
      contentType: 'text/event-stream',
      body: 'event: progress\ndata: {"phase":"LLM"}\n\n',
    }),
  )
  await page.goto('/coffee-beans/new')
  await page.locator('input[type=file]').setInputFiles('public/img/bean-card.png')
  await expect(page.getByRole('heading', { name: 'AI가 읽은 정보를 확인해주세요' })).toBeVisible()
  await expect(page.getByLabel('농장 · 워싱스테이션')).toHaveValue('ZARZA')
  await expect(page.getByLabel('사진에서 읽은 향미')).toHaveValue('체리, 밀키스')
  await expect(page.locator('.flavor-wheel')).toHaveCount(0)
  await expect(page.getByLabel('구매 가격')).not.toBeVisible()
  await page.getByLabel('사진에서 읽은 향미').fill('체리 주스, 밀키스')
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await expect(page.getByLabel('구매 가격')).toHaveValue('18000')
  await expect(page.getByLabel('남은 중량')).toHaveValue('200')
  await expect(page.getByLabel('구매 날짜')).toHaveValue('')
})

test('API proxy rejects routes outside its explicit allowlist', async ({ request }) => {
  expect((await request.get('/api/backend/front/unknown')).status()).toBe(404)
  expect((await request.get('/api/backend/https://example.com')).status()).toBe(404)
})

test('home advances one scene per wheel gesture and restores ordinary scroll on other pages', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 954 })
  await page.goto('/')
  await page.mouse.move(900, 450)
  await page.mouse.wheel(0, 120)
  await expect
    .poll(() =>
      page.locator('#home-demo').evaluate((el) => Math.round(el.getBoundingClientRect().top)),
    )
    .toBe(0)
  await page.waitForTimeout(950)
  await page.mouse.wheel(0, 120)
  await expect
    .poll(() =>
      page.locator('#home-final').evaluate((el) => Math.round(el.getBoundingClientRect().top)),
    )
    .toBe(0)
  await expect(page.getByRole('link', { name: 'GitHub · Seonggyun01 ↗' })).toBeInViewport()
  await page.waitForTimeout(950)
  await page.mouse.wheel(0, -120)
  await expect
    .poll(() =>
      page.locator('#home-demo').evaluate((el) => Math.round(el.getBoundingClientRect().top)),
    )
    .toBe(0)
  await page.goto('/coffee-beans/new')
  await expect(page.locator('html')).not.toHaveClass(/home-snap/)
  const before = await page.evaluate(() => scrollY)
  await page.mouse.wheel(0, 170)
  await expect.poll(() => page.evaluate(() => scrollY)).toBeGreaterThan(before)
})
test('home keyboard paging respects reduced motion and mobile swipe moves to the next scene', async ({
  browser,
}) => {
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    hasTouch: true,
    reducedMotion: 'reduce',
  })
  const page = await context.newPage()
  await page.goto('http://127.0.0.1:3000/')
  await page.keyboard.press('PageDown')
  await expect
    .poll(() =>
      page.locator('#home-demo').evaluate((el) => Math.round(el.getBoundingClientRect().top)),
    )
    .toBe(0)
  await page.waitForTimeout(950)
  await page.evaluate(() => {
    const target = document.getElementById('home-demo')!
    const start = new Touch({ identifier: 1, target, clientX: 200, clientY: 620 })
    const end = new Touch({ identifier: 1, target, clientX: 200, clientY: 420 })
    target.dispatchEvent(
      new TouchEvent('touchstart', { bubbles: true, touches: [start], changedTouches: [start] }),
    )
    target.dispatchEvent(
      new TouchEvent('touchmove', {
        bubbles: true,
        cancelable: true,
        touches: [end],
        changedTouches: [end],
      }),
    )
    target.dispatchEvent(
      new TouchEvent('touchend', { bubbles: true, touches: [], changedTouches: [end] }),
    )
  })
  await expect
    .poll(() =>
      page.locator('#home-final').evaluate((el) => Math.round(el.getBoundingClientRect().top)),
    )
    .toBe(0)
  await expect(page.getByRole('link', { name: '지금 바로 시작하기 →' })).toBeInViewport()
  await context.close()
})

test('detail dialog closes with Escape and restores the page scroll', async ({ page, request }) => {
  const { id } = await (
    await request.post('/api/backend/front/beans', { data: { name: unique(), weight: 200 } })
  ).json()
  await page.goto(`/coffee-beans/${id}`)
  await expect(page.getByRole('dialog')).toBeVisible()
  await expect(page.locator('body')).toHaveCSS('overflow', 'hidden')
  await page.keyboard.press('Tab')
  await expect(page.getByRole('link', { name: '수정하기' })).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(page).toHaveURL('/coffee-beans')
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden')
})

test('saving from AI review excludes unseen price and weight candidates', async ({
  page,
  request,
}) => {
  await page.route('**/api/backend/front/extraction', (route) =>
    route.fulfill({
      json: { form: { name: unique(), price: 18000, weight: 200, flavorNotes: [] }, warnings: [] },
    }),
  )
  await page.route('**/api/backend/coffee-bean-card-extractions/**/events', (route) =>
    route.fulfill({
      contentType: 'text/event-stream',
      body: 'event: progress\ndata: {"phase":"LLM"}\n\n',
    }),
  )
  await page.goto('/coffee-beans/new')
  await page.locator('input[type=file]').setInputFiles('public/img/bean-card.png')
  await expect(page.getByRole('heading', { name: 'AI가 읽은 정보를 확인해주세요' })).toBeVisible()
  await page.getByRole('button', { name: '확인한 정보 저장' }).click()
  await expect(page).toHaveURL(/\/coffee-beans\/\d+$/)
  const id = page.url().split('/').at(-1),
    bean = await (await request.get(`/api/backend/front/beans/${id}`)).json()
  expect(bean.price).toBeNull()
  expect(bean.weight).toBeNull()
})
test('purchase place is a draft until applied; cancel preserves selection and selected coordinates are saved', async ({
  page,
  request,
}) => {
  await page.route('**/api/backend/front/session', (route) =>
    route.fulfill({ json: { user: null, securityEnabled: false, kakaoJavascriptKey: 'test-key' } }),
  )
  await page.route('https://dapi.kakao.com/**', (route) =>
    route.fulfill({
      contentType: 'application/javascript',
      body: `
    const places=[{id:'fritz',place_name:'프릳츠 원서점',road_address_name:'서울 종로구 율곡로 83',place_url:'https://place.map.kakao.com/123',x:'126.989',y:'37.577'},{id:'second',place_name:'두 번째 카페',road_address_name:'서울 종로구 테스트로 1',place_url:'https://place.map.kakao.com/456',x:'127.01',y:'37.58'}];
    window.kakao={maps:{load:cb=>cb(),LatLng:class{constructor(lat,lng){this.lat=lat;this.lng=lng}},Map:class{constructor(el,o){this.el=el;this.setCenter(o.center)}relayout(){}setCenter(c){this.el.dataset.mapCenter=c.lat+','+c.lng}},Marker:class{constructor(o){o.map.el.dataset.marker='true'}setMap(){}},services:{Status:{OK:'OK'},Places:class{keywordSearch(q,cb){cb(places,'OK')}}}}};
  `,
    }),
  )
  await page.goto('/coffee-beans/new')
  await page.getByLabel('원두 이름').fill(unique())
  await page.getByRole('button', { name: '다음 단계 →' }).click()
  await page.getByRole('button', { name: '구매처 검색', exact: true }).click()
  let dialog = page.getByRole('dialog', { name: '구매처 검색' })
  await dialog.getByRole('textbox').fill('프릳츠')
  await dialog.getByRole('button', { name: '검색', exact: true }).click()
  await dialog.getByRole('button', { name: '프릳츠 원서점 선택' }).click()
  await expect(dialog.locator('.place-map')).toHaveAttribute('data-map-center', '37.577,126.989')
  await dialog.getByRole('button', { name: '닫기', exact: true }).click()
  await expect(page.locator('.selected-place')).toHaveCount(0)
  await page.getByRole('button', { name: '구매처 검색', exact: true }).click()
  dialog = page.getByRole('dialog', { name: '구매처 검색' })
  await dialog.getByRole('textbox').fill('프릳츠')
  await dialog.getByRole('button', { name: '검색', exact: true }).click()
  await dialog.getByRole('button', { name: '프릳츠 원서점 선택' }).click()
  await dialog.getByRole('button', { name: '이 장소 적용' }).click()
  await expect(page.locator('.selected-place')).toContainText('프릳츠 원서점')
  await expect(page.locator('.selected-place-map')).toHaveAttribute(
    'data-map-center',
    '37.577,126.989',
  )
  await page.getByRole('button', { name: '다른 구매처 검색' }).click()
  dialog = page.getByRole('dialog', { name: '구매처 검색' })
  await dialog.getByRole('button', { name: '검색', exact: true }).click()
  await dialog.getByRole('button', { name: '두 번째 카페 선택' }).click()
  await dialog.getByRole('button', { name: '닫기', exact: true }).click()
  await expect(page.locator('.selected-place')).toContainText('프릳츠 원서점')
  await page.getByRole('button', { name: '확인한 정보 저장' }).click()
  await expect(page).toHaveURL(/\/coffee-beans\/\d+$/)
  const id = page.url().split('/').at(-1),
    bean = await (await request.get(`/api/backend/front/beans/${id}`)).json()
  expect(bean.purchasePlaceName).toBe('프릳츠 원서점')
  expect(bean.purchasePlaceLatitude).toBe(37.577)
  expect(bean.purchasePlaceLongitude).toBe(126.989)
})
