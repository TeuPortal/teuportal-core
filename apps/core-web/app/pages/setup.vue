<template>
  <div class="flex min-h-screen items-center justify-center py-12 sm:py-16 lg:py-20">
    <UContainer class="w-full">
      <div class="mx-auto w-full max-w-2xl">
        <UCard>
          <template #header>
            <div class="space-y-2">
              <h1 class="text-2xl font-semibold">First-time setup</h1>
              <p class="text-sm text-gray-500">
                Provide the details we need to personalise your portal. This information can be seeded when deploying to production.
              </p>
            </div>
          </template>

          <div v-if="!completed" class="space-y-8">
            <UAlert
              icon="i-heroicons-information-circle"
              color="primary"
              variant="soft"
              title="One-time configuration"
              description="Run this step locally to bootstrap your company workspace. In production, you can automate it with environment variables."
            />

            <UForm :state="formState" class="space-y-6" @submit="handleSubmit">
              <div class="grid gap-6 min-[520px]:grid-cols-2">
                <UFormField
                  label="Company name"
                  name="companyName"
                  :error="errors.companyName"
                  size="lg"
                >
                  <UInput v-model="formState.companyName" placeholder="Acme Inc." autocomplete="organization" required size="lg" color="neutral" />
                </UFormField>

                <UFormField
                  label="Administrator email"
                  name="adminEmail"
                  :error="errors.adminEmail"
                  help="We will use this address for the primary owner and setup notifications."
                  size="lg"
                >
                  <UInput v-model="formState.adminEmail" type="email" placeholder="admin@example.com" autocomplete="email" required size="lg" color="neutral" />
                </UFormField>
              </div>

              <div class="flex flex-wrap items-center gap-3">
                <UButton type="submit" size="lg">
                  Complete setup
                </UButton>
                <span class="text-xs text-gray-500">
                  You can update these details later from the settings screen.
                </span>
              </div>
            </UForm>
          </div>

          <div v-else class="space-y-6 text-center">
            <UIcon name="i-heroicons-check-badge" class="mx-auto h-12 w-12 text-green-500" aria-hidden="true" />
            <div class="space-y-2">
              <h2 class="text-xl font-semibold">You are all set</h2>
              <p class="text-sm text-gray-500">
                Your workspace has been initialised. You can revisit this page at any time to review your settings.
              </p>
            </div>
            <div class="flex flex-wrap items-center justify-center gap-3">
              <UButton to="/app" size="lg" color="primary">
                Go to dashboard
              </UButton>
              <UButton variant="ghost" color="primary" to="/login">
                Back to sign in
              </UButton>
            </div>
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
import { useToast } from '#imports'

definePageMeta({
  title: 'Workspace setup | teuportal'
})

export default defineComponent({
  name: 'SetupPage',
  setup() {
    const toast = useToast()
    return { addToast: toast.add }
  },
  data() {
    return {
      formState: {
        companyName: '',
        adminEmail: ''
      },
      errors: {
        companyName: undefined as string | undefined,
        adminEmail: undefined as string | undefined
      },
      completed: false
    }
  },
  methods: {
    validate() {
      this.errors.companyName = this.formState.companyName ? undefined : 'Company name is required.'
      if (!this.formState.adminEmail) {
        this.errors.adminEmail = 'Administrator email is required.'
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.formState.adminEmail)) {
        this.errors.adminEmail = 'Enter a valid email address.'
      } else {
        this.errors.adminEmail = undefined
      }

      return !this.errors.companyName && !this.errors.adminEmail
    },
    handleSubmit(event: Event) {
      event.preventDefault()
      if (!this.validate()) {
        return
      }

      this.completed = true
      this.addToast({
        title: 'Setup complete',
        description: 'Your workspace is ready. Continue to the dashboard to start inviting your team.',
        icon: 'i-heroicons-check-circle',
        color: 'success'
      })
    }
  }
})
</script>

