import { createSlice } from '@reduxjs/toolkit'
import { api } from '../api'
import { makeThunk } from './makeThunk'
import { loggedOut } from './authSlice'
import { setActive } from './workspaceSlice'

export const fetchAnalytics = makeThunk('analytics/fetch', (wid) => api.analytics(wid))

const initialState = { data: null, error: null }

const analyticsSlice = createSlice({
  name: 'analytics',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchAnalytics.fulfilled, (s, a) => { s.data = a.payload; s.error = null })
      .addCase(fetchAnalytics.rejected, (s, a) => { s.error = a.payload })
      .addCase(setActive, () => initialState)
      .addCase(loggedOut, () => initialState)
  },
})

export default analyticsSlice.reducer