import { NavLink } from 'react-router-dom'

const navigationByRole = {
  ADMINISTRATOR: [
    { to: '/admin', label: 'Overview', end: true },
    { to: '/admin/events', label: 'Event management' },
    { to: '/admin/committees', label: 'Committees' },
    { to: '/admin/timetable', label: 'Timetable' },
    { to: '/admin/coordinators', label: 'Coordinators' },
    { to: '/admin/faculty', label: 'Faculty' },
    { to: '/admin/attendance', label: 'Attendance reports' },
    { to: '/notifications', label: 'Notifications' },
  ],
  STUDENT: [
    { to: '/student', label: 'Overview', end: true },
    { to: '/events', label: 'Campus events' },
    { to: '/my-registrations', label: 'My registrations' },
    { to: '/notifications', label: 'Notifications' },
  ],
  FACULTY: [
    { to: '/faculty', label: 'Overview', end: true },
    { to: '/faculty/attendance', label: 'Attendance' },
    { to: '/faculty/timetable', label: 'Schedule lecture' },
    { to: '/notifications', label: 'Notifications' },
  ],
  COMMITTEE_COORDINATOR: [
    { to: '/coordinator', label: 'Overview', end: true },
    { to: '/coordinator/events', label: 'Committee events' },
    { to: '/notifications', label: 'Notifications' },
  ],
}

export default function AppSidebar({ role, unreadCount = 0 }) {
  const links = navigationByRole[String(role || '').toUpperCase()] || []

  return (
    <aside className="app-sidebar" aria-label="Main navigation">
      <p className="app-sidebar__label">Workspace</p>
      <nav className="app-sidebar__nav">
        {links.map(({ to, label, end }) => (
          <NavLink
            className={({ isActive }) => `app-sidebar__link${isActive ? ' active' : ''}`}
            end={end}
            key={to}
            to={to}
          >
            <span>{label}</span>
            {label === 'Notifications' && unreadCount > 0 &&
              <span className="app-sidebar__count">{unreadCount}</span>}
          </NavLink>
        ))}
      </nav>
      <div className="app-sidebar__footer">
        A connected campus, in one place.
      </div>
    </aside>
  )
}
