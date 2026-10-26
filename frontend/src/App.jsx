import { useCallback, useEffect, useState } from 'react'
import { api, clearToken, getToken } from './api'
import Login from './Login'
import Composer from './Composer'
import CalendarView from './CalendarView'
import Analytics from './Analytics'

export default function App() {
  const [email, setEmail] = useState(getToken() ? localStorage.getItem('email') : null)
  const [tab, setTab] = useState('compose')
  const [posts, setPosts] = useState([])
  const [error, setError] = useState('')

  const logout = useCallback(() => {
    clearToken()
    localStorage.removeItem('email')
    setEmail(null)
    setPosts([])
  }, [])

  const refresh = useCallback(async () => {
    try {
      setPosts(await api.listPosts())
      setError('')
    } catch (e) {
      setError(e.message)
    }
  }, [])

  useEffect(() => {
    window.addEventListener('auth-expired', logout)
    return () => window.removeEventListener('auth-expired', logout)
  }, [logout])

  // load posts, then poll so status changes (QUEUED -> PUBLISHED) show up live
  useEffect(() => {
    if (!email) return
    refresh()
    const id = setInterval(refresh, 5000)
    return () => clearInterval(id)
  }, [email, refresh])

  if (!email) return <Login onLogin={setEmail} />

  return (
    <div className="app">
      <header className="topbar">
        <h1>Post Scheduler</h1>
        <div>
          <span className="muted">{email}</span>
          <button className="btn-link" onClick={logout}>Log out</button>
        </div>
      </header>

      <nav className="tabs">
        {['compose', 'calendar', 'analytics'].map((t) => (
          <button key={t} className={tab === t ? 'tab active' : 'tab'} onClick={() => setTab(t)}>
            {t[0].toUpperCase() + t.slice(1)}
          </button>
        ))}
      </nav>

      {error && <div className="alert err">{error}</div>}

      {tab === 'compose' && <Composer posts={posts} onChange={refresh} />}
      {tab === 'calendar' && <CalendarView posts={posts} />}
      {tab === 'analytics' && <Analytics posts={posts} />}
    </div>
  )
}