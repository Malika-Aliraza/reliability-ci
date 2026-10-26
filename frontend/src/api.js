const BASE = import.meta.env.VITE_API_URL ?? ''
const TOKEN_KEY = 'token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (t) => localStorage.setItem(TOKEN_KEY, t)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(`${BASE}/api${path}`, { ...options, headers })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?')
  }

  if (res.status === 204) return null
  let data = null
  try { data = await res.json() } catch { /* empty body */ }

  if (!res.ok) {
    if (res.status === 401 && token) {
      clearToken()
      window.dispatchEvent(new Event('auth-expired'))
    }
    throw new ApiError(res.status, data?.message || `Request failed (${res.status})`)
  }
  return data
}

export const api = {
  register: (email, password) => request('/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  login: (email, password) => request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  listPosts: () => request('/posts'),
  createPost: (post) => request('/posts', { method: 'POST', body: JSON.stringify(post) }),
  schedulePost: (id, scheduledAt) => request(`/posts/${id}/schedule`, { method: 'PUT', body: JSON.stringify({ scheduledAt }) }),
  deletePost: (id) => request(`/posts/${id}`, { method: 'DELETE' }),
  analytics: () => request('/analytics'),
}