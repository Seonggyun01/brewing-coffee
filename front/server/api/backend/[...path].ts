export default defineEventHandler(async (event) => {
  const path = getRouterParam(event, 'path') || ''
  // Keep the proxy limited to the application's API, never arbitrary URLs.
  const allowed =
    /^(front\/(session|image-preview|beans(?:\/\d+(?:\/form)?)?|brews(?:\/\d+(?:\/form)?)?|cafes(?:\/\d+(?:\/form)?)?|extraction)|origin-map\/countries|maps\/cafes|taste-space(?:\/rebuild)?|coffee-bean-card-extractions\/[A-Za-z0-9_-]{1,100}\/events)$/
  if (!allowed.test(path)) throw createError({ statusCode: 404 })
  const config = useRuntimeConfig(event)
  const query = getRequestURL(event).search
  setResponseHeader(event, 'cache-control', 'no-store')
  return proxyRequest(event, `${config.backendUrl}/api/${path}${query}`, {
    fetchOptions: { redirect: 'manual' },
  })
})
