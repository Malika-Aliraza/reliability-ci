import { useState } from 'react'
import { api } from './api'

const PLATFORMS = ['TWITTER', 'LINKEDIN', 'INSTAGRAM', 'FACEBOOK']
const MAX = 280

const pad = (n) => String(n).padStart(2, '0')
const toLocalInput = (d) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`

export default function Composer({ posts, onChange }) {
  const [content, setContent] = useState('')
  const [platform, setPlatform] = useState('TWITTER')
  const [when, setWhen] = useState('')
  const [msg, setMsg] = useState(null)
  const [busy, setBusy] = useState(false)
  const [rowTimes, setRowTimes] = useState({})
  const minTime = toLocalInput(new Date(Date.now() + 60000))

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setMsg(null)
    try {
      await api.createPost({ content, platform, scheduledAt: when ? new Date(when).toISOString() : null })
      setMsg({ type: 'ok', text: when ? 'Post scheduled!' : 'Draft saved.' })
      setContent('')
      setWhen('')
      await onChange()
    } catch (err) {
      setMsg({ type: 'err', text: err.message })
    } finally {
      setBusy(false)
    }
  }

  async function schedule(id) {
    const value = rowTimes[id]
    if (!value) return setMsg({ type: 'err', text: 'Pick a date and time first' })
    try {
      await api.schedulePost(id, new Date(value).toISOString())
      setMsg({ type: 'ok', text: 'Post scheduled!' })
      await onChange()
    } catch (err) {
      setMsg({ type: 'err', text: err.message })
    }
  }

  async function remove(id) {
    try {
      await api.deletePost(id)
      await onChange()
    } catch (err) {
      setMsg({ type: 'err', text: err.message })
    }
  }

  return (
    <>
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

      <div className="card">
        <h2>Your posts</h2>
        {posts.length === 0 ? <p className="muted">No posts yet.</p> : (
          <table>
            <thead><tr><th>Content</th><th>Platform</th><th>Status</th><th>When</th><th></th></tr></thead>
            <tbody>
              {posts.map((p) => (
                <tr key={p.id}>
                  <td className="content-cell">{p.content}</td>
                  <td>{p.platform}</td>
                  <td><span className={`badge ${p.status}`}>{p.status}</span></td>
                  <td>
                    {p.status === 'DRAFT' ? (
                      <span className="inline">
                        <input type="datetime-local" min={minTime} value={rowTimes[p.id] || ''}
                          onChange={(e) => setRowTimes({ ...rowTimes, [p.id]: e.target.value })} />
                        <button className="btn small" type="button" onClick={() => schedule(p.id)}>Schedule</button>
                      </span>
                    ) : p.scheduledAt ? new Date(p.scheduledAt).toLocaleString() : '-'}
                  </td>
                  <td><button className="btn-link danger" onClick={() => remove(p.id)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}