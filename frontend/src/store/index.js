import { configureStore } from '@reduxjs/toolkit'
import auth from './authSlice'
import workspace from './workspaceSlice'
import posts from './postsSlice'
import analytics from './analyticsSlice'

export const store = configureStore({ reducer: { auth, workspace, posts, analytics } })