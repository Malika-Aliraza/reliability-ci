import { createSlice } from '@reduxjs/toolkit'
import { api } from '../api'
import { makeThunk } from './makeThunk'
import { loggedOut } from './authSlice'

export const fetchWorkspaces = makeThunk('workspace/fetchAll', () => api.listWorkspaces())

export const createWorkspace = makeThunk('workspace/create', async (name, dispatch) => {
  const created = await api.createWorkspace(name)
  await dispatch(fetchWorkspaces())
  dispatch(setActive(created.id))
})

export const fetchMembers = makeThunk('workspace/members', (wid) => api.listMembers(wid))

export const addMember = makeThunk('workspace/addMember', async ({ wid, email, role }, dispatch) => {
  await api.addMember(wid, email, role)
  await dispatch(fetchMembers(wid))
})

export const changeRole = makeThunk('workspace/changeRole', async ({ wid, userId, role }, dispatch) => {
  await api.changeRole(wid, userId, role)
  await dispatch(fetchMembers(wid))
})

export const removeMember = makeThunk('workspace/removeMember', async ({ wid, userId }, dispatch) => {
  await api.removeMember(wid, userId)
  await dispatch(fetchMembers(wid))
})

const initialState = { list: [], activeId: null, members: [], error: null }

const workspaceSlice = createSlice({
  name: 'workspace',
  initialState,
  reducers: {
    setActive: (state, action) => { state.activeId = action.payload; state.members = [] },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchWorkspaces.fulfilled, (s, a) => {
        s.list = a.payload
        s.error = null
        if (!a.payload.some((w) => w.id === s.activeId)) s.activeId = a.payload[0]?.id ?? null
      })
      .addCase(fetchWorkspaces.rejected, (s, a) => { s.error = a.payload })
      .addCase(fetchMembers.fulfilled, (s, a) => { s.members = a.payload })
      .addCase(loggedOut, () => initialState)
  },
})

export const { setActive } = workspaceSlice.actions
export const selectRole = (s) => s.workspace.list.find((w) => w.id === s.workspace.activeId)?.role
export default workspaceSlice.reducer