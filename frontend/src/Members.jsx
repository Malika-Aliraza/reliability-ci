import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { errorMessage } from './api'
import { addMember, changeRole, createWorkspace, fetchMembers, removeMember, selectRole } from './store/workspaceSlice'

const ROLES = ['VIEWER', 'EDITOR', 'ADMIN']

export default function Members() {
  const dispatch = useDispatch()
  const { activeId, members } = useSelector((s) => s.workspace)
  const role = useSelector(selectRole)
  const me = useSelector((s) => s.auth.email)
  const isAdmin = role === 'ADMIN'

  const [email, setEmail] = useState('')
  const [newRole, setNewRole] = useState('EDITOR')
  const [wsName, setWsName] = useState('')
  const [msg, setMsg] = useState(null)

  useEffect(() => {
    if (activeId) dispatch(fetchMembers(activeId))
  }, [activeId, dispatch])

  async function run(action, okText) {
    try {
      await dispatch(action).unwrap()
      setMsg({ type: 'ok', text: okText })
      return true
    } catch (e) {
      setMsg({ type: 'err', text: errorMessage(e) })
      return false
    }
  }

  async function submitMember(e) {
    e.preventDefault()
    if (await run(addMember({ wid: activeId, email, role: newRole }), 'Member added.')) setEmail('')
  }

  async function submitWorkspace(e) {
    e.preventDefault()
    if (await run(createWorkspace(wsName), 'Workspace created.')) setWsName('')
  }

  return (
    <>
      {msg && <div className={`alert ${msg.type}`}>{msg.text}</div>}

      <div className="card">
        <h2>Team members</h2>
        <table>
          <thead><tr><th>Email</th><th>Role</th><th></th></tr></thead>
          <tbody>
            {members.map((m) => (
              <tr key={m.userId}>
                <td>{m.email}{m.email === me && ' (you)'}</td>
                <td>
                  {isAdmin ? (
                    <select value={m.role}
                      onChange={(e) => run(changeRole({ wid: activeId, userId: m.userId, role: e.target.value }), 'Role updated.')}>
                      {ROLES.map((r) => <option key={r}>{r}</option>)}
                    </select>
                  ) : <span className={`role ${m.role}`}>{m.role}</span>}
                </td>
                <td>
                  {isAdmin && (
                    <button className="btn-link danger"
                      onClick={() => run(removeMember({ wid: activeId, userId: m.userId }), 'Member removed.')}>
                      Remove
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {isAdmin ? (
          <form className="row" onSubmit={submitMember}>
            <input type="email" placeholder="Registered user's email" value={email}
              onChange={(e) => setEmail(e.target.value)} required />
            <select value={newRole} onChange={(e) => setNewRole(e.target.value)}>
              {ROLES.map((r) => <option key={r}>{r}</option>)}
            </select>
            <button className="btn">Add member</button>
          </form>
        ) : (
          <p className="muted small">Only admins can manage members.</p>
        )}
      </div>

      <div className="card">
        <h2>Create a new workspace</h2>
        <form className="row" onSubmit={submitWorkspace}>
          <input placeholder="e.g. Marketing team" value={wsName} onChange={(e) => setWsName(e.target.value)} required />
          <button className="btn">Create</button>
        </form>
        <p className="muted small">You become the admin of any workspace you create.</p>
      </div>
    </>
  )
}