// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  pages: true,
  dir: {
    pages: 'pages'
  },
  modules: ['@pinia/nuxt', '@nuxt/ui'],
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE ?? '/api',
      requireSetup: process.env.NUXT_PUBLIC_REQUIRE_SETUP === 'true',
      appName: process.env.NUXT_PUBLIC_APP_NAME ?? 'teuportal'
    }
  },
  typescript: {
    shim: false
  }
})
