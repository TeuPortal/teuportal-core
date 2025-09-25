<script setup lang="ts">
const config = useRuntimeConfig()
const apiBase = computed(() => `${config.public.apiBase}`.replace(/\/$/, ''))

const {
  data: health,
  pending: healthPending,
  error: healthError
} = await useFetch(() => `${apiBase.value}/health`, {
  credentials: 'include',
  key: 'health-check'
})
</script>

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
          <UBadge color="gray" variant="soft">{{ apiBase }}/health</UBadge>
        </div>
      </template>
      <div class="space-y-2 text-sm text-gray-700">
        <template v-if="healthPending">
          <span>Checking service health…</span>
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
