import { createSlice } from '@reduxjs/toolkit'
import { api, clearToken, getToken, setToken } from '../api'
import { makeThunk } from './makeThunk'

export const authenticate = makeThunk('auth/authenticate', async ({ mode, email, password }) => {
  const res = mode === 'login' ? await api.login(email, password) : await api.register(email, password)
  setToken(res.token)
  localStorage.setItem('email', res.email)
  return res.email
})

const authSlice = createSlice({
  name: 'auth',
  initialState: { email: getToken() ? localStorage.getItem('email') : null, loading: false, error: null },
  reducers: {
    loggedOut: (state) => { state.email = null; state.error = null },
  },
  extraReducers: (builder) => {
    builder
      .addCase(authenticate.pending, (s) => { s.loading = true; s.error = null })
      .addCase(authenticate.fulfilled, (s, a) => { s.loading = false; s.email = a.payload })
      .addCase(authenticate.rejected, (s, a) => { s.loading = false; s.error = a.payload })
  },
})

export const { loggedOut } = authSlice.actions

export const logout = () => (dispatch) => {
  clearToken()
  localStorage.removeItem('email')
  dispatch(loggedOut())
}

export default authSlice.reducer