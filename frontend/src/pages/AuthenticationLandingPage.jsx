import { Link, useLocation } from 'react-router-dom'

export default function AuthenticationLandingPage() {
  const location = useLocation()
  const loginState = location.state?.from ? { from: location.state.from } : undefined
  return (
    <main className="login-page">
      <section className="login-shell">
        <div className="login-hero" aria-label="CampusSync campus overview">
          <div className="login-hero__content">
            <p className="eyebrow">SPIT · Campus activity platform</p>
            <h1>Welcome to CampusSync</h1>
            <p className="lead">
              A unified space for student life, committee coordination, faculty workflows and campus operations.
            </p>
            <div className="login-hero__stats">
              <div>
                <strong>4</strong>
                <span>Role access</span>
              </div>
              <div>
                <strong>24/7</strong>
                <span>Academic updates</span>
              </div>
              <div>
                <strong>1</strong>
                <span>Campus portal</span>
              </div>
            </div>
          </div>
        </div>

        <div className="card shadow-sm border-0 login-card mx-auto w-100">
          <div className="card-body p-4 p-md-5">
            <p className="text-primary fw-semibold mb-2">Access portal</p>
            <h2 className="h2 mb-2">Choose your sign-in</h2>
            <p className="text-secondary mb-4">Select the role that matches your CampusSync account.</p>
            <div className="d-grid gap-3">
              <Link className="btn btn-primary btn-lg" to="/login/student" state={loginState}>Continue as Student</Link>
              <Link className="btn btn-outline-primary btn-lg" to="/login/faculty" state={loginState}>Continue as Faculty</Link>
              <Link className="btn btn-outline-primary btn-lg" to="/login/coordinator" state={loginState}>Continue as Committee Coordinator</Link>
              <Link className="btn btn-outline-primary btn-lg" to="/login/administrator" state={loginState}>Continue as Administrator</Link>
            </div>
            <p className="text-secondary text-center mt-4 mb-0">
              New student? <Link to="/register">Create Account</Link>
            </p>
          </div>
        </div>
      </section>
    </main>
  )
}
