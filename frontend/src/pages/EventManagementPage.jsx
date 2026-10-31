import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { getSession } from '../auth/session'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList, displayDate } from '../utils/data'

const blankEvent = () => ({
  committeeId: '',
  title: '',
  description: '',
  category: '',
  startsAt: '',
  endsAt: '',
  registrationDeadline: '',
  venue: '',
  capacity: '',
})

const localDateTimeInput = (value) => value ? String(value).slice(0, 16) : ''
const localDateTimePayload = (value) => value.length === 16 ? `${value}:00` : value
const currentLocalDateTime = () => {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

export default function EventManagementPage() {
  const isAdministrator = String(getSession()?.user?.role || '').toUpperCase() === 'ADMINISTRATOR'
  const [events, setEvents] = useState([])
  const [committees, setCommittees] = useState([])
  const [form, setForm] = useState(blankEvent)
  const [editingEvent, setEditingEvent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [publishingId, setPublishingId] = useState(null)
  const [cancellingId, setCancellingId] = useState(null)
  const [participantsEvent, setParticipantsEvent] = useState(null)
  const [participants, setParticipants] = useState([])
  const [participantsLoading, setParticipantsLoading] = useState(false)
  const [participantsError, setParticipantsError] = useState('')
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    try {
      const [eventResponse, committeeResponse] = await Promise.all([
        api.get('/events/management'),
        api.get('/committees/manageable'),
      ])
      setEvents(asList(eventResponse.data))
      setCommittees(asList(committeeResponse.data))
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Event management data could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void Promise.resolve().then(load) }, [load])

  const retryLoad = () => {
    load()
  }

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setFormError('')
  }

  const resetForm = () => {
    setForm(blankEvent())
    setEditingEvent(null)
    setFormError('')
  }

  const editEvent = (event) => {
    setEditingEvent(event)
    setForm({
      committeeId: String(event.committeeId),
      title: event.title || '',
      description: event.description || '',
      category: event.category || '',
      startsAt: localDateTimeInput(event.startsAt),
      endsAt: localDateTimeInput(event.endsAt),
      registrationDeadline: localDateTimeInput(event.registrationDeadline),
      venue: event.venue || '',
      capacity: String(event.capacity ?? ''),
    })
    setFormError('')
    setNotice('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const validateForm = () => {
    if (!editingEvent && !form.committeeId) return 'Choose a committee.'
    if (!editingEvent && (!form.title.trim() || !form.category.trim())) return 'Title and category are required.'
    if (!form.startsAt || !form.endsAt || !form.registrationDeadline) return 'Start, end, and registration deadline are required.'
    if (form.startsAt <= currentLocalDateTime()) return 'Event must start in the future.'
    if (form.endsAt <= form.startsAt) return 'Event end must be after its start.'
    if (form.registrationDeadline <= currentLocalDateTime()) return 'Registration deadline must be in the future.'
    if (form.registrationDeadline > form.startsAt) return 'Registration deadline must be no later than the event start.'
    if (!form.venue.trim()) return 'Venue is required.'
    const capacity = Number(form.capacity)
    if (!Number.isInteger(capacity) || capacity <= 0) return 'Capacity must be a positive whole number.'
    return ''
  }

  const save = async (event) => {
    event.preventDefault()
    setNotice('')
    const validationError = validateForm()
    if (validationError) {
      setFormError(validationError)
      return
    }
    setSaving(true)
    setFormError('')
    const payload = {
      description: form.description,
      startsAt: localDateTimePayload(form.startsAt),
      endsAt: localDateTimePayload(form.endsAt),
      registrationDeadline: localDateTimePayload(form.registrationDeadline),
      venue: form.venue.trim(),
      capacity: Number(form.capacity),
    }
    if (!editingEvent || String(editingEvent.status).toUpperCase() === 'DRAFT') {
      payload.title = form.title.trim()
      payload.category = form.category.trim()
    }
    try {
      if (editingEvent) {
        await api.put(`/events/${editingEvent.id}`, payload)
        setNotice('Event changes saved.')
      } else {
        await api.post('/events', {
          ...payload,
          committeeId: Number(form.committeeId),
          title: form.title.trim(),
          category: form.category.trim(),
        })
        setNotice('Event created as a draft.')
      }
      resetForm()
      setLoading(true)
      await load()
    } catch (requestError) {
      setFormError(getErrorMessage(requestError, 'Event changes could not be saved.'))
    } finally {
      setSaving(false)
    }
  }

  const publish = async (event) => {
    setPublishingId(event.id)
    setError('')
    setNotice('')
    try {
      await api.post(`/events/${event.id}/publish`)
      setNotice(`“${event.title}” is now published.`)
      setLoading(true)
      await load()
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'This event could not be published.'))
    } finally {
      setPublishingId(null)
    }
  }

  const cancelEvent = async (event) => {
    const reason = window.prompt('Enter a cancellation reason (optional):', '')
    if (reason === null) return
    if (!window.confirm(`Cancel “${event.title}”? Students will no longer be able to register.`)) return
    setCancellingId(event.id)
    setError('')
    setNotice('')
    try {
      await api.post(`/events/${event.id}/cancel`, { reason: reason.trim() || null })
      setNotice(`“${event.title}” was cancelled.`)
      setLoading(true)
      await load()
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'This event could not be cancelled.'))
    } finally {
      setCancellingId(null)
    }
  }

  const copyEventLink = async (event) => {
    const link = `${window.location.origin}/events/${event.id}`
    try {
      await navigator.clipboard.writeText(link)
      setNotice('Shareable event link copied.')
    } catch {
      setNotice(link)
    }
  }

  const showParticipants = async (event) => {
    setParticipantsEvent(event)
    setParticipants([])
    setParticipantsError('')
    setParticipantsLoading(true)
    try {
      const path = isAdministrator
        ? `/admin/events/${event.id}/registrations`
        : `/events/${event.id}/registrations`
      setParticipants(asList((await api.get(path)).data))
    } catch (requestError) {
      setParticipantsError(getErrorMessage(requestError, 'Event registrations could not be loaded.'))
    } finally {
      setParticipantsLoading(false)
    }
  }

  const committeesById = new Map(committees.map((committee) => [committee.id, committee]))
  const titleAndCategoryEditable = !editingEvent || String(editingEvent.status).toUpperCase() === 'DRAFT'

  return (
    <main className="container page-content">
      <div className="mb-4">
        <h1 className="h2 mb-1">Event management</h1>
        <p className="text-secondary mb-0">Create, update, and publish campus events.</p>
      </div>

      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {error && <ErrorAlert message={error} onRetry={retryLoad} />}

      <section className="card border-0 shadow-sm mb-4">
        <div className="card-body p-3 p-md-4">
          <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
            <div>
              <h2 className="h5 mb-1">{editingEvent ? 'Edit event' : 'Create an event'}</h2>
              {editingEvent && !titleAndCategoryEditable && <p className="small text-secondary mb-0">Title and category cannot be changed after publication.</p>}
            </div>
            {editingEvent && <button type="button" className="btn btn-sm btn-outline-secondary" onClick={resetForm}>Cancel edit</button>}
          </div>

          {formError && <div className="alert alert-danger" role="alert">{formError}</div>}
          {!editingEvent && !loading && !error && !committees.length && <div className="alert alert-warning" role="status">
            {isAdministrator
              ? <>Create or activate a committee before creating an event. <Link to="/admin/committees" className="alert-link">Open Committee Management</Link>.</>
              : 'No active assigned committees are available for event creation.'}
          </div>}
          <form onSubmit={save} noValidate>
            <div className="row g-3">
              {!editingEvent && <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="event-committee">Committee</label>
                <select id="event-committee" className="form-select" name="committeeId" value={form.committeeId} onChange={updateField} required>
                  <option value="">Select a committee</option>
                  {committees.map((committee) => <option key={committee.id} value={committee.id}>{committee.name} ({committee.code})</option>)}
                </select>
              </div>}
              <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="event-title">Title</label>
                <input id="event-title" className="form-control" name="title" maxLength="200" value={form.title} onChange={updateField} disabled={!titleAndCategoryEditable} required />
              </div>
              <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="event-category">Category</label>
                <input id="event-category" className="form-control" name="category" maxLength="50" value={form.category} onChange={updateField} disabled={!titleAndCategoryEditable} required />
              </div>
              <div className="col-12">
                <label className="form-label" htmlFor="event-description">Description</label>
                <textarea id="event-description" className="form-control" name="description" rows="3" maxLength="65535" value={form.description} onChange={updateField} />
              </div>
              <DateTimeField id="event-start" name="startsAt" label="Starts at" value={form.startsAt} onChange={updateField} />
              <DateTimeField id="event-end" name="endsAt" label="Ends at" value={form.endsAt} onChange={updateField} />
              <DateTimeField id="event-deadline" name="registrationDeadline" label="Registration deadline" value={form.registrationDeadline} onChange={updateField} />
              <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="event-venue">Venue</label>
                <input id="event-venue" className="form-control" name="venue" maxLength="255" value={form.venue} onChange={updateField} required />
              </div>
              <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="event-capacity">Capacity</label>
                <input id="event-capacity" className="form-control" name="capacity" type="number" min="1" step="1" value={form.capacity} onChange={updateField} required />
              </div>
              <div className="col-12">
                <button className="btn btn-primary" type="submit" disabled={saving || loading || (!editingEvent && !committees.length)}>
                  {saving ? 'Saving...' : editingEvent ? 'Save changes' : 'Create draft'}
                </button>
              </div>
            </div>
          </form>
        </div>
      </section>

      <section>
        <h2 className="h5 mb-3">Managed events</h2>
        {loading ? <LoadingSpinner label="Loading managed events..." /> : error ? null : !events.length ? (
          <EmptyState title="No events yet" detail="Create an event above to get started." />
        ) : (
          <div className="table-responsive card border-0 shadow-sm">
            <table className="table align-middle mb-0">
              <thead><tr><th>Event</th><th>Committee</th><th>Schedule</th><th>Participants</th><th>Status</th><th className="text-end">Actions</th></tr></thead>
              <tbody>{events.map((event) => {
                const status = String(event.status || '').toUpperCase()
                const committee = committeesById.get(event.committeeId)
                return <tr key={event.id}>
                  <td><strong>{event.title}</strong><span className="d-block small text-secondary">{event.category}</span></td>
                  <td>{committee?.name || `Committee ${event.committeeId}`}</td>
                  <td>{displayDate(event.startsAt)}</td>
                  <td>{event.registeredCount ?? 0} / {event.capacity ?? '—'}<span className="d-block small text-secondary">{event.remainingSeats ?? '—'} seats remaining</span></td>
                  <td><span className={`badge ${status === 'PUBLISHED' ? 'text-bg-success' : status === 'DRAFT' ? 'text-bg-warning' : 'text-bg-secondary'}`}>{status || 'UNKNOWN'}</span></td>
                  <td className="text-end text-nowrap">
                    <button type="button" className="btn btn-sm btn-outline-secondary me-2" onClick={() => showParticipants(event)}>Participants</button>
                    <button type="button" className="btn btn-sm btn-outline-primary me-2" onClick={() => editEvent(event)} disabled={!['DRAFT', 'PUBLISHED'].includes(status)}>Edit</button>
                    {status === 'PUBLISHED' && <button type="button" className="btn btn-sm btn-outline-primary me-2" onClick={() => copyEventLink(event)}>Copy link</button>}
                    {status === 'DRAFT' && <button type="button" className="btn btn-sm btn-primary me-2" onClick={() => publish(event)} disabled={publishingId === event.id}>{publishingId === event.id ? 'Publishing...' : 'Publish'}</button>}
                    {['PUBLISHED', 'REGISTRATION_CLOSED'].includes(status) && <button type="button" className="btn btn-sm btn-outline-danger" onClick={() => cancelEvent(event)} disabled={cancellingId === event.id}>{cancellingId === event.id ? 'Cancelling...' : 'Cancel event'}</button>}
                  </td>
                </tr>
              })}</tbody>
            </table>
          </div>
        )}
      </section>
      {participantsEvent && <section className="card border-0 shadow-sm mt-4">
        <div className="card-body">
          <div className="d-flex justify-content-between gap-3 mb-3">
            <div><h2 className="h5 mb-1">Registrations: {participantsEvent.title}</h2><p className="small text-secondary mb-0">{isAdministrator ? 'Registration information from student profiles.' : 'Confirmed students only. Personal contact details are not displayed.'}</p></div>
            <button type="button" className="btn btn-sm btn-outline-secondary align-self-start" onClick={() => setParticipantsEvent(null)}>Close</button>
          </div>
          {participantsLoading ? <LoadingSpinner label="Loading event registrations..." /> : participantsError ? <ErrorAlert message={participantsError} onRetry={() => showParticipants(participantsEvent)} /> : !participants.length ? <EmptyState title="No registrations found" detail="Students who register for this event will appear here." /> : <div className="table-responsive">
            {isAdministrator ? <table className="table align-middle mb-0"><thead><tr><th>Event</th><th>Student</th><th>Email</th><th>Phone</th><th>Enrollment number</th><th>Semester</th><th>Department</th><th>Status</th></tr></thead><tbody>
              {participants.map((participant) => <tr key={participant.userId}><td>{participant.eventName || participantsEvent.title}</td><td>{participant.studentFullName || 'Student'}</td><td>{participant.emailAddress || '—'}</td><td>{participant.phoneNumber || '—'}</td><td>{participant.enrollmentNumber || '—'}</td><td>{participant.semester ?? '—'}</td><td>{participant.department || '—'}</td><td><span className="badge text-bg-secondary">{participant.registrationStatus || '—'}</span></td></tr>)}
            </tbody></table> : <table className="table align-middle mb-0"><thead><tr><th>Student</th><th>Department ID</th><th>Semester</th><th>Registered at</th></tr></thead><tbody>
              {participants.map((participant) => <tr key={participant.userId}><td>{participant.participantName || 'Student'}</td><td>{participant.departmentId ?? '—'}</td><td>{participant.semester ?? '—'}</td><td>{displayDate(participant.registeredAt)}</td></tr>)}
            </tbody></table>}
          </div>}
        </div>
      </section>}
    </main>
  )
}

function DateTimeField({ id, name, label, value, onChange }) {
  return <div className="col-12 col-md-6">
    <label className="form-label" htmlFor={id}>{label}</label>
    <input id={id} className="form-control" name={name} type="datetime-local" value={value} onChange={onChange} required />
  </div>
}
