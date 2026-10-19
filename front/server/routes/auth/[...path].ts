export default defineEventHandler((event) => {
  const path = getRouterParam(event, 'path') || ''
  if (!['oauth2/authorization/kakao', 'logout'].includes(path))
    throw createError({ statusCode: 404 })
  return proxyAuth(event, path)
})
