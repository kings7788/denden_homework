<script setup>
import { ref, computed } from 'vue'
import { api } from '../api'
import { useAuth } from '../composables/useAuth'
import AppCard from './ui/AppCard.vue'
import AppInput from './ui/AppInput.vue'
import AppButton from './ui/AppButton.vue'
import SectionLabel from './ui/SectionLabel.vue'

const { setSession } = useAuth()

const mode = ref('login') // 'login' | 'register'
const loginStep = ref('credentials') // 'credentials' | 'otp'

const email = ref('')
const password = ref('')
const otp = ref('')
const challengeId = ref('')

const loading = ref(false)
const error = ref('')
const info = ref('')
const registered = ref(false)

const title = computed(() => {
  if (mode.value === 'register') return registered.value ? '查收信箱' : '建立帳號'
  return loginStep.value === 'otp' ? '驗證碼' : '歡迎回來'
})
const subtitle = computed(() => {
  if (mode.value === 'register') {
    return registered.value
      ? '我們已寄出開通連結。'
      : '以 Email 註冊,我們會寄送開通信。'
  }
  return loginStep.value === 'otp'
    ? '請輸入寄到你信箱的 6 位數驗證碼。'
    : '輸入帳號密碼開始登入。'
})

function reset(keepMode = true) {
  error.value = ''
  info.value = ''
  if (!keepMode) {
    password.value = ''
    otp.value = ''
    challengeId.value = ''
    loginStep.value = 'credentials'
    registered.value = false
  }
}

function switchMode(next) {
  mode.value = next
  reset(false)
}

async function submitRegister() {
  reset()
  loading.value = true
  try {
    await api.register(email.value, password.value)
    registered.value = true
    info.value = '開通連結已寄出。本機開發請查看後端 console 的 [EMAIL] 紀錄。'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function submitCredentials() {
  reset()
  loading.value = true
  try {
    const res = await api.login(email.value, password.value)
    challengeId.value = res.challenge_id
    loginStep.value = 'otp'
    info.value = '驗證碼已寄出。本機開發請查看後端 console 的 [EMAIL] 紀錄。'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function submitOtp() {
  reset()
  loading.value = true
  try {
    const res = await api.loginVerify(challengeId.value, otp.value)
    setSession(res.access_token, email.value) // App switches to the authed view
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-inner">
      <p class="wordmark">Denden</p>

      <AppCard accent-top class="auth-card">
        <SectionLabel>Members</SectionLabel>
        <h1 class="title">{{ title }}</h1>
        <p class="subtitle">{{ subtitle }}</p>

        <p v-if="error" class="banner error" role="alert">{{ error }}</p>
        <p v-else-if="info" class="banner info">{{ info }}</p>

        <!-- REGISTER -->
        <form v-if="mode === 'register' && !registered" class="form" @submit.prevent="submitRegister">
          <AppInput id="reg-email" v-model="email" label="Email" type="email"
            placeholder="you@example.com" autocomplete="email" />
          <div>
            <AppInput id="reg-pw" v-model="password" label="Password" type="password"
              placeholder="至少 8 碼" autocomplete="new-password" />
            <p class="hint">密碼至少 8 碼。</p>
          </div>
          <AppButton type="submit" variant="primary" block :disabled="loading">
            {{ loading ? '處理中…' : '註冊' }}
          </AppButton>
        </form>

        <!-- REGISTER SUCCESS -->
        <div v-else-if="mode === 'register' && registered" class="form">
          <AppButton variant="primary" block @click="switchMode('login')">前往登入</AppButton>
        </div>

        <!-- LOGIN — credentials -->
        <form v-else-if="loginStep === 'credentials'" class="form" @submit.prevent="submitCredentials">
          <AppInput id="login-email" v-model="email" label="Email" type="email"
            placeholder="you@example.com" autocomplete="email" />
          <AppInput id="login-pw" v-model="password" label="Password" type="password"
            placeholder="••••••••" autocomplete="current-password" />
          <AppButton type="submit" variant="primary" block :disabled="loading">
            {{ loading ? '處理中…' : '登入' }}
          </AppButton>
        </form>

        <!-- LOGIN — OTP -->
        <form v-else class="form" @submit.prevent="submitOtp">
          <AppInput id="login-otp" v-model="otp" label="Verification code"
            placeholder="000000" inputmode="numeric" maxlength="6" autocomplete="one-time-code" />
          <AppButton type="submit" variant="primary" block :disabled="loading">
            {{ loading ? '驗證中…' : '驗證並登入' }}
          </AppButton>
          <div class="center">
            <AppButton variant="ghost" @click="loginStep = 'credentials'">返回</AppButton>
          </div>
        </form>
      </AppCard>

      <p class="footer">
        <template v-if="mode === 'login'">
          還沒有帳號?
          <AppButton variant="ghost" @click="switchMode('register')">建立帳號</AppButton>
        </template>
        <template v-else>
          已經有帳號?
          <AppButton variant="ghost" @click="switchMode('login')">前往登入</AppButton>
        </template>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.25rem;
}
.auth-inner {
  width: 100%;
  max-width: 27rem;
}
.wordmark {
  font-family: var(--font-display);
  font-size: 1.75rem;
  font-weight: 600;
  letter-spacing: -0.01em;
  text-align: center;
  margin: 0 0 2rem;
}
.auth-card {
  padding: 2.5rem;
}
.title {
  font-size: 2.25rem;
  line-height: 1.15;
  letter-spacing: -0.01em;
  margin: 1.25rem 0 0.5rem;
}
.subtitle {
  color: var(--muted-foreground);
  margin: 0 0 1.75rem;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.hint {
  margin: 0.5rem 0 0;
  font-size: 0.8rem;
  color: var(--muted-foreground);
}
.banner {
  margin: 0 0 1.5rem;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-size: 0.9rem;
  border: 1px solid var(--border);
}
.banner.error {
  color: #8a1f1f;
  background: #fbf3f2;
  border-color: #f0dcd8;
}
.banner.info {
  color: var(--accent);
  background: rgba(184, 134, 11, 0.06);
  border-color: rgba(184, 134, 11, 0.2);
}
.center {
  text-align: center;
}
.footer {
  margin-top: 1.75rem;
  text-align: center;
  color: var(--muted-foreground);
  font-size: 0.95rem;
}
@media (max-width: 480px) {
  .auth-card {
    padding: 1.75rem;
  }
  .title {
    font-size: 2rem;
  }
}
</style>
