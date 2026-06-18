<script setup>
import { ref } from 'vue'
import { useAuth } from './composables/useAuth'
import AuthView from './components/AuthView.vue'
import AccountView from './components/AccountView.vue'
import StompChat from './components/StompChat.vue'

const { isAuthed, logout } = useAuth()
const view = ref('account') // 'account' | 'chat' (only when authed)
</script>

<template>
  <AuthView v-if="!isAuthed" />

  <div v-else class="app">
    <header class="topbar">
      <span class="wordmark">Denden</span>
      <nav class="nav">
        <button :class="['link', { active: view === 'account' }]" @click="view = 'account'">帳號</button>
        <button :class="['link', { active: view === 'chat' }]" @click="view = 'chat'">聊天室</button>
      </nav>
      <button class="link logout" @click="logout">登出</button>
    </header>

    <main>
      <AccountView v-if="view === 'account'" @open-chat="view = 'chat'" />
      <StompChat v-else />
    </main>
  </div>
</template>

<style scoped>
.app {
  position: relative;
  z-index: 1;
}
.topbar {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  height: 5rem;
  padding: 0 1.5rem;
  border-bottom: 1px solid var(--border);
  background: rgba(250, 250, 248, 0.85);
  backdrop-filter: blur(6px);
}
.wordmark {
  font-family: var(--font-display);
  font-size: 1.35rem;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.nav {
  display: flex;
  gap: 1.5rem;
  margin-left: auto;
}
.link {
  background: none;
  border: none;
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.8rem;
  font-weight: 500;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted-foreground);
  padding: 0.5rem 0;
  transition: color 200ms ease-out;
  touch-action: manipulation;
}
.link:hover,
.link.active {
  color: var(--foreground);
}
.link.active {
  border-bottom: 1px solid var(--accent);
}
.logout {
  margin-left: 0.5rem;
}
.logout:hover {
  color: var(--accent);
}
</style>
