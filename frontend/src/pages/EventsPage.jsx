import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList, displayDate, eventCommittee, eventId, eventStart, eventTitle, seatsRemaining } from '../utils/data'

export default function EventsPage() {
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')
  const [sort, setSort] = useState('soonest')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setEvents(asList((await api.get('/events')).data)) }
    catch (requestError) { setError(getErrorMessage(requestError, 'Published events could not be loaded.')) }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { load() }, [load])

  const categories = useMemo(() => [...new Set(events.map((event) => event.category).filter(Boolean))], [events])
  const visibleEvents = useMemo(() => events
    .filter((event) => !query || `${eventTitle(event)} ${eventCommittee(event)} ${event.venue || ''}`.toLowerCase().includes(query.toLowerCase()))
    .filter((event) => !category || event.category === category)
    .sort((a, b) => sort === 'title' ? eventTitle(a).localeCompare(eventTitle(b)) : new Date(eventStart(a) || 0) - new Date(eventStart(b) || 0)), [events, query, category, sort])

  return <main className="container page-content">
    <div className="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4"><div><h1 className="h2 mb-1">Campus events</h1><p className="text-secondary mb-0">Discover published events and reserve your place.</p></div></div>
    <section className="card border-0 shadow-sm mb-4"><div className="card-body"><div className="row g-2">
      <div className="col-12 col-md-6"><label className="visually-hidden" htmlFor="event-search">Search events</label><input id="event-search" className="form-control" placeholder="Search by title, committee, or venue" value={query} onChange={(e) => setQuery(e.target.value)} /></div>
      <div className="col-6 col-md-3"><select className="form-select" aria-label="Filter by category" value={category} onChange={(e) => setCategory(e.target.value)}><option value="">All categories</option>{categories.map((item) => <option value={item} key={item}>{item}</option>)}</select></div>
      <div className="col-6 col-md-3"><select className="form-select" aria-label="Sort events" value={sort} onChange={(e) => setSort(e.target.value)}><option value="soonest">Soonest first</option><option value="title">Title A-Z</option></select></div>
    </div></div></section>
    {loading ? <LoadingSpinner label="Loading events..." /> : error ? <ErrorAlert message={error} onRetry={load} /> : !visibleEvents.length ? <EmptyState title="No events found" detail="Try another search or check back when new events are published." /> : <section className="row g-4">
      {visibleEvents.map((event) => <EventCard event={event} key={eventId(event)} />)}
    </section>}
  </main>
}

function EventCard({ event }) {
  const seats = seatsRemaining(event)
  return <div className="col-12 col-md-6 col-xl-4"><article className="card event-card h-100 border-0 shadow-sm overflow-hidden">
    {event.posterUrl || event.poster ? <img className="event-poster" src={event.posterUrl || event.poster} alt={`${eventTitle(event)} poster`} /> : <div className="event-poster event-poster--placeholder" aria-label="Event poster placeholder">CampusSync</div>}
    <div className="card-body d-flex flex-column"><div className="d-flex justify-content-between gap-2 mb-2"><span className="badge text-bg-primary">{event.category || 'Campus event'}</span>{seats != null && <span className={`badge ${seats > 0 ? 'text-bg-success' : 'text-bg-danger'}`}>{seats} seats left</span>}</div>
      <h2 className="h5">{eventTitle(event)}</h2><p className="small text-secondary mb-2">Hosted by {eventCommittee(event)}</p>
      <dl className="small mb-3"><div><dt className="d-inline fw-semibold">When: </dt><dd className="d-inline">{displayDate(eventStart(event))}</dd></div><div><dt className="d-inline fw-semibold">Venue: </dt><dd className="d-inline">{event.venue || 'To be announced'}</dd></div></dl>
      <Link className="btn btn-outline-primary mt-auto" to={`/events/${eventId(event)}`}>View event</Link>
    </div>
  </article></div>
}
