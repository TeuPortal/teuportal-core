// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  pages: true,
  modules: ['@pinia/nuxt', '@nuxt/ui'],
  css: ['~/assets/css/main.css'],
  colorMode: {
    preference: 'light',
    fallback: 'light',
    classSuffix: '',
    storageKey: 'teuportal-color-mode'
  },
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE ?? '/api',
      requireSetup: process.env.NUXT_PUBLIC_REQUIRE_SETUP === 'true',
      appName: process.env.NUXT_PUBLIC_APP_NAME ?? 'teuportal'
    }
  },
  nitro: {
    devProxy: {
      '/api': {
        target: process.env.NUXT_DEV_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      }
    }
  },
  typescript: {
    shim: false
  }
})
