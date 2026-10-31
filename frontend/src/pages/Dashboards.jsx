import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { getSession } from '../auth/session'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList, displayDate, eventStart, eventTitle } from '../utils/data'
import TimetableSchedule from '../components/TimetableSchedule'

const dashboardConfig = {
  student: {
    title: 'Student dashboard',
    subtitle: 'Your campus schedule, event registrations, and latest updates.',
    endpoint: '/dashboard/student',
    cards: [],
  },
  coordinator: {
    title: 'Coordinator dashboard',
    subtitle: 'Monitor your committee events and participant activity.',
    endpoint: '/dashboard/coordinator',
    cards: [
      ['committeeEvents', 'Committee events'],
      ['draftEvents', 'Awaiting publication'],
      ['participantCount', 'Participants'],
    ],
  },
  faculty: {
    title: 'Faculty dashboard',
    subtitle: 'Your assigned subjects and attendance work.',
    endpoint: '/attendance/faculty/subjects',
    cards: [],
  },
  admin: {
    title: 'Administrator dashboard',
    subtitle: 'A live overview of campus events and participation.',
    endpoint: '/dashboard/admin',
    cards: [
      ['events', 'Total events'],
      ['publishedEvents', 'Published events'],
      ['draftEvents', 'Draft events'],
      ['registrations', 'Confirmed registrations'],
      ['committees', 'Committees'],
      ['users', 'Active users'],
    ],
  },
}

function metricValue(data, key) {
  const value = data?.[key] ?? data?.counts?.[key] ?? data?.summary?.[key]
  return Array.isArray(value) ? value.length : value ?? 0
}

export default function Dashboard({ kind }) {
  const config = dashboardConfig[kind]
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      const response = await api.get(config.endpoint)
      setData(response.data || {})
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Dashboard data is currently unavailable.'))
    } finally {
      setLoading(false)
    }
  }, [config.endpoint])

  useEffect(() => { void Promise.resolve().then(load) }, [load])

  if (loading) return <LoadingSpinner label="Loading dashboard..." />
  if (kind === 'student') return <StudentDashboard data={data} error={error} onRetry={load} />
  if (kind === 'faculty') return <FacultyDashboard subjects={asList(data)} error={error} onRetry={load} />
  return (
    <main className="container page-content dashboard-page">
      <div className="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4">
        <div><p className="eyebrow dashboard-eyebrow">{kind === 'admin' ? 'Campus operations' : 'Committee workspace'}</p><h1 className="h2 mb-1">{config.title}</h1><p className="text-secondary mb-0">{config.subtitle}</p></div>
        {kind === 'admin' ? <div className="d-flex flex-wrap gap-2 align-self-start"><Link className="btn btn-primary" to="/admin/events">Manage events</Link><Link className="btn btn-outline-primary" to="/admin/committees">Manage committees</Link><Link className="btn btn-outline-primary" to="/admin/timetable">Manage timetable</Link><Link className="btn btn-outline-primary" to="/admin/coordinators">Provision coordinators</Link><Link className="btn btn-outline-primary" to="/admin/faculty">Provision faculty</Link><Link className="btn btn-outline-primary" to="/admin/attendance">Attendance reports</Link></div> : kind === 'coordinator' ? <Link className="btn btn-primary align-self-start" to="/coordinator/events">Manage committee events</Link> : kind === 'student' && <div className="d-flex gap-2 align-self-start"><Link className="btn btn-primary" to="/events">Browse events</Link><Link className="btn btn-outline-primary" to="/my-registrations">My registrations</Link></div>}
      </div>
      {error && <ErrorAlert message={error} onRetry={load} />}
      {!error && <>
        <section className="row g-3 mb-4">
          {config.cards.map(([key, label]) => (
            <div className="col-6 col-md-4 col-xl" key={key}>
              <article className="card h-100 border-0 shadow-sm stat-card">
                <div className="card-body"><p className="text-secondary small mb-2">{label}</p><p className="display-6 fw-semibold mb-0">{metricValue(data, key)}</p></div>
              </article>
            </div>
          ))}
        </section>
        {kind === 'admin' ? <AdminSections data={data} /> : <EventPreview events={asList(data?.upcomingEvents || data?.recentEvents || data?.events)} />}
      </>}
    </main>
  )
}

function FacultyDashboard({ subjects, error, onRetry }) {
  const assignedSubjectCount = new Set(subjects.map((subject) => subject.id)).size
  return (
    <main className="container page-content dashboard-page">
      <div className="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4">
        <div><p className="eyebrow dashboard-eyebrow">Teaching workspace</p><h1 className="h2 mb-1">Faculty dashboard</h1><p className="text-secondary mb-0">Your assigned subjects and attendance work.</p></div>
        <div className="d-flex flex-wrap gap-2 align-self-start"><Link className="btn btn-primary" to="/faculty/attendance">Manage attendance</Link><Link className="btn btn-outline-secondary" to="/faculty/timetable">Schedule a lecture</Link></div>
      </div>
      {error ? <ErrorAlert message={error} onRetry={onRetry} /> : <>
        <section className="row g-3 mb-4" aria-label="Faculty summary">
          <SummaryCard label="Assigned subjects" value={assignedSubjectCount} detail="Current teaching assignments" />
        </section>
        <TimetableSchedule role="faculty" />
        <section className="card border-0 shadow-sm" aria-labelledby="faculty-subjects-heading">
          <div className="card-body p-3 p-md-4">
            <h2 id="faculty-subjects-heading" className="h5 mb-3">Assigned subjects</h2>
            {!subjects.length ? <EmptyState title="No assigned subjects" detail="An administrator must provision your faculty profile and assign a subject, semester, and division before attendance can be recorded." /> :
              <div className="row g-3">
                {subjects.map((subject) => <div className="col-12 col-md-6 col-xl-4" key={subject.id}>
                  <article className="card h-100 bg-light border-0">
                    <div className="card-body">
                      <h3 className="h6 mb-1">{subject.code} · {subject.name}</h3>
                      <p className="small text-secondary mb-0">Semester {subject.semester ?? 'not assigned'} · Division {subject.division || 'not assigned'}</p>
                    </div>
                  </article>
                </div>)}
              </div>}
          </div>
        </section>
      </>}
    </main>
  )
}

function StudentDashboard({ data, error, onRetry }) {
  const upcoming = asList(data?.upcomingEvents)
  const notifications = asList(data?.notifications)
  const sessionUser = getSession()?.user
  const displayName = sessionUser?.name || sessionUser?.firstName || sessionUser?.username
  return (
    <main className="container page-content dashboard-page">
      <div className="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4">
        <div><p className="eyebrow dashboard-eyebrow">Student workspace</p><h1 className="h2 mb-1">Welcome{displayName ? `, ${displayName}` : ''}</h1><p className="text-secondary mb-0">Your campus schedule, event registrations, and latest updates.</p></div>
        <nav className="d-flex flex-wrap gap-2 align-self-start" aria-label="Student quick links">
          <Link className="btn btn-primary" to="/events">Browse events</Link>
          <Link className="btn btn-outline-primary" to="/my-registrations">My registrations</Link>
        </nav>
      </div>
      {error ? <ErrorAlert message={error} onRetry={onRetry} /> : <>
        <section className="row g-3 mb-4" aria-label="Student summary">
          <SummaryCard label="Active event registrations" value={data?.activeRegistrations ?? 0} detail="Confirmed registrations" />
          <SummaryCard label="Unread notifications" value={data?.unreadNotifications ?? 0} detail="For your account" />
        </section>

        <section className="row g-4">
          <div className="col-12"><TimetableSchedule role="student" /></div>
          <div className="col-12">
            <StudentAttendance />
          </div>

          <div className="col-12">
            <section className="card border-0 shadow-sm" aria-labelledby="upcoming-events-heading">
              <div className="card-body p-3 p-md-4">
                <div className="d-flex justify-content-between align-items-center gap-2 mb-3">
                  <h2 id="upcoming-events-heading" className="h5 mb-0">Upcoming campus events</h2>
                  <Link to="/events" className="small">Browse all events</Link>
                </div>
                {!upcoming.length ? <EmptyState title="No upcoming published events" detail="Published campus events will appear here when available." /> :
                  <div className="list-group list-group-flush">
                    {upcoming.map((event) => (
                      <article className="list-group-item px-0 py-3" key={event.id}>
                        <div className="d-flex flex-column flex-md-row justify-content-between gap-3">
                          <div>
                            <h3 className="h6 mb-1"><Link to={`/events/${event.id}`}>{event.title}</Link></h3>
                            <p className="small text-secondary mb-1">
                              <time dateTime={event.startsAt}>{displayDate(event.startsAt)}</time>
                              {' · '}{event.venue || 'Venue not specified'}
                            </p>
                            <p className="small text-secondary mb-0">
                              {event.remainingSeats ?? '—'} seats remaining
                              {event.registrationStatus && <> · Your status: {event.registrationStatus}</>}
                              {!event.registrationStatus && <> · Not registered</>}
                            </p>
                          </div>
                          <Link className="btn btn-sm btn-outline-primary align-self-md-center flex-shrink-0" to={`/events/${event.id}`}>
                            {event.registrationStatus === 'CONFIRMED' ? 'Event details' : 'View and register'}
                          </Link>
                        </div>
                      </article>
                    ))}
                  </div>}
              </div>
            </section>
          </div>

          <div className="col-12">
            <section className="card border-0 shadow-sm" aria-labelledby="recent-notifications-heading">
              <div className="card-body p-3 p-md-4">
                <div className="d-flex justify-content-between align-items-center gap-2 mb-3">
                  <h2 id="recent-notifications-heading" className="h5 mb-0">Recent notifications</h2>
                  <Link to="/notifications" className="small">View all</Link>
                </div>
                {!notifications.length ? <EmptyState title="No notifications" detail="Updates for your account will appear here." /> :
                  <div className="list-group list-group-flush">
                    {notifications.map((notification) => (
                      <article className={`list-group-item px-0 ${notification.read ? '' : 'notification--unread'}`} key={notification.id}>
                        <div className="d-flex justify-content-between gap-3">
                          <div>
                            <h3 className="h6 mb-1">{notification.title}</h3>
                            <p className="mb-1">{notification.message}</p>
                            <time className="small text-secondary" dateTime={notification.createdAt}>{displayDate(notification.createdAt)}</time>
                          </div>
                          {!notification.read && <span className="badge text-bg-primary align-self-start">Unread</span>}
                        </div>
                      </article>
                    ))}
                  </div>}
              </div>
            </section>
          </div>
        </section>
      </>}
    </main>
  )
}

function StudentAttendance() {
  const [attendance, setAttendance] = useState(null)
  const [loadingAttendance, setLoadingAttendance] = useState(true)
  const [attendanceError, setAttendanceError] = useState('')

  const loadAttendance = useCallback(async () => {
    setLoadingAttendance(true)
    setAttendanceError('')
    try {
      setAttendance((await api.get('/attendance/student/me')).data)
    } catch (requestError) {
      setAttendanceError(getErrorMessage(requestError, 'Attendance information could not be loaded.'))
    } finally {
      setLoadingAttendance(false)
    }
  }, [])
  useEffect(() => { void Promise.resolve().then(loadAttendance) }, [loadAttendance])

  return <section className="card border-0 shadow-sm" aria-labelledby="student-attendance-heading">
    <div className="card-body p-3 p-md-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
          <h2 id="student-attendance-heading" className="h5 mb-1">My attendance</h2>
          <p className="small text-secondary mb-0">
            {attendance ? `Shortage threshold: ${attendance.thresholdPercent}%` : 'Attendance shortage threshold'}
          </p>
        </div>
        {!loadingAttendance && !attendanceError && attendance?.heldClasses > 0 &&
          <span className={`badge ${attendance.belowThreshold ? 'text-bg-danger' : 'text-bg-success'}`}>
            {attendance.overallPercentage}% overall
          </span>}
      </div>
      {loadingAttendance ? <LoadingSpinner label="Loading attendance..." /> :
        attendanceError ? <ErrorAlert message={attendanceError} onRetry={loadAttendance} /> :
          attendance?.heldClasses === 0 ? <EmptyState title="No attendance sessions recorded" detail="Your attendance will appear after faculty record class sessions for your division." /> :
            <>
              {attendance.belowThreshold && <div className="alert alert-warning" role="status">
                Your recorded attendance is below the {attendance.thresholdPercent}% threshold.
              </div>}
              <div className="row g-3 mb-4">
                <div className="col-6 col-md-3"><div className="rounded border bg-light p-3"><span className="small text-secondary d-block">Attended classes</span><strong>{attendance.attendedClasses}</strong></div></div>
                <div className="col-6 col-md-3"><div className="rounded border bg-light p-3"><span className="small text-secondary d-block">Missed classes</span><strong>{Math.max(0, attendance.heldClasses - attendance.attendedClasses)}</strong></div></div>
                <div className="col-6 col-md-3"><div className="rounded border bg-light p-3"><span className="small text-secondary d-block">Classes conducted</span><strong>{attendance.heldClasses}</strong></div></div>
                <div className="col-6 col-md-3"><div className="rounded border bg-light p-3"><span className="small text-secondary d-block">Overall attendance</span><strong>{attendance.overallPercentage}%</strong></div></div>
              </div>
              <h3 className="h6">By subject</h3>
              <div className="table-responsive mb-4">
                <table className="table table-sm align-middle">
                  <thead><tr><th scope="col">Subject</th><th scope="col">Classes conducted</th><th scope="col">Attended</th><th scope="col">Missed</th><th scope="col">Attendance</th></tr></thead>
                  <tbody>{attendance.subjects.map((subject) => <tr key={subject.subjectId}>
                    <th scope="row">{subject.subjectCode} · {subject.subjectName}</th>
                    <td>{subject.heldClasses}</td><td>{subject.attendedClasses}</td>
                    <td>{Math.max(0, subject.heldClasses - subject.attendedClasses)}</td>
                    <td><span className={subject.belowThreshold ? 'text-danger fw-semibold' : ''}>{subject.percentage}%</span>
                      {subject.belowThreshold && <span className="badge text-bg-danger ms-2">Shortage</span>}</td>
                  </tr>)}</tbody>
                </table>
              </div>
              <h3 className="h6">Recent history</h3>
              {!attendance.history.length ? <p className="small text-secondary mb-0">No recorded attendance history.</p> :
                <ul className="list-group list-group-flush">
                  {attendance.history.slice(0, 10).map((item) => <li className="list-group-item px-0 d-flex flex-wrap justify-content-between gap-2" key={item.recordId}>
                    <span>{item.subjectCode} · {item.subjectName} · {displayDate(item.sessionDate, { dateStyle: 'medium' })}{item.startTime ? ` · ${item.startTime}` : ''}</span>
                    <span className={`badge ${item.status === 'PRESENT' ? 'text-bg-success' : 'text-bg-secondary'}`}>{item.status}</span>
                  </li>)}
                </ul>}
            </>}
    </div>
  </section>
}

function SummaryCard({ label, value, detail }) {
  return <div className="col-6 col-xl-3">
    <article className="card h-100 border-0 shadow-sm stat-card">
      <div className="card-body">
        <p className="text-secondary small mb-2">{label}</p>
        <p className="h4 fw-semibold mb-1">{value}</p>
        <p className="small text-secondary mb-0">{detail}</p>
      </div>
    </article>
  </div>
}

function AdminSections({ data }) {
  const upcoming = asList(data?.upcomingEvents)
  const counts = asList(data?.registrationCounts)
  return (
    <>
      <EventPreview events={upcoming} title="Upcoming campus events" />
      <section className="card border-0 shadow-sm mt-4"><div className="card-body">
        <h2 className="h5 mb-3">Confirmed registrations by event</h2>
        {!counts.length ? <EmptyState title="No confirmed registrations" detail="Registration summaries will appear when students register for events." /> : <div className="table-responsive">
          <table className="table align-middle mb-0"><thead><tr><th>Event</th><th className="text-end">Registrations</th></tr></thead><tbody>
            {counts.map((item) => <tr key={item.eventId}><td>{item.eventTitle}</td><td className="text-end">{item.registrationCount}</td></tr>)}
          </tbody></table>
        </div>}
      </div></section>
    </>
  )
}

function EventPreview({ events, title = 'Recent event activity' }) {
  if (!events.length) return <EmptyState title="No upcoming events" detail="Published campus events will appear here when available." />
  return (
    <section className="card border-0 shadow-sm"><div className="card-body p-3 p-md-4">
      <h2 className="h5 mb-3">{title}</h2>
      <div className="list-group list-group-flush">
        {events.slice(0, 5).map((event, index) => <div className="list-group-item px-0" key={event.id || event.eventId || index}>
          <strong>{eventTitle(event)}</strong>
          <span className="d-block small text-secondary">
            {displayDate(eventStart(event))}
            {event.registeredCount != null && ` · ${event.registeredCount} registered`}
            {event.remainingSeats != null && ` · ${event.remainingSeats} seats remaining`}
          </span>
        </div>)}
      </div>
    </div></section>
  )
}
