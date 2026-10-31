import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { dashboardPath, saveSession } from '../auth/session'

export default function LoginPage({ onAuthenticated, selectedRole }) {
  const roleLabel = {
    STUDENT: 'Student',
    FACULTY: 'Faculty',
    COMMITTEE_COORDINATOR: 'Committee Coordinator',
    ADMINISTRATOR: 'Administrator',
  }[selectedRole] || 'Account'
  const [form, setForm] = useState({ identifier: '', password: '', remember: true })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const response = await api.post('/auth/login', {
        email: form.identifier,
        password: form.password,
        selectedRole,
      })
      if (response.data?.user?.role !== selectedRole) {
        throw new Error('The authenticated account does not match the selected sign-in option.')
      }
      const session = saveSession(response.data, form.remember)
      onAuthenticated(session)
      const destination = location.state?.from
        ? `${location.state.from.pathname || ''}${location.state.from.search || ''}${location.state.from.hash || ''}`
        : dashboardPath(session.user.role)
      navigate(destination || dashboardPath(session.user.role), { replace: true })
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to sign in. Check your details and try again.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-shell login-shell--compact">
        <div className="login-hero login-hero--compact" aria-label="CampusSync sign-in overview">
          <div className="login-hero__content">
            <p className="eyebrow">CampusSync · {roleLabel}</p>
            <h1>Secure access</h1>
            <p className="lead">Your personalized campus dashboard is waiting with your schedule, events and alerts.</p>
          </div>
        </div>

        <div className="card shadow-sm border-0 login-card mx-auto w-100">
          <div className="card-body p-4 p-md-5">
            <p className="text-primary fw-semibold mb-2">{roleLabel} sign in</p>
            <h2 className="h2 mb-2">Welcome back</h2>
            <p className="text-secondary mb-4">Sign in with your email address.</p>
            {location.state?.notice && <div className="alert alert-success" role="status">{location.state.notice}</div>}
            {error && <div className="alert alert-danger" role="alert">{error}</div>}
            <form onSubmit={submit} noValidate>
              <div className="mb-3">
                <label className="form-label" htmlFor="identifier">Email address</label>
                <input className="form-control" id="identifier" autoComplete="username" value={form.identifier} onChange={(e) => setForm({ ...form, identifier: e.target.value })} required />
              </div>
              <div className="mb-3">
                <label className="form-label" htmlFor="password">Password</label>
                <input className="form-control" id="password" type="password" autoComplete="current-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
              </div>
              <div className="form-check mb-4">
                <input className="form-check-input" id="remember" type="checkbox" checked={form.remember} onChange={(e) => setForm({ ...form, remember: e.target.checked })} />
                <label className="form-check-label" htmlFor="remember">Keep me signed in on this device</label>
              </div>
              <button className="btn btn-primary w-100" disabled={loading}>
                {loading ? <><span className="spinner-border spinner-border-sm me-2" />Signing in...</> : 'Sign in'}
              </button>
            </form>
            <p className="text-secondary text-center mt-4 mb-0">
              {selectedRole === 'STUDENT' ? <>New student? <Link to="/register">Create Account</Link></> : <Link to="/login">Choose another sign-in option</Link>}
            </p>
          </div>
        </div>
      </section>
    </main>
  )
}
