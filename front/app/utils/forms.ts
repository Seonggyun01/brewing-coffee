export const tasteAxes = [
  { key: 'acidity', label: '산미' },
  { key: 'sweetness', label: '단맛' },
  { key: 'bitterness', label: '쓴맛' },
  { key: 'body', label: '바디' },
  { key: 'aroma', label: '향' },
  { key: 'balance', label: '밸런스' },
] as const
export function today() {
  return new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' }).format(new Date())
}
export function emptyForm() {
  return {
    name: '',
    roastery: '',
    originCountryCode: '',
    country: '',
    region: '',
    farm: '',
    variety: '',
    altitude: '',
    processType: '',
    status: 'CURRENT',
    flavorNotes: [] as string[],
    customFlavorNotesText: '',
    memo: '',
    purchasedDate: '',
    price: '',
    weight: '',
    purchasePlaceName: '',
    purchasePlaceAddress: '',
    purchasePlaceUrl: '',
    purchasePlaceLatitude: null,
    purchasePlaceLongitude: null,
    purchasePlaceType: 'CAFE',
    coffeeBeanId: '',
    brewedDate: today(),
    visitedDate: today(),
    temperatureType: 'HOT',
    brewMethod: '',
    beanAmount: '',
    waterAmount: '',
    waterTemperature: '',
    grinderModel: '',
    grinderSetting: '',
    grindSizeMicron: '',
    brewTimeSec: '',
    pourSteps: [] as any[],
    rating: '',
    acidity: '',
    sweetness: '',
    bitterness: '',
    body: '',
    aroma: '',
    balance: '',
    feelingTags: [] as string[],
    customFeelingTagsText: '',
    cafeName: '',
    cafeAddress: '',
    cafeUrl: '',
    cafeLatitude: null,
    cafeLongitude: null,
    recordRecipe: false,
  }
}
export function payload(form: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(form).map(([key, value]) => [key, value === '' ? null : value]),
  )
}
export function safeLink(value?: string) {
  return value && /^https?:\/\//i.test(value) ? value : undefined
}
