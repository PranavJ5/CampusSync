import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { dashboardPath, saveSession } from '../auth/session'

export default function LoginPage({ onAuthenticated }) {
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
        identifier: form.identifier,
        email: form.identifier,
        password: form.password,
      })
      const session = saveSession(response.data, form.remember)
      onAuthenticated(session)
      navigate(location.state?.from?.pathname || dashboardPath(session.user.role), { replace: true })
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to sign in. Check your details and try again.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page d-flex align-items-center py-4">
      <section className="card shadow-sm border-0 login-card mx-auto w-100">
        <div className="card-body p-4 p-md-5">
          <p className="text-primary fw-semibold mb-2">SPIT · Campus activity platform</p>
          <h1 className="h2 mb-2">Welcome to CampusSync</h1>
          <p className="text-secondary mb-4">Sign in with your email address or enrolment number.</p>
          {error && <div className="alert alert-danger" role="alert">{error}</div>}
          <form onSubmit={submit} noValidate>
            <div className="mb-3">
              <label className="form-label" htmlFor="identifier">Email or enrolment number</label>
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
        </div>
      </section>
    </main>
  )
}
