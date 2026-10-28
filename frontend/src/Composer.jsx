import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { errorMessage } from './api'
import { createPost, deletePost, schedulePost } from './store/postsSlice'
import { selectRole } from './store/workspaceSlice'

const PLATFORMS = ['TWITTER', 'LINKEDIN', 'INSTAGRAM', 'FACEBOOK']
const MAX = 280

const pad = (n) => String(n).padStart(2, '0')
const toLocalInput = (d) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`

export default function Composer() {
  const dispatch = useDispatch()
  const wid = useSelector((s) => s.workspace.activeId)
  const posts = useSelector((s) => s.posts.items)
  const role = useSelector(selectRole)
  const me = useSelector((s) => s.auth.email)
  const canEdit = role === 'EDITOR' || role === 'ADMIN'

  const [content, setContent] = useState('')
  const [platform, setPlatform] = useState('TWITTER')
  const [when, setWhen] = useState('')
  const [msg, setMsg] = useState(null)
  const [busy, setBusy] = useState(false)
  const [rowTimes, setRowTimes] = useState({})
  const minTime = toLocalInput(new Date(Date.now() + 60000))

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

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setMsg(null)
    const post = { content, platform, scheduledAt: when ? new Date(when).toISOString() : null }
    if (await run(createPost({ wid, post }), when ? 'Post scheduled!' : 'Draft saved.')) {
      setContent('')
      setWhen('')
    }
    setBusy(false)
  }

  function schedule(id) {
    const value = rowTimes[id]
    if (!value) return setMsg({ type: 'err', text: 'Pick a date and time first' })
    run(schedulePost({ wid, id, at: new Date(value).toISOString() }), 'Post scheduled!')
  }

  const canDelete = (p) => role === 'ADMIN' || (canEdit && p.authorEmail === me)

  return (
    <>
      {canEdit ? (
        <form className="card" onSubmit={submit}>
          <h2>New post</h2>
          {msg && <div className={`alert ${msg.type}`}>{msg.text}</div>}
          <textarea rows="4" placeholder="What do you want to share?" value={content}
            onChange={(e) => setContent(e.target.value)} />
          <div className={content.length > MAX ? 'counter over' : 'counter'}>{content.length} / {MAX}</div>
          <div className="row">
            <select value={platform} onChange={(e) => setPlatform(e.target.value)}>
              {PLATFORMS.map((p) => <option key={p}>{p}</option>)}
            </select>
            <input type="datetime-local" min={minTime} value={when} onChange={(e) => setWhen(e.target.value)} />
            <button className="btn" disabled={busy || !content.trim() || content.length > MAX}>
              {when ? 'Schedule post' : 'Save draft'}
            </button>
          </div>
          <p className="muted small">Leave the date empty to save a draft. Include the word FAIL to test the retry and DLQ flow.</p>
        </form>
      ) : (
        <div className="alert info">You have view-only access to this workspace. Ask an admin for the EDITOR role to create posts.</div>
      )}

      <div className="card">
        <h2>Posts</h2>
        {!canEdit && msg && <div className={`alert ${msg.type}`}>{msg.text}</div>}
        {posts.length === 0 ? <p className="muted">No posts yet.</p> : (
          <table>
            <thead><tr><th>Content</th><th>Author</th><th>Platform</th><th>Status</th><th>When</th><th></th></tr></thead>
            <tbody>
              {posts.map((p) => (
                <tr key={p.id}>
                  <td className="content-cell">{p.content}</td>
                  <td className="muted small">{p.authorEmail}</td>
                  <td>{p.platform}</td>
                  <td><span className={`badge ${p.status}`}>{p.status}</span></td>
                  <td>
                    {p.status === 'DRAFT' && canEdit ? (
                      <span className="inline">
                        <input type="datetime-local" min={minTime} value={rowTimes[p.id] || ''}
                          onChange={(e) => setRowTimes({ ...rowTimes, [p.id]: e.target.value })} />
                        <button className="btn small" type="button" onClick={() => schedule(p.id)}>Schedule</button>
                      </span>
                    ) : p.scheduledAt ? new Date(p.scheduledAt).toLocaleString() : '-'}
                  </td>
                  <td>
                    {canDelete(p) && (
                      <button className="btn-link danger" onClick={() => run(deletePost({ wid, id: p.id }), 'Post deleted.')}>
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}