<template>
  <div class="flex min-h-screen items-center justify-center py-12 sm:py-16">
    <UContainer class="w-full">
      <div class="mx-auto w-full max-w-md">
        <UCard>
          <template #header>
            <div class="space-y-2 text-center">
              <h1 class="text-2xl font-semibold">Welcome back</h1>
              <p class="text-sm text-gray-500">
                Sign in with a magic link or use your preferred provider. No passwords required.
              </p>
            </div>
          </template>

          <UForm :state="formState" class="space-y-6" @submit="handleSubmit">
            <UFormField
              label="Email address"
              name="email"
              :error="errors.email"
              help="We will send a single-use link to this address."
            >
              <UInput v-model="formState.email" type="email" placeholder="you@example.com" autocomplete="email" required size="lg" color="neutral" class="w-full" />
            </UFormField>

            <div class="space-y-3">
              <UButton type="submit" size="lg" block :loading="isSubmitting" :disabled="isSubmitting">
                Send magic link
              </UButton>
              <p class="text-xs text-gray-500">
                By signing in you agree to receive a one-time email to access your workspace.
              </p>
            </div>
          </UForm>

          <div class="my-6">
            <div class="relative flex items-center">
              <span class="h-px w-full bg-gray-200"></span>
              <span class="relative mx-4 whitespace-nowrap text-xs uppercase tracking-wide text-gray-400">Or continue with</span>
              <span class="h-px w-full bg-gray-200"></span>
            </div>
          </div>

          <div class="grid gap-3">
            <UButton
              v-for="provider in oauthProviders"
              :key="provider.registrationId"
              block
              color="primary"
              variant="soft"
              size="lg"
              :aria-label="`Sign in with ${provider.label}`"
              :disabled="isSubmitting"
              @click="handleProviderClick(provider)"
            >
              <template #leading>
                <UIcon :name="provider.icon" class="text-xl" aria-hidden="true" />
              </template>
              Continue with {{ provider.label }}
            </UButton>
          </div>
        </UCard>
      </div>
    </UContainer>
  </div>
</template>

<style scoped>
</style>

<script lang="ts">
import { defineComponent } from 'vue'
import { useRuntimeConfig, useNuxtApp, useToast, definePageMeta } from '#imports'
import { $fetch } from 'ofetch'

type OAuthProvider = {
  label: string
  icon: string
  registrationId: string
  authorizationUrl: string
}

type RuntimeOAuthEntry = OAuthProvider | string

type RuntimeOAuthConfig = Array<RuntimeOAuthEntry> | undefined

const normalizeProviders = (providers: RuntimeOAuthConfig, apiBase: string): OAuthProvider[] => {
  if (!providers || providers.length === 0) {
    return [
      {
        label: 'Google',
        icon: 'i-logos-google-icon',
        registrationId: 'google',
        authorizationUrl: `${apiBase}/oauth2/authorization/google`
      }
    ]
  }

  return providers.map((entry) => {
    if (typeof entry === 'string') {
      return {
        label: entry.replace(/^[a-z]/, (match) => match.toUpperCase()),
        icon: inferIcon(entry),
        registrationId: entry,
        authorizationUrl: `${apiBase}/oauth2/authorization/${entry}`
      }
    }

    const registrationId = entry.registrationId || entry.label.toLowerCase()
    return {
      label: entry.label,
      icon: entry.icon || inferIcon(registrationId),
      registrationId,
      authorizationUrl: entry.authorizationUrl || `${apiBase}/oauth2/authorization/${registrationId}`
    }
  })
}

const inferIcon = (registrationId: string): string => {
  switch (registrationId) {
    case 'google':
      return 'i-logos-google-icon'
    case 'microsoft':
    case 'azure':
      return 'i-logos-microsoft-icon'
    default:
      return 'i-heroicons-user-circle'
  }
}

export default defineComponent({
  name: 'LoginPage',
  setup() {
    definePageMeta({
      title: 'Sign in | teuportal'
    })
    const toast = useToast()
    const config = useRuntimeConfig()
    const apiBase = (config.public?.apiBase ?? '/api').replace(/\/$/, '')
    const providers = normalizeProviders(config.public?.oauthProviders as RuntimeOAuthConfig, apiBase)

    return {
      toast,
      addToast: toast.add,
      apiBase,
      oauthProviders: providers
    }
  },
  data() {
    return {
      formState: {
        email: ''
      },
      errors: {
        email: undefined as string | undefined
      },
      isSubmitting: false
    }
  },
  methods: {
    validateEmail(email: string) {
      if (!email) {
        return 'Email address is required.'
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return 'Enter a valid email address.'
      }
      return undefined
    },
    async handleSubmit() {
      const trimmedEmail = this.formState.email.trim()
      this.errors.email = this.validateEmail(trimmedEmail)
      if (this.errors.email) {
        return
      }

      this.formState.email = trimmedEmail
      this.isSubmitting = true
      try {
        const response = await $fetch<{ message?: string }>(`${this.apiBase}/auth/email`, {
          method: 'POST',
          body: { email: trimmedEmail },
          credentials: 'include',
          headers: {
            Accept: 'application/json'
          }
        })
        const successMessage = response?.message ?? 'If the email exists, we have sent a sign-in link.'
        this.toast.add({
          title: 'Check your email',
          description: successMessage,
          color: 'success'
        })
        this.formState.email = ''
      } catch (error: any) {
        console.error('Error during login request:', error)
        const message = error?.data?.message ?? 'Unable to send the magic link right now.'
        this.addToast({
          title: 'Request failed',
          description: message,
          icon: 'i-heroicons-exclamation-triangle',
          color: 'error'
        })
      } finally {
        this.isSubmitting = false
      }
    },
    handleProviderClick(provider: OAuthProvider) {
      this.isSubmitting = true
      window.location.href = provider.authorizationUrl
    }
  }
})
</script>
