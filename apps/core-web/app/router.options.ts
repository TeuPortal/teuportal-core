import type { RouterConfig } from '@nuxt/schema'

export default <RouterConfig>{
  routes: (_routes) => [
    {
      name: 'index-manual',
      path: '/',
      component: () => import('../pages/index.vue').then((m) => m.default || m)
    },
    ..._routes
  ]
}
