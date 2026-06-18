<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

// Dev: '/ws-stomp' (Vite proxies to :8080). Prod: full Cloud Run URL via VITE_WS_URL.
const wsUrl = import.meta.env.VITE_WS_URL || '/ws-stomp'

const messages = ref([])
const draft = ref('')
const connected = ref(false)
const sender = '使用者-' + Math.floor(Math.random() * 1000)
let client

onMounted(() => {
  client = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 3000, // auto-reconnect if the connection drops
    onConnect: () => {
      connected.value = true
      // Subscribe to the broadcast channel.
      client.subscribe('/topic/public', (frame) => {
        messages.value.push(JSON.parse(frame.body))
      })
    },
    onWebSocketClose: () => {
      connected.value = false
    },
  })
  client.activate()
})

onUnmounted(() => client?.deactivate()) // close connection when component unmounts

function send() {
  if (!draft.value || !connected.value) return
  client.publish({
    destination: '/app/chat',
    body: JSON.stringify({ sender, content: draft.value }),
  })
  draft.value = ''
}
</script>

<template>
  <div class="chat">
    <h2>STOMP 聊天室 (Vue)</h2>
    <p class="status">{{ connected ? '🟢 已連線' : '🔴 連線中…' }} · 我是 <b>{{ sender }}</b></p>
    <div class="log">
      <div v-for="(m, i) in messages" :key="i" class="line">
        <b>{{ m.sender }}</b>: {{ m.content }}
      </div>
    </div>
    <div class="row">
      <input v-model="draft" @keydown.enter="send" placeholder="輸入訊息後按 Enter" />
      <button @click="send" :disabled="!connected">送出</button>
    </div>
  </div>
</template>

<style scoped>
.chat { max-width: 600px; margin: 40px auto; font-family: sans-serif; }
.status { color: #666; }
.log { border: 1px solid #ccc; border-radius: 6px; height: 300px; overflow-y: auto; padding: 10px; margin: 12px 0; }
.line { padding: 2px 0; }
.row { display: flex; gap: 8px; }
.row input { flex: 1; padding: 8px; }
.row button { padding: 8px 16px; }
</style>
