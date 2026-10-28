import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { authenticate } from './store/authSlice'

export default function Login() {
  const dispatch = useDispatch()
  const { loading, error } = useSelector((s) => s.auth)
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function submit(e) {
    e.preventDefault()
    dispatch(authenticate({ mode, email, password }))
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
        <button className="btn" disabled={loading}>
          {loading ? 'Please wait...' : mode === 'login' ? 'Log in' : 'Register'}
        </button>
        <button type="button" className="btn-link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? 'No account? Register' : 'Have an account? Log in'}
        </button>
      </form>
    </div>
  )
}