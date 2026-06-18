// Thin fetch wrapper for the backend REST API.
// In dev, paths are relative ("/api/...") and Vite proxies them to :8080.
const BASE = import.meta.env.VITE_API_BASE || ''

async function request(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    // Backend returns { error } or { errors: { field: msg } }.
    const msg =
      data.error ||
      (data.errors && Object.values(data.errors)[0]) ||
      `請求失敗 (${res.status})`
    throw new Error(msg)
  }
  return data
}

export const api = {
  register: (email, password) =>
    request('/api/auth/register', { method: 'POST', body: { email, password } }),
  login: (email, password) =>
    request('/api/auth/login', { method: 'POST', body: { email, password } }),
  loginVerify: (challengeId, otp) =>
    request('/api/auth/login/verify', { method: 'POST', body: { challenge_id: challengeId, otp } }),
  lastLogin: (token) => request('/api/users/me/last-login', { token }),
}
