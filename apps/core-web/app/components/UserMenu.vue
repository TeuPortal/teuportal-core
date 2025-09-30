<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import type { DropdownMenuItem } from '@nuxt/ui'
import { useColorMode } from '@vueuse/core'
import { useAppConfig, useRouter, useRuntimeConfig, useState } from '#imports'
import { $fetch } from 'ofetch'

interface SessionState {
  userId: string
  email: string
  name: string
  roles: string[]
  companyId: string
}

defineProps<{
  collapsed?: boolean
}>()

const colorMode = useColorMode()
const appConfig = useAppConfig()
const router = useRouter()
const config = useRuntimeConfig()
const apiBase = (config.public?.apiBase ?? '/api').replace(/\/$/, '')
const sessionState = useState<SessionState | null>('session', () => null)
const isLoggingOut = ref(false)

const colors = ['red', 'orange', 'amber', 'yellow', 'lime', 'green', 'emerald', 'teal', 'cyan', 'sky', 'blue', 'indigo', 'violet', 'purple', 'fuchsia', 'pink', 'rose']
const neutrals = ['slate', 'gray', 'zinc', 'neutral', 'stone']

const DEFAULT_USER_NAME = 'Signed in user'
const DEFAULT_USER_EMAIL = ''

const buildAvatar = (displayName: string, email: string) => {
  const base = (displayName || email || 'User').trim()
  const initials = base
    .split(/\s+/)
    .filter(Boolean)
    .map(segment => segment[0]?.toUpperCase() ?? '')
    .join('')
    .slice(0, 2)
  return {
    alt: base,
    text: initials || base[0]?.toUpperCase() || 'U'
  }
}

const resetUser = () => {
  user.value = {
    name: DEFAULT_USER_NAME,
    email: DEFAULT_USER_EMAIL,
    avatar: buildAvatar(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL)
  }
}

const user = ref({
  name: DEFAULT_USER_NAME,
  email: DEFAULT_USER_EMAIL,
  avatar: buildAvatar(DEFAULT_USER_NAME, DEFAULT_USER_EMAIL)
})

watchEffect(() => {
  const current = sessionState.value
  if (!current) {
    resetUser()
    return
  }

  const displayName = current.name?.trim() || current.email
  user.value = {
    name: displayName,
    email: current.email,
    avatar: buildAvatar(displayName, current.email)
  }
})

const handleLogout = async () => {
  if (isLoggingOut.value) {
    return
  }

  isLoggingOut.value = true
  try {
    await $fetch(`${apiBase}/auth/logout`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        Accept: 'application/json'
      }
    })
    sessionState.value = null
    await router.replace('/login')
  } catch (error) {
    console.error('Failed to log out', error)
  } finally {
    isLoggingOut.value = false
  }
}

const items = computed<DropdownMenuItem[][]>(() => ([[{
  type: 'label',
  label: user.value.name,
  avatar: user.value.avatar
}], [{
  label: 'Profile',
  icon: 'i-lucide-user'
}, {
  label: 'Billing',
  icon: 'i-lucide-credit-card'
}, {
  label: 'Settings',
  icon: 'i-lucide-settings',
  to: '/settings'
}], [{
  label: 'Theme',
  icon: 'i-lucide-palette',
  children: [{
    label: 'Primary',
    slot: 'chip',
    chip: appConfig.ui.colors.primary,
    content: {
      align: 'center',
      collisionPadding: 16
    },
    children: colors.map(color => ({
      label: color,
      chip: color,
      slot: 'chip',
      checked: appConfig.ui.colors.primary === color,
      type: 'checkbox',
      onSelect: (event) => {
        event.preventDefault()
        appConfig.ui.colors.primary = color
      }
    }))
  }, {
    label: 'Neutral',
    slot: 'chip',
    chip: appConfig.ui.colors.neutral === 'neutral' ? 'old-neutral' : appConfig.ui.colors.neutral,
    content: {
      align: 'end',
      collisionPadding: 16
    },
    children: neutrals.map(color => ({
      label: color,
      chip: color === 'neutral' ? 'old-neutral' : color,
      slot: 'chip',
      type: 'checkbox',
      checked: appConfig.ui.colors.neutral === color,
      onSelect: (event) => {
        event.preventDefault()
        appConfig.ui.colors.neutral = color
      }
    }))
  }]
}, {
  label: 'Appearance',
  icon: 'i-lucide-sun-moon',
  children: [{
    label: 'Light',
    icon: 'i-lucide-sun',
    type: 'checkbox',
    checked: colorMode.value === 'light',
    onSelect(event: Event) {
      event.preventDefault()
      colorMode.value = 'light'
    }
  }, {
    label: 'Dark',
    icon: 'i-lucide-moon',
    type: 'checkbox',
    checked: colorMode.value === 'dark',
    onUpdateChecked(checked: boolean) {
      if (checked) {
        colorMode.value = 'dark'
      }
    },
    onSelect(event: Event) {
      event.preventDefault()
    }
  }]
}], [{
  label: 'Documentation',
  icon: 'i-lucide-book-open',
  to: 'https://ui.nuxt.com/docs/getting-started/installation/vue',
  target: '_blank'
}, {
  label: 'GitHub repository',
  icon: 'simple-icons:github',
  to: 'https://github.com/nuxt-ui-templates/dashboard-vue',
  target: '_blank'
}], [{
  label: 'Log out',
  icon: 'i-lucide-log-out',
  disabled: isLoggingOut.value,
  onSelect: async (event: Event) => {
    event.preventDefault()
    await handleLogout()
  }
}]]))
</script>

<template>
  <UDropdownMenu
    :items="items"
    :content="{ align: 'center', collisionPadding: 12 }"
    :ui="{ content: collapsed ? 'w-48' : 'w-(--reka-dropdown-menu-trigger-width)' }"
  >
    <UButton
      v-bind="{
        ...user,
        label: collapsed ? undefined : user?.name,
        trailingIcon: collapsed ? undefined : 'i-lucide-chevrons-up-down'
      }"
      color="neutral"
      variant="ghost"
      block
      :square="collapsed"
      class="data-[state=open]:bg-elevated"
      :ui="{
        trailingIcon: 'text-dimmed'
      }"
    />

    <template #chip-leading="{ item }">
      <span
        :style="{
          '--chip-light': `var(--color-${(item as any).chip}-500)`,
          '--chip-dark': `var(--color-${(item as any).chip}-400)`
        }"
        class="ms-0.5 size-2 rounded-full bg-(--chip-light) dark:bg-(--chip-dark)"
      />
    </template>
  </UDropdownMenu>
</template>
