import { ref, computed } from 'vue'

// Module-level singletons so every component shares one auth state.
const token = ref(localStorage.getItem('token') || '')
const email = ref(localStorage.getItem('email') || '')

export function useAuth() {
  const isAuthed = computed(() => !!token.value)

  function setSession(newToken, userEmail) {
    token.value = newToken
    email.value = userEmail
    localStorage.setItem('token', newToken)
    localStorage.setItem('email', userEmail)
  }

  function logout() {
    token.value = ''
    email.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('email')
  }

  return { token, email, isAuthed, setSession, logout }
}
