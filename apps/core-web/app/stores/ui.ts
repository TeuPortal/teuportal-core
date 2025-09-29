import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const sidebarOpen = ref(true)
  const notificationsOpen = ref(false)

  const setSidebarOpen = (value: boolean) => {
    sidebarOpen.value = value
  }

  const toggleSidebar = () => {
    sidebarOpen.value = !sidebarOpen.value
  }

  const setNotificationsOpen = (value: boolean) => {
    notificationsOpen.value = value
  }

  const openNotifications = () => {
    notificationsOpen.value = true
  }

  const closeNotifications = () => {
    notificationsOpen.value = false
  }

  return {
    sidebarOpen,
    notificationsOpen,
    setSidebarOpen,
    toggleSidebar,
    setNotificationsOpen,
    openNotifications,
    closeNotifications
  }
})
