import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import Login from './Login'
import Composer from './Composer'
import CalendarView from './CalendarView'
import Analytics from './Analytics'
import Members from './Members'
import { logout } from './store/authSlice'
import { fetchWorkspaces, selectRole, setActive } from './store/workspaceSlice'
import { fetchPosts } from './store/postsSlice'

const TABS = ['compose', 'calendar', 'analytics', 'team']

export default function App() {
  const dispatch = useDispatch()
  const email = useSelector((s) => s.auth.email)
  const { list, activeId } = useSelector((s) => s.workspace)
  const role = useSelector(selectRole)
  const error = useSelector((s) => s.posts.error)
  const [tab, setTab] = useState('compose')

  useEffect(() => {
    const onExpired = () => dispatch(logout())
    window.addEventListener('auth-expired', onExpired)
    return () => window.removeEventListener('auth-expired', onExpired)
  }, [dispatch])

  useEffect(() => {
    if (email) dispatch(fetchWorkspaces())
  }, [email, dispatch])

  // poll so asynchronous changes (publishing, role changes by an admin) appear live
  useEffect(() => {
    if (!activeId) return
    dispatch(fetchPosts(activeId))
    const id = setInterval(() => {
      dispatch(fetchPosts(activeId))
      dispatch(fetchWorkspaces())
    }, 5000)
    return () => clearInterval(id)
  }, [activeId, dispatch])

  if (!email) return <Login />

  return (
    <div className="app">
      <header className="topbar">
        <h1>Post Scheduler</h1>
        <div className="topbar-right">
          <select value={activeId ?? ''} onChange={(e) => dispatch(setActive(Number(e.target.value)))}>
            {list.map((w) => <option key={w.id} value={w.id}>{w.name} ({w.role})</option>)}
          </select>
          {role && <span className={`role ${role}`}>{role}</span>}
          <span className="muted">{email}</span>
          <button className="btn-link" onClick={() => dispatch(logout())}>Log out</button>
        </div>
      </header>

      <nav className="tabs">
        {TABS.map((t) => (
          <button key={t} className={tab === t ? 'tab active' : 'tab'} onClick={() => setTab(t)}>
            {t[0].toUpperCase() + t.slice(1)}
          </button>
        ))}
      </nav>

      {error && <div className="alert err">{error}</div>}

      {tab === 'compose' && <Composer />}
      {tab === 'calendar' && <CalendarView />}
      {tab === 'analytics' && <Analytics />}
      {tab === 'team' && <Members />}
    </div>
  )
}