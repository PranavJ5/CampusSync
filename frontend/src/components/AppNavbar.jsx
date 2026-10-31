import { NavLink, useNavigate } from 'react-router-dom'
import { clearSession } from '../auth/session'

const linkClass = ({ isActive }) => `nav-link ${isActive ? 'active fw-semibold' : ''}`

export default function AppNavbar({ session, unreadCount = 0, onLogout }) {
  const navigate = useNavigate()
  const role = session?.user?.role || 'Unknown role'

  const logout = () => {
    clearSession()
    onLogout?.()
    navigate('/login', { replace: true })
  }

  return (
    <nav className="navbar navbar-expand-lg navbar-dark campus-nav sticky-top">
      <div className="container campus-nav__inner">
        <NavLink className="navbar-brand fw-bold" to="/">
          <span className="campus-brand-mark">C</span>
          <span>CampusSync</span>
        </NavLink>
        <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#primary-nav" aria-controls="primary-nav" aria-expanded="false" aria-label="Toggle navigation">
          <span className="navbar-toggler-icon" />
        </button>
        <div className="collapse navbar-collapse" id="primary-nav">
          <div className="navbar-nav me-auto campus-nav__links">
            <NavLink className={linkClass} to="/events">Events</NavLink>
            {String(role).toUpperCase().includes('ADMINISTRATOR') && <NavLink className={linkClass} to="/admin/events">Manage events</NavLink>}
            {String(role).toUpperCase().includes('ADMINISTRATOR') && <NavLink className={linkClass} to="/admin/committees">Manage committees</NavLink>}
            {String(role).toUpperCase().includes('ADMINISTRATOR') && <NavLink className={linkClass} to="/admin/attendance">Attendance reports</NavLink>}
            {String(role).toUpperCase().includes('ADMINISTRATOR') && <NavLink className={linkClass} to="/admin/faculty">Provision faculty</NavLink>}
            {String(role).toUpperCase().includes('FACULTY') && <NavLink className={linkClass} to="/faculty">Dashboard</NavLink>}
            {String(role).toUpperCase().includes('FACULTY') && <NavLink className={linkClass} to="/faculty/attendance">Attendance</NavLink>}
            {String(role).toUpperCase().includes('STUDENT') && <NavLink className={linkClass} to="/my-registrations">My registrations</NavLink>}
            {['COMMITTEE_COORDINATOR', 'FACULTY', 'STUDENT'].includes(String(role).toUpperCase()) && <NavLink className={linkClass} to="/coordinator/events">Assigned committee</NavLink>}
            <NavLink className={linkClass} to="/notifications">
              Notifications {unreadCount > 0 && <span className="badge rounded-pill text-bg-warning ms-1">{unreadCount}</span>}
            </NavLink>
          </div>
          <div className="d-flex flex-column flex-lg-row align-items-lg-center gap-2 mt-3 mt-lg-0 campus-user-menu">
            <span className="badge campus-role-pill align-self-start align-self-lg-auto">{role}</span>
            <span className="campus-user-name">{session?.user?.name || session?.user?.email}</span>
            <button className="btn btn-sm btn-outline-light campus-logout" onClick={logout}>Log out</button>
          </div>
        </div>
      </div>
    </nav>
  )
}
