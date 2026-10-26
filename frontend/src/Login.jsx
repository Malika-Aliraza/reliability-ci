import { useState } from 'react'
import { api, setToken } from './api'

export default function Login({ onLogin }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const res = mode === 'login' ? await api.login(email, password) : await api.register(email, password)
      setToken(res.token)
      localStorage.setItem('email', res.email)
      onLogin(res.email)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <form className="card login" onSubmit={submit}>
        <h1>Post Scheduler</h1>
        <p className="muted">{mode === 'login' ? 'Sign in to your account' : 'Create an account'}</p>
        {error && <div className="alert err">{error}</div>}
        <label>Email</label>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label>Password</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <button className="btn" disabled={busy}>
          {busy ? 'Please wait...' : mode === 'login' ? 'Log in' : 'Register'}
        </button>
        <button type="button" className="btn-link"
          onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError('') }}>
          {mode === 'login' ? "No account? Register" : 'Have an account? Log in'}
        </button>
      </form>
    </div>
  )
}