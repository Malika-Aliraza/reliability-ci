import { createSlice } from '@reduxjs/toolkit'
import { api } from '../api'
import { makeThunk } from './makeThunk'
import { loggedOut } from './authSlice'
import { setActive } from './workspaceSlice'

export const fetchPosts = makeThunk('posts/fetch', (wid) => api.listPosts(wid))

export const createPost = makeThunk('posts/create', async ({ wid, post }, dispatch) => {
  await api.createPost(wid, post)
  await dispatch(fetchPosts(wid))
})

export const schedulePost = makeThunk('posts/schedule', async ({ wid, id, at }, dispatch) => {
  await api.schedulePost(wid, id, at)
  await dispatch(fetchPosts(wid))
})

export const deletePost = makeThunk('posts/delete', async ({ wid, id }, dispatch) => {
  await api.deletePost(wid, id)
  await dispatch(fetchPosts(wid))
})

const initialState = { items: [], error: null }

const postsSlice = createSlice({
  name: 'posts',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchPosts.fulfilled, (s, a) => { s.items = a.payload; s.error = null })
      .addCase(fetchPosts.rejected, (s, a) => { s.error = a.payload })
      .addCase(setActive, () => initialState)
      .addCase(loggedOut, () => initialState)
  },
})

export default postsSlice.reducer