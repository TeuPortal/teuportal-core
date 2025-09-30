<template>
  <section class="space-y-6">
    <div class="space-y-2">
      <h1 class="text-3xl font-semibold">{{ config.public.appName }}</h1>
      <p class="text-gray-500">Session-aware portal UI powered by Nuxt 3.</p>
    </div>

    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">API connection</span>
          <UBadge color="primary" variant="solid">{{ config.public.apiBase }}</UBadge>
        </div>
      </template>
      <p>
        This app expects a session cookie issued by the Spring Boot API. Configure the API
        base in <code>.env</code> or <code>NUXT_PUBLIC_API_BASE</code> before running locally.
      </p>
    </UCard>

    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">Live health probe</span>
          <UBadge color="primary" variant="soft">{{ apiBase }}/health</UBadge>
        </div>
      </template>
      <div class="space-y-2 text-sm text-gray-700">
        <template v-if="healthPending">
          <span>Checking service health.</span>
        </template>
        <template v-else-if="healthError">
          <span class="text-red-600">Failed: {{ healthError.message }}</span>
        </template>
        <template v-else>
          <span class="font-medium text-green-600">Status: {{ health?.status ?? 'unknown' }}</span>
          <pre class="bg-gray-100 rounded p-3 overflow-auto text-xs">{{ JSON.stringify(health, null, 2) }}</pre>
        </template>
      </div>
    </UCard>
  </section>
</template>


<script lang="ts">
import { defineNuxtComponent, useRuntimeConfig } from '#imports'
import { $fetch } from 'ofetch'

interface HealthPayload {
  status?: string
  timestamp?: string
  [key: string]: unknown
}

export default defineNuxtComponent({
  data() {
    const config = useRuntimeConfig()

    return {
      config,
      health: null as HealthPayload | null,
      healthPending: true,
      healthError: null as Error | null
    }
  },
  computed: {
    apiBase(): string {
      return `${this.config.public.apiBase}`.replace(/\/$/, '')
    }
  },
  methods: {
    async loadHealth() {
      this.healthPending = true
      this.healthError = null

      try {
        this.health = await $fetch<HealthPayload>(`${this.apiBase}/health`, {
          credentials: 'include'
        })
      } catch (error: unknown) {
        console.error('Health check failed', error)
        this.health = null
        this.healthError = error instanceof Error ? error : new Error('Health check failed')
      } finally {
        this.healthPending = false
      }
    }
  },
  serverPrefetch() {
    return this.loadHealth()
  },
  mounted() {
    if (!this.health && !this.healthError) {
      this.loadHealth()
    }
  }
})
</script>
