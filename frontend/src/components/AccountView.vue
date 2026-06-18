<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api'
import { useAuth } from '../composables/useAuth'
import AppCard from './ui/AppCard.vue'
import AppButton from './ui/AppButton.vue'
import SectionLabel from './ui/SectionLabel.vue'

const emit = defineEmits(['open-chat'])
const { token, email, logout } = useAuth()

const lastLogin = ref('')
const error = ref('')

function formatDate(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return Number.isNaN(d.getTime())
    ? iso
    : d.toLocaleString('zh-TW', { dateStyle: 'long', timeStyle: 'medium' })
}

onMounted(async () => {
  try {
    const res = await api.lastLogin(token.value)
    lastLogin.value = res.last_login_at
  } catch (e) {
    // Token expired / invalid → drop the session.
    error.value = e.message
    if (/(401|unauthor)/i.test(e.message)) logout()
  }
})
</script>

<template>
  <div class="account">
    <AppCard accent-top class="card">
      <SectionLabel>Account</SectionLabel>
      <h1 class="title">歡迎回來</h1>
      <p class="email">{{ email }}</p>

      <p v-if="error" class="banner error" role="alert">{{ error }}</p>

      <div class="stat">
        <span class="small-caps label">最後登入時間</span>
        <p class="value">{{ formatDate(lastLogin) }}</p>
      </div>

      <div class="actions">
        <AppButton variant="primary" @click="emit('open-chat')">前往聊天室</AppButton>
        <AppButton variant="secondary" @click="logout">登出</AppButton>
      </div>
    </AppCard>
  </div>
</template>

<style scoped>
.account {
  position: relative;
  z-index: 1;
  min-height: calc(100vh - 5rem);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.25rem;
}
.card {
  width: 100%;
  max-width: 30rem;
  padding: 2.5rem;
}
.title {
  font-size: 2.5rem;
  line-height: 1.15;
  letter-spacing: -0.01em;
  margin: 1.25rem 0 0.25rem;
}
.email {
  color: var(--muted-foreground);
  margin: 0 0 2rem;
}
.stat {
  padding: 1.5rem 0;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  margin-bottom: 2rem;
}
.label {
  color: var(--muted-foreground);
}
.value {
  font-family: var(--font-display);
  font-size: 1.75rem;
  margin: 0.5rem 0 0;
}
.actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.banner {
  margin: 0 0 1.5rem;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-size: 0.9rem;
}
.banner.error {
  color: #8a1f1f;
  background: #fbf3f2;
  border: 1px solid #f0dcd8;
}
@media (max-width: 480px) {
  .card {
    padding: 1.75rem;
  }
  .title {
    font-size: 2rem;
  }
}
</style>
