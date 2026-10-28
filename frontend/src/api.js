const BASE = import.meta.env.VITE_API_URL ?? ''
const TOKEN_KEY = 'token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (t) => localStorage.setItem(TOKEN_KEY, t)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)
export const errorMessage = (e) => (typeof e === 'string' ? e : e?.message || 'Something went wrong')

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

const ws = (wid) => `/workspaces/${wid}`
const send = (method, body) => ({ method, body: JSON.stringify(body) })

export const api = {
  register: (email, password) => request('/auth/register', send('POST', { email, password })),
  login: (email, password) => request('/auth/login', send('POST', { email, password })),

  listWorkspaces: () => request('/workspaces'),
  createWorkspace: (name) => request('/workspaces', send('POST', { name })),
  listMembers: (wid) => request(`${ws(wid)}/members`),
  addMember: (wid, email, role) => request(`${ws(wid)}/members`, send('POST', { email, role })),
  changeRole: (wid, userId, role) => request(`${ws(wid)}/members/${userId}`, send('PUT', { role })),
  removeMember: (wid, userId) => request(`${ws(wid)}/members/${userId}`, { method: 'DELETE' }),

  listPosts: (wid) => request(`${ws(wid)}/posts`),
  createPost: (wid, post) => request(`${ws(wid)}/posts`, send('POST', post)),
  schedulePost: (wid, id, scheduledAt) => request(`${ws(wid)}/posts/${id}/schedule`, send('PUT', { scheduledAt })),
  deletePost: (wid, id) => request(`${ws(wid)}/posts/${id}`, { method: 'DELETE' }),

  analytics: (wid) => request(`${ws(wid)}/analytics`),
}