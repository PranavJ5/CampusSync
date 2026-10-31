import { useCallback, useEffect, useState } from 'react'
import api, { getErrorMessage } from '../api/axios'
import { ErrorAlert, LoadingSpinner } from '../components/Feedback'
import TimetableSchedule from '../components/TimetableSchedule'

const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']

const emptyForm = () => ({
  subjectId: '',
  dayOfWeek: 'MONDAY',
  startTime: '',
  endTime: '',
  venue: '',
  academicYear: '2026-27',
  effectiveFrom: '2026-08-10',
})

export default function FacultyTimetablePage() {
  const [subjects, setSubjects] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadSubjects = useCallback(async () => {
    try {
      setSubjects((await api.get('/timetable/faculty/subjects')).data || [])
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Assigned subjects could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void Promise.resolve().then(loadSubjects) }, [loadSubjects])

  const change = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setError('')
    setNotice('')
  }

  const save = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      await api.post('/timetable/faculty', {
        subjectId: Number(form.subjectId),
        departmentId: null,
        semester: null,
        division: null,
        labBatch: null,
        dayOfWeek: form.dayOfWeek,
        startTime: form.startTime,
        endTime: form.endTime,
        venue: form.venue,
        classType: 'LECTURE',
        label: null,
        academicYear: form.academicYear,
        effectiveFrom: form.effectiveFrom,
        active: true,
      })
      setNotice('Lecture scheduled successfully.')
      setForm(emptyForm())
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'The lecture could not be scheduled.'))
    } finally {
      setSaving(false)
    }
  }

  return <main className="container-fluid py-4">
    <div className="mb-4">
      <p className="eyebrow mb-1">Faculty workspace</p>
      <h1 className="h2 mb-1">Schedule a lecture</h1>
      <p className="text-secondary mb-0">Only subjects assigned to your faculty profile can be scheduled. Department, semester, and division come from the subject assignment.</p>
    </div>
    {notice && <div className="alert alert-success" role="status">{notice}</div>}
    {error && <ErrorAlert message={error} onRetry={loadSubjects} />}
    <section className="card border-0 shadow-sm mb-4">
      <div className="card-body p-3 p-md-4">
        {loading ? <LoadingSpinner label="Loading your assigned subjects..." /> :
          <form onSubmit={save}>
            <div className="row g-3">
              <div className="col-12">
                <label className="form-label" htmlFor="faculty-timetable-subject">Assigned subject</label>
                <select id="faculty-timetable-subject" className="form-select" name="subjectId" value={form.subjectId} onChange={change} required>
                  <option value="">Choose a subject</option>
                  {subjects.map((subject) => <option key={subject.id} value={subject.id}>
                    {subject.code} · {subject.name} · Semester {subject.semester ?? '—'} · Division {subject.division || '—'}
                  </option>)}
                </select>
                {!subjects.length && <p className="small text-warning mt-2 mb-0">No subjects are assigned to your faculty profile.</p>}
              </div>
              <div className="col-12 col-md-3">
                <label className="form-label" htmlFor="faculty-timetable-day">Day</label>
                <select id="faculty-timetable-day" className="form-select" name="dayOfWeek" value={form.dayOfWeek} onChange={change}>{weekdays.map((day) => <option key={day} value={day}>{day.charAt(0) + day.slice(1).toLowerCase()}</option>)}</select>
              </div>
              <div className="col-6 col-md-3"><label className="form-label" htmlFor="faculty-timetable-start">Start time</label><input id="faculty-timetable-start" className="form-control" name="startTime" type="time" value={form.startTime} onChange={change} required /></div>
              <div className="col-6 col-md-3"><label className="form-label" htmlFor="faculty-timetable-end">End time</label><input id="faculty-timetable-end" className="form-control" name="endTime" type="time" value={form.endTime} onChange={change} required /></div>
              <div className="col-12 col-md-3"><label className="form-label" htmlFor="faculty-timetable-venue">Room number</label><input id="faculty-timetable-venue" className="form-control" name="venue" value={form.venue} onChange={change} maxLength="255" required /></div>
              <div className="col-6 col-md-3"><label className="form-label" htmlFor="faculty-timetable-year">Academic year</label><input id="faculty-timetable-year" className="form-control" name="academicYear" value={form.academicYear} onChange={change} maxLength="20" required /></div>
              <div className="col-6 col-md-3"><label className="form-label" htmlFor="faculty-timetable-effective">Effective from</label><input id="faculty-timetable-effective" className="form-control" name="effectiveFrom" type="date" value={form.effectiveFrom} onChange={change} required /></div>
              <div className="col-12"><button className="btn btn-primary" type="submit" disabled={saving || loading || !subjects.length}>{saving ? 'Scheduling...' : 'Schedule lecture'}</button></div>
            </div>
          </form>}
      </div>
    </section>
    <TimetableSchedule role="faculty" />
  </main>
}
