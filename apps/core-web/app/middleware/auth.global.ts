import { defineNuxtRouteMiddleware, navigateTo, useRequestHeaders, useRuntimeConfig, useState } from '#imports'
import { $fetch } from 'ofetch'

type SessionResponse = {
  userId: string
  email: string
  name: string
  roles: string[]
  companyId: string
}

const PROTECTED_PREFIXES = ['/app']

export default defineNuxtRouteMiddleware(async (to) => {
  const isProtectedRoute = PROTECTED_PREFIXES.some(prefix => to.path === prefix || to.path.startsWith(`${prefix}/`))
  if (!isProtectedRoute) {
    return
  }

  const config = useRuntimeConfig()
  const apiBase = (config.public?.apiBase ?? '/api').replace(/\/$/, '')
  const sessionState = useState<SessionResponse | null>('session', () => null)

  if (process.client && sessionState.value) {
    return
  }

  const headers = process.server ? { cookie: useRequestHeaders(['cookie']).cookie ?? '' } : undefined

  try {
    const data = await $fetch<SessionResponse>(`${apiBase}/auth/session`, {
      method: 'GET',
      credentials: 'include',
      headers,
      cache: 'no-cache'
    })
    sessionState.value = data
  } catch (error) {
    sessionState.value = null
    const redirectOptions = process.server ? { redirectCode: 302 } : { replace: true }
    return navigateTo('/login', redirectOptions)
  }
})
