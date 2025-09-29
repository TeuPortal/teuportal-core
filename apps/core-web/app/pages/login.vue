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
              :key="provider.name"
              block
              color="gray"
              variant="soft"
              size="lg"
              :aria-label="`Sign in with ${provider.name}`"
              :disabled="isSubmitting"
              @click="handleProviderClick(provider)"
            >
              <template #leading>
                <UIcon :name="provider.icon" class="text-xl" aria-hidden="true" />
              </template>
              Continue with {{ provider.name }}
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

export default defineComponent({
  name: 'LoginPage',
  setup() {
    definePageMeta({
      title: 'Sign in | teuportal'
    })
    const toast = useToast()
    const config = useRuntimeConfig()
    //const { $fetch } = useNuxtApp()
    return {
      toast: toast,
      addToast: toast.add,
      apiBase: (config.public?.apiBase ?? '/api').replace(/\/$/, ''),
      //$fetch
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
      isSubmitting: false,
      oauthProviders: [
        { name: 'Google', icon: 'i-logos-google-icon', registrationId: 'google' },
        { name: 'Microsoft', icon: 'i-logos-microsoft-icon', registrationId: 'microsoft' }
      ]
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
      console.log('Submitting', this.formState)
      this.errors.email = this.validateEmail(this.formState.email)
      if (this.errors.email) {
        return
      }
      console.log('Validated, submitting...')

      this.isSubmitting = true
      try {
        /*await this.$fetch(`${this.apiBase}/auth/magic-link`, {
          method: 'POST',
          body: { email: this.formState.email },
          credentials: 'include'
        })*/
        this.toast.add({
          title: 'Success',
          description: 'Your action was completed successfully.',
          color: 'success'
        })
        console.log('Submitted');
      } catch (error: any) {
        const message = error?.data?.message ?? 'Unable to send the magic link right now.'
        this.addToast({
          title: 'Request failed',
          description: message,
          icon: 'i-heroicons-exclamation-triangle',
          color: 'red'
        })
      } finally {
        this.isSubmitting = false
      }
    },
    handleProviderClick(provider: { name: string; registrationId: string }) {
      window.location.href = `${this.apiBase}/oauth2/authorization/${provider.registrationId}`
    }
  }
})
</script>




