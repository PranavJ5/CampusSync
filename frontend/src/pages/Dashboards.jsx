import { useCallback, useEffect, useState } from 'react'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList, displayDate, eventStart, eventTitle } from '../utils/data'

const dashboardConfig = {
  student: {
    title: 'Student dashboard',
    subtitle: 'Keep track of your campus activities and memberships.',
    endpoint: '/dashboard/student',
    cards: [
      ['upcomingEvents', 'Upcoming registered events'],
      ['committeeMemberships', 'Committee memberships'],
      ['unreadNotifications', 'Unread notifications'],
    ],
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
  admin: {
    title: 'Administrator dashboard',
    subtitle: 'A live overview of CampusSync master data and participation.',
    endpoint: '/dashboard/admin',
    cards: [
      ['users', 'Users'],
      ['departments', 'Departments'],
      ['committees', 'Committees'],
      ['events', 'Events'],
      ['registrations', 'Registrations'],
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
    setLoading(true)
    setError('')
    try {
      const response = await api.get(config.endpoint)
      setData(response.data || {})
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Dashboard data is currently unavailable.'))
    } finally {
      setLoading(false)
    }
  }, [config.endpoint])

  useEffect(() => { load() }, [load])

  if (loading) return <LoadingSpinner label="Loading dashboard..." />
  return (
    <main className="container page-content">
      <div className="mb-4">
        <h1 className="h2 mb-1">{config.title}</h1>
        <p className="text-secondary mb-0">{config.subtitle}</p>
      </div>
      {error && <ErrorAlert message={error} onRetry={load} />}
      {!error && <>
        <section className="row g-3 mb-4">
          {config.cards.map(([key, label]) => (
            <div className="col-6 col-md" key={key}>
              <article className="card h-100 border-0 shadow-sm stat-card">
                <div className="card-body"><p className="text-secondary small mb-2">{label}</p><p className="display-6 fw-semibold mb-0">{metricValue(data, key)}</p></div>
              </article>
            </div>
          ))}
        </section>
        <EventPreview events={asList(data?.upcomingEvents || data?.recentEvents || data?.events)} />
      </>}
    </main>
  )
}

function EventPreview({ events }) {
  if (!events.length) return <EmptyState title="No event activity to show" detail="New or registered events will appear here." />
  return (
    <section className="card border-0 shadow-sm"><div className="card-body p-3 p-md-4">
      <h2 className="h5 mb-3">Recent event activity</h2>
      <div className="list-group list-group-flush">
        {events.slice(0, 5).map((event, index) => <div className="list-group-item px-0" key={event.id || event.eventId || index}>
          <strong>{eventTitle(event)}</strong><span className="d-block small text-secondary">{displayDate(eventStart(event))}</span>
        </div>)}
      </div>
    </div></section>
  )
}
