import { NavLink, useNavigate } from 'react-router-dom'
import { clearSession } from '../auth/session'

const linkClass = ({ isActive }) => `nav-link ${isActive ? 'active fw-semibold' : ''}`

export default function AppNavbar({ session, unreadCount = 0, onLogout }) {
  const navigate = useNavigate()
  const role = session?.user?.role || 'STUDENT'

  const logout = () => {
    clearSession()
    onLogout?.()
    navigate('/login', { replace: true })
  }

  return (
    <nav className="navbar navbar-expand-lg navbar-dark campus-nav sticky-top">
      <div className="container">
        <NavLink className="navbar-brand fw-bold" to="/">CampusSync</NavLink>
        <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#primary-nav" aria-controls="primary-nav" aria-expanded="false" aria-label="Toggle navigation">
          <span className="navbar-toggler-icon" />
        </button>
        <div className="collapse navbar-collapse" id="primary-nav">
          <div className="navbar-nav me-auto">
            <NavLink className={linkClass} to="/events">Events</NavLink>
            {String(role).toUpperCase().includes('STUDENT') && <NavLink className={linkClass} to="/my-registrations">My registrations</NavLink>}
            <NavLink className={linkClass} to="/notifications">
              Notifications {unreadCount > 0 && <span className="badge rounded-pill text-bg-warning ms-1">{unreadCount}</span>}
            </NavLink>
          </div>
          <div className="d-flex flex-column flex-lg-row align-items-lg-center gap-2 mt-3 mt-lg-0">
            <span className="badge text-bg-light text-uppercase align-self-start align-self-lg-auto">{role}</span>
            <span className="small text-white-50">{session?.user?.name || session?.user?.email}</span>
            <button className="btn btn-sm btn-outline-light" onClick={logout}>Log out</button>
          </div>
        </div>
      </div>
    </nav>
  )
}
