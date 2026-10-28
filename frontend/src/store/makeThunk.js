import { createAsyncThunk } from '@reduxjs/toolkit'
import { errorMessage } from '../api'

export const makeThunk = (type, fn) =>
  createAsyncThunk(type, async (arg, { dispatch, rejectWithValue }) => {
    try {
      return await fn(arg, dispatch)
    } catch (e) {
      return rejectWithValue(errorMessage(e))
    }
  })