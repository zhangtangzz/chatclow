<template>
  <div v-if="loading" class="app-loading">
    <div class="loader" />
  </div>
  <ErrorBoundary v-else>
    <router-view />
  </ErrorBoundary>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from './stores/user'
import ErrorBoundary from './components/ErrorBoundary.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)

onMounted(async () => {
  if (localStorage.getItem('token')) {
    const ok = await userStore.fetchUserInfo()
    if (!ok) {
      router.push('/login')
    }
  }
  loading.value = false
})
</script>

<style scoped>
.app-loading {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-page, #fdfbf7);
}
.loader {
  width: 36px;
  height: 36px;
  border: 4px solid rgba(45, 45, 45, 0.15);
  border-top-color: var(--primary, #ff4d4d);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
