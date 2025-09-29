<template>
  <Suspense>
      <UDashboardGroup unit="rem" storage="local">
        <UDashboardSidebar
          id="default"
          v-model:open="sidebarOpen"
          collapsible
          resizable
          class="bg-elevated/25"
          :ui="{ footer: 'lg:border-t lg:border-default' }"
        >
          <template #header="{ collapsed }">
            <TeamsMenu :collapsed="collapsed" />
          </template>

          <template #default="{ collapsed }">
            <UDashboardSearchButton :collapsed="collapsed" class="bg-transparent ring-default" />

            <UNavigationMenu
              :collapsed="collapsed"
              :items="links[0]"
              orientation="vertical"
              tooltip
              popover
            />

            <UNavigationMenu
              :collapsed="collapsed"
              :items="links[1]"
              orientation="vertical"
              tooltip
              class="mt-auto"
            />
          </template>

          <template #footer="{ collapsed }">
            <UserMenu :collapsed="collapsed" />
          </template>
        </UDashboardSidebar>

        <UDashboardSearch :groups="groups" />

        <RouterView />

        <NotificationsSlideover />
      </UDashboardGroup>
  </Suspense>
</template>

<script lang="ts">
import { defineNuxtComponent } from '#imports'
import type { NavigationMenuItem } from '@nuxt/ui'
import { useUiStore } from '~/stores/ui'

interface SearchGroup {
  id: string
  label: string
  items: NavigationMenuItem[]
}

export default defineNuxtComponent({
  name: 'AppIndexPage',
  data() {
    return {
      uiStore: useUiStore()
    }
  },
  computed: {
    sidebarOpen: {
      get(): boolean {
        return this.uiStore.sidebarOpen
      },
      set(value: boolean) {
        this.uiStore.setSidebarOpen(value)
      }
    },
    links(): NavigationMenuItem[][] {
      const closeSidebar = () => {
        this.uiStore.setSidebarOpen(false)
      }

      const primary: NavigationMenuItem[] = [
        {
          label: 'Home',
          icon: 'i-lucide-house',
          to: '/',
          onSelect: closeSidebar
        },
        {
          label: 'Inbox',
          icon: 'i-lucide-inbox',
          to: '/inbox',
          badge: '4',
          onSelect: closeSidebar
        },
        {
          label: 'Customers',
          icon: 'i-lucide-users',
          to: '/customers',
          onSelect: closeSidebar
        },
        {
          label: 'Settings',
          to: '/settings',
          icon: 'i-lucide-settings',
          defaultOpen: true,
          type: 'trigger',
          children: [
            {
              label: 'General',
              to: '/settings',
              exact: true,
              onSelect: closeSidebar
            },
            {
              label: 'Members',
              to: '/settings/members',
              onSelect: closeSidebar
            },
            {
              label: 'Notifications',
              to: '/settings/notifications',
              onSelect: closeSidebar
            },
            {
              label: 'Security',
              to: '/settings/security',
              onSelect: closeSidebar
            }
          ]
        }
      ]

      const secondary: NavigationMenuItem[] = [
        {
          label: 'Feedback',
          icon: 'i-lucide-message-circle',
          to: 'https://github.com/nuxt-ui-templates/dashboard-vue',
          target: '_blank'
        },
        {
          label: 'Help & Support',
          icon: 'i-lucide-info',
          to: 'https://github.com/nuxt/ui',
          target: '_blank'
        }
      ]

      return [primary, secondary]
    },
    groups(): SearchGroup[] {
      const flattened = this.links.flat()
      const routePath = this.$route.path

      return [
        {
          id: 'links',
          label: 'Go to',
          items: flattened
        },
        {
          id: 'code',
          label: 'Code',
          items: [
            {
              id: 'source',
              label: 'View page source',
              icon: 'simple-icons:github',
              to: `https://github.com/nuxt-ui-templates/dashboard-vue/blob/main/src/pages${routePath === '/' ? '/index' : routePath}.vue`,
              target: '_blank'
            }
          ]
        }
      ]
    }
  }
})
</script>
