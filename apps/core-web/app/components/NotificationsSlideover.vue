<script setup lang="ts">
import type { AvatarProps } from '@nuxt/ui'
import { storeToRefs } from 'pinia'
import { useFetch, formatTimeAgo } from '@vueuse/core'
import { useUiStore } from '~/stores/ui'

const { data: notifications } = useFetch('https://dashboard-template.nuxt.dev/api/notifications', { initialData: [] }).json<Notification[]>()

export type UserStatus = 'subscribed' | 'unsubscribed' | 'bounced'

export interface Notification {
  id: number
  unread?: boolean
  sender: User
  body: string
  date: string
}

export interface User {
  id: number
  name: string
  email: string
  avatar?: AvatarProps
  status: UserStatus
  location: string
}

const uiStore = useUiStore()
const { notificationsOpen } = storeToRefs(uiStore)
</script>

<template>
  <USlideover
    v-model:open="notificationsOpen"
    title="Notifications"
  >
    <template #body>
      <RouterLink
        v-for="notification in notifications"
        :key="notification.id"
        :to="`/inbox?id=${notification.id}`"
        class="px-3 py-2.5 rounded-md hover:bg-elevated/50 flex items-center gap-3 relative -mx-3 first:-mt-3 last:-mb-3"
      >
        <UChip
          color="error"
          :show="!!notification.unread"
          inset
        >
          <UAvatar
            v-bind="notification.sender.avatar"
            :alt="notification.sender.name"
            size="md"
          />
        </UChip>

        <div class="text-sm flex-1">
          <p class="flex items-center justify-between">
            <span class="text-highlighted font-medium">{{ notification.sender.name }}</span>

            <time
              :datetime="notification.date"
              class="text-muted text-xs"
              v-text="formatTimeAgo(new Date(notification.date))"
            />
          </p>

          <p class="text-dimmed">
            {{ notification.body }}
          </p>
        </div>
      </RouterLink>
    </template>
  </USlideover>
</template>
