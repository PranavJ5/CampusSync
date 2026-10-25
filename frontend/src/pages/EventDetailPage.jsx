import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { displayDate, eventCommittee, eventDeadline, eventStart, eventTitle, isPast, seatsRemaining } from '../utils/data'

const registrationMessage = (error) => {
  const status = error.response?.status
  if (status === 409) return 'Registration could not be completed: you are already registered or all seats are full.'
  if (status === 400) return 'Registration is closed because the deadline has passed or this request is invalid.'
  return getErrorMessage(error, 'Registration could not be completed. Please try again.')
}

export default function EventDetailPage() {
  const { id } = useParams()
  const [event, setEvent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState(null)
  const [registering, setRegistering] = useState(false)

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setEvent((await api.get(`/events/${id}`)).data) }
    catch (requestError) { setError(getErrorMessage(requestError, 'This event could not be found.')) }
    finally { setLoading(false) }
  }, [id])
  useEffect(() => { load() }, [load])

  const register = async () => {
    setRegistering(true); setNotice(null)
    try {
      const response = await api.post(`/events/${id}/registrations`)
      if (response.status === 201) {
        setNotice({ type: 'success', text: 'Registration confirmed. A notification has been added to your account.' })
        load()
      }
    } catch (requestError) {
      setNotice({ type: 'danger', text: registrationMessage(requestError) })
    } finally { setRegistering(false) }
  }

  if (loading) return <LoadingSpinner label="Loading event details..." />
  if (error) return <main className="container page-content"><ErrorAlert message={error} onRetry={load} /><Link to="/events" className="btn btn-outline-primary">Back to events</Link></main>
  const seats = seatsRemaining(event)
  const registrationClosed = seats === 0 || isPast(eventDeadline(event))

  return <main className="container page-content"><Link to="/events" className="small text-decoration-none">← Back to events</Link>
    <article className="card border-0 shadow-sm mt-3 overflow-hidden"><div className="row g-0">
      <div className="col-lg-5">{event.posterUrl || event.poster ? <img className="event-detail-poster" src={event.posterUrl || event.poster} alt={`${eventTitle(event)} poster`} /> : <div className="event-detail-poster event-poster--placeholder">CampusSync event</div>}</div>
      <div className="col-lg-7"><div className="card-body p-4 p-lg-5"><div className="d-flex flex-wrap gap-2 mb-3"><span className="badge text-bg-primary">{event.category || 'Campus event'}</span>{seats != null && <span className={`badge ${seats > 0 ? 'text-bg-success' : 'text-bg-danger'}`}>{seats} seats remaining</span>}</div>
        <h1 className="h2">{eventTitle(event)}</h1><p className="lead text-secondary">Hosted by {eventCommittee(event)}</p>
        <p>{event.description || 'Event details will be available soon.'}</p>
        <dl className="row mb-4"><dt className="col-sm-4">Schedule</dt><dd className="col-sm-8">{displayDate(eventStart(event))}</dd><dt className="col-sm-4">Venue</dt><dd className="col-sm-8">{event.venue || 'To be announced'}</dd><dt className="col-sm-4">Registration deadline</dt><dd className="col-sm-8">{displayDate(eventDeadline(event))}</dd></dl>
        {notice && <div className={`alert alert-${notice.type}`} role="alert">{notice.text}</div>}
        <button className="btn btn-primary" onClick={register} disabled={registering || registrationClosed}>{registering ? <><span className="spinner-border spinner-border-sm me-2" />Registering...</> : registrationClosed ? 'Registration closed' : 'Register for event'}</button>
      </div></div>
    </div></article>
  </main>
}
