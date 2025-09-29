<template>
   <UDashboardPanel id="home">
    <template #header>
      <UDashboardNavbar title="Home" :ui="{ right: 'gap-3' }">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>

        <template #right>
          <UTooltip text="Notifications" :shortcuts="['N']">
            <UButton
              color="neutral"
              variant="ghost"
              square
              @click="uiStore.openNotifications()"
            >
              <UChip color="error" inset>
                <UIcon name="i-lucide-bell" class="size-5 shrink-0" />
              </UChip>
            </UButton>
          </UTooltip>

          <UDropdownMenu :items="items">
            <UButton icon="i-lucide-plus" size="md" class="rounded-full" />
          </UDropdownMenu>
        </template>
      </UDashboardNavbar>

      <UDashboardToolbar>
        <template #left>
          <!-- NOTE: The `-ms-1` class is used to align with the `DashboardSidebarCollapse` button here. -->
          <HomeDateRangePicker v-model="range" class="-ms-1" />

          <!--HomePeriodSelect v-model="period" :range="range" /-->
        </template>
      </UDashboardToolbar>
    </template>

    <template #body>
      <!--HomeStats :period="period" :range="range" />
      <HomeChart :period="period" :range="range" />
      <HomeSales :period="period" :range="range" /-->
    </template>
  </UDashboardPanel>
</template>

<script lang="ts">
import { defineNuxtComponent } from '#imports'
import type { DropdownMenuItem } from '@nuxt/ui'
import { sub } from 'date-fns'
import { useUiStore } from '~/stores/ui'
import HomeDateRangePicker from '~/components/utils/date/HomeDateRangePicker.vue'

export type Period = 'daily' | 'weekly' | 'monthly'

export interface Range {
  start: Date
  end: Date
}

export default defineNuxtComponent({
  name: 'AppHomePage',
  components: {
    HomeDateRangePicker
  },
  setup() {
    definePageMeta({
      title: 'Home | teuportal',
      layout: 'app'
    })
  },
  data() {
    const uiStore = useUiStore()

    return {
      uiStore,
      period: 'daily' as Period,
      range: {
        start: sub(new Date(), { days: 14 }),
        end: new Date()
      } as Range,
      items: [[{
        label: 'New mail',
        icon: 'i-lucide-send',
        to: '/inbox'
      }, {
        label: 'New customer',
        icon: 'i-lucide-user-plus',
        to: '/customers'
      }]] as DropdownMenuItem[][]
    }
  }
})
</script>
