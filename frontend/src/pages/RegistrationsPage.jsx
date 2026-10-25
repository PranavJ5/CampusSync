import { useCallback, useEffect, useMemo, useState } from 'react'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList, displayDate, eventDeadline, eventStart, eventTitle, isPast } from '../utils/data'

export default function RegistrationsPage() {
  const [registrations, setRegistrations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState('upcoming')
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setRegistrations(asList((await api.get('/registrations/me')).data)) }
    catch (requestError) { setError(getErrorMessage(requestError, 'Your registrations could not be loaded.')) }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { load() }, [load])

  const filtered = useMemo(() => registrations.filter((registration) => {
    const event = registration.event || registration
    return activeTab === 'past' ? isPast(eventStart(event)) : !isPast(eventStart(event))
  }), [registrations, activeTab])

  const cancel = async (registration) => {
    const event = registration.event || registration
    const eventId = event.id || event.eventId
    if (!window.confirm(`Cancel your registration for “${eventTitle(event)}”?`)) return
    setNotice('')
    try {
      await api.delete(`/registrations/${eventId}`)
      setNotice('Your registration has been cancelled.')
      load()
    } catch (requestError) { setError(getErrorMessage(requestError, 'This registration could not be cancelled.')) }
  }

  return <main className="container page-content"><h1 className="h2 mb-1">My registrations</h1><p className="text-secondary mb-4">Manage your confirmed campus event registrations.</p>
    <ul className="nav nav-tabs mb-3"><li className="nav-item"><button className={`nav-link ${activeTab === 'upcoming' ? 'active' : ''}`} onClick={() => setActiveTab('upcoming')}>Upcoming</button></li><li className="nav-item"><button className={`nav-link ${activeTab === 'past' ? 'active' : ''}`} onClick={() => setActiveTab('past')}>Past</button></li></ul>
    {notice && <div className="alert alert-success" role="alert">{notice}</div>}{error && <ErrorAlert message={error} onRetry={load} />}
    {loading ? <LoadingSpinner label="Loading registrations..." /> : !error && !filtered.length ? <EmptyState title={activeTab === 'upcoming' ? 'No registered events found' : 'No past registrations found'} detail="Your event registrations will appear here." /> : !error && <div className="table-responsive card border-0 shadow-sm"><table className="table align-middle mb-0"><thead><tr><th>Event</th><th>Schedule</th><th>Status</th><th className="text-end">Action</th></tr></thead><tbody>{filtered.map((registration, index) => {
      const event = registration.event || registration; const cancellable = !isPast(eventStart(event)) && !isPast(eventDeadline(event)) && String(registration.status || registration.registrationStatus || '').toUpperCase() !== 'CANCELLED'
      return <tr key={registration.id || registration.registrationId || event.id || index}><td><strong>{eventTitle(event)}</strong><span className="d-block small text-secondary">{event.venue || 'Venue TBA'}</span></td><td>{displayDate(eventStart(event))}</td><td><span className="badge text-bg-secondary">{registration.status || registration.registrationStatus || 'Registered'}</span></td><td className="text-end">{cancellable && <button className="btn btn-sm btn-outline-danger" onClick={() => cancel(registration)}>Cancel registration</button>}</td></tr>
    })}</tbody></table></div>}
  </main>
}
