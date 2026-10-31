import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from './Feedback'

const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
const dayLabels = { MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday', THURSDAY: 'Thursday', FRIDAY: 'Friday' }
const timeLabel = (time) => time ? String(time).slice(0, 5) : 'Time not set'
const subjectLabel = (entry) => entry.subjectCode
  ? `${entry.subjectCode} · ${entry.subjectName}`
  : entry.label || entry.classType

export default function TimetableSchedule({ role }) {
  const [timetable, setTimetable] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const endpoint = role === 'faculty' ? '/timetable/faculty/me' : '/timetable/student/me'

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setTimetable((await api.get(endpoint)).data)
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Your timetable could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [endpoint])

  useEffect(() => { load() }, [load])

  if (loading) return <LoadingSpinner label="Loading timetable..." />
  if (error) return <ErrorAlert message={error} onRetry={load} />
  if (!timetable?.timetableAvailable) {
    return <>
      {role === 'student' && timetable?.labBatchAssignmentRequired &&
        <div className="alert alert-warning" role="status">Your division has batch-specific lab sessions, but your lab batch is not assigned yet. Contact an administrator.</div>}
      <EmptyState title="No timetable assigned" detail="Your timetable will appear here after an administrator assigns the current schedule to your actual department, semester, division, and (for labs) batch." />
    </>
  }

  const next = timetable.nextClass
  const todayEntries = timetable.today || []
  const weekly = timetable.weekly || []

  return <section className="row g-4" aria-label={`${role} timetable`}>
    {role === 'student' && timetable.labBatchAssignmentRequired &&
      <div className="col-12"><div className="alert alert-warning mb-0" role="status">
        Your division has batch-specific lab sessions, but your lab batch is not assigned yet. Contact an administrator; only division-wide classes are shown until then.
      </div></div>}
    <div className="col-12">
      <article className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column flex-md-row justify-content-between gap-3 align-items-md-center">
          <div>
            <h2 className="h5 mb-1">Next upcoming class</h2>
            {next ? <><strong>{subjectLabel(next)}</strong><p className="small text-secondary mb-0">
              {dayLabels[next.dayOfWeek] || next.dayOfWeek} · {timeLabel(next.startTime)}–{timeLabel(next.endTime)}
              {role === 'faculty' ? ` · Division ${next.division}` : ''}
              {next.labBatch ? ` · Batch ${next.labBatch}` : ''}
              {next.facultyName ? ` · ${next.facultyName}` : ''}
              {next.venue ? ` · ${next.venue}` : ''}
              {timetable.nextClassStartsAt ? ` · ${new Date(timetable.nextClassStartsAt).toLocaleString()}` : ''}
            </p></> : <p className="text-secondary mb-0">No upcoming lecture or lab is scheduled.</p>}
          </div>
          {role === 'faculty' && next && next.classType !== 'BREAK' &&
            <Link className="btn btn-primary" to={`/faculty/attendance?timetableEntryId=${next.id}`}>Open attendance</Link>}
        </div>
      </article>
    </div>
    <div className="col-12 col-xl-5">
      <section className="card border-0 shadow-sm h-100" aria-labelledby={`${role}-today-heading`}>
        <div className="card-body p-3 p-md-4">
          <h2 id={`${role}-today-heading`} className="h5 mb-3">Today's timetable</h2>
          {!todayEntries.length ? <EmptyState title="No classes today" detail="There are no timetable entries assigned for today." /> :
            <div className="list-group list-group-flush">
              {todayEntries.map((entry) => <TimetableRow key={entry.id} entry={entry} role={role} />)}
            </div>}
        </div>
      </section>
    </div>
    <div className="col-12 col-xl-7">
      <section className="card border-0 shadow-sm h-100" aria-labelledby={`${role}-week-heading`}>
        <div className="card-body p-3 p-md-4">
          <h2 id={`${role}-week-heading`} className="h5 mb-3">Weekly timetable</h2>
          <div className="d-grid gap-3">
            {weekdays.map((day) => {
              const entries = weekly.filter((entry) => entry.dayOfWeek === day)
              return <section key={day} aria-label={`${dayLabels[day]} classes`}>
                <h3 className="h6 border-bottom pb-2">{dayLabels[day]}</h3>
                {!entries.length ? <p className="small text-secondary mb-2">No classes</p> :
                  <div className="list-group list-group-flush">
                    {entries.map((entry) => <TimetableRow key={entry.id} entry={entry} role={role} />)}
                  </div>}
              </section>
            })}
          </div>
        </div>
      </section>
    </div>
  </section>
}

function TimetableRow({ entry, role }) {
  return <article className="list-group-item px-0 py-2">
    <div className="d-flex flex-column flex-md-row justify-content-between gap-2">
      <div>
        <strong>{timeLabel(entry.startTime)}–{timeLabel(entry.endTime)} · {subjectLabel(entry)}</strong>
        <p className="small text-secondary mb-0">
          {entry.classType}
          {role === 'faculty' ? ` · Division ${entry.division}` : ''}
          {entry.labBatch ? ` · Batch ${entry.labBatch}` : ''}
          {entry.facultyName ? ` · ${entry.facultyName}` : ''}
          {entry.venue ? ` · ${entry.venue}` : ''}
        </p>
      </div>
      {role === 'faculty' && entry.classType !== 'BREAK' &&
        <Link className="btn btn-sm btn-outline-primary align-self-start" to={`/faculty/attendance?timetableEntryId=${entry.id}`}>Attendance</Link>}
    </div>
  </article>
}
