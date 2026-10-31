import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList, displayDate } from '../utils/data'

const today = () => {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
const dateForWeekday = (weekday) => {
  const date = new Date()
  const weekdayNumber = { MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3, THURSDAY: 4, FRIDAY: 5 }[weekday]
  if (!weekdayNumber) return today()
  date.setDate(date.getDate() + ((weekdayNumber - date.getDay() + 7) % 7))
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export default function FacultyAttendancePage() {
  const [searchParams] = useSearchParams()
  const [subjects, setSubjects] = useState([])
  const [timetableEntries, setTimetableEntries] = useState([])
  const [timetableEntryId, setTimetableEntryId] = useState(() => searchParams.get('timetableEntryId') || '')
  const [subjectId, setSubjectId] = useState('')
  const [sessions, setSessions] = useState([])
  const [sessionId, setSessionId] = useState('')
  const [roster, setRoster] = useState([])
  const [corrections, setCorrections] = useState([])
  const [attendance, setAttendance] = useState({})
  const [rosterLoading, setRosterLoading] = useState(false)
  const [sessionDate, setSessionDate] = useState(today)
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const selectedSubject = subjects.find((subject) => String(subject.id) === subjectId)
  const selectedTimetableEntry = timetableEntries.find((entry) => String(entry.id) === timetableEntryId)

  const loadSubjects = useCallback(async () => {
    try {
      const response = await api.get('/attendance/faculty/subjects')
      const list = asList(response.data)
      setSubjects(list)
      setSubjectId((current) => current || (list.length ? String(list[0].id) : ''))
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Assigned subjects could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  const loadTimetable = useCallback(async () => {
    try {
      const response = await api.get('/timetable/faculty/me')
      const list = asList(response.data?.weekly)
      setTimetableEntries(list.filter((entry) => entry.classType !== 'BREAK'))
      const requestedEntry = searchParams.get('timetableEntryId')
      if (requestedEntry) {
        const entry = list.find((item) => String(item.id) === requestedEntry)
        if (entry?.subjectId) {
          setTimetableEntryId(requestedEntry)
          setSubjectId(String(entry.subjectId))
          setSessionDate(dateForWeekday(entry.dayOfWeek))
        }
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Your assigned timetable could not be loaded.'))
    }
  }, [searchParams])

  const loadSessions = useCallback(async () => {
    try {
      const response = await api.get(`/attendance/faculty/subjects/${subjectId}/sessions`)
      const list = asList(response.data)
      setSessions(list)
      setSessionId((current) => current && !list.some((item) => String(item.id) === current) ? '' : current)
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Class sessions could not be loaded.'))
    }
  }, [subjectId])

  const loadRoster = useCallback(async () => {
    if (!sessionId && (!selectedSubject?.semester || !selectedSubject?.division?.trim())) {
      return
    }
    try {
      const response = sessionId
        ? await Promise.all([
          api.get(`/attendance/faculty/sessions/${sessionId}/attendance`),
          api.get(`/attendance/faculty/sessions/${sessionId}/changes`),
        ])
        : [await api.get(`/attendance/faculty/subjects/${subjectId}/students`), { data: [] }]
      const list = asList(response[0].data)
      setRoster(list)
      setAttendance(Object.fromEntries(list.map((student) => [
        student.studentId,
        student.status || '',
      ])))
      setCorrections(asList(response[1].data))
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'The division roster could not be loaded.'))
    } finally {
      setRosterLoading(false)
    }
  }, [subjectId, sessionId, selectedSubject])

  useEffect(() => { void Promise.resolve().then(() => { loadSubjects(); loadTimetable() }) }, [loadSubjects, loadTimetable])
  useEffect(() => {
    if (subjectId) void Promise.resolve().then(loadSessions)
  }, [subjectId, timetableEntryId, loadSessions])
  useEffect(() => {
    if (subjectId) void Promise.resolve().then(loadRoster)
  }, [sessionId, subjectId, timetableEntryId, loadRoster])

  const createSession = async (event) => {
    event.preventDefault()
    setError('')
    setNotice('')
    try {
      const response = selectedTimetableEntry
        ? await api.post(`/attendance/faculty/timetable/${selectedTimetableEntry.id}/sessions`, { sessionDate })
        : await api.post('/attendance/faculty/sessions', {
          subjectId: Number(subjectId),
          sessionDate,
          startTime: startTime || null,
          endTime: endTime || null,
        })
      setSessionId(String(response.data.id))
      setNotice('Class session created for the selected date. Attendance is not marked until you submit the conducted class roster.')
      await loadSessions()
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Class session could not be created.'))
    }
  }

  const saveAttendance = async () => {
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const response = await api.put(`/attendance/faculty/sessions/${sessionId}/attendance`, {
        entries: roster.map((student) => ({
          studentId: student.studentId,
          status: attendance[student.studentId],
        })),
      })
      const list = asList(response.data)
      setRoster(list)
      setAttendance(Object.fromEntries(list.map((student) => [student.studentId, student.status || ''])))
      await loadRoster()
      setNotice('Attendance saved. Any changed marks have been added to the correction history.')
      await loadSessions()
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Attendance could not be saved. Check that every listed student has a status.'))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <LoadingSpinner label="Loading assigned subjects..." />
  const selectedSession = sessions.find((session) => String(session.id) === sessionId)

  return <main className="container page-content">
    <div className="mb-4">
      <h1 className="h2 mb-1">Attendance management</h1>
      <p className="text-secondary mb-0">Record attendance only for subjects assigned to your faculty profile.</p>
    </div>
    {error && <ErrorAlert message={error} onRetry={() => { loadSubjects(); loadSessions(); loadRoster() }} />}
    {notice && <div className="alert alert-success" role="status">{notice}</div>}
    {!subjects.length ? <EmptyState title="No assigned subjects" detail="An administrator must assign a subject, semester, and division to your faculty profile before attendance can be recorded." /> :
      <>
        <section className="card border-0 shadow-sm mb-4">
          <div className="card-body p-3 p-md-4">
            <label className="form-label" htmlFor="attendance-timetable">Scheduled class (optional)</label>
            <select id="attendance-timetable" className="form-select mb-3" value={timetableEntryId} onChange={(event) => {
              const entry = timetableEntries.find((item) => String(item.id) === event.target.value)
              setTimetableEntryId(event.target.value)
              if (entry?.subjectId) {
                setSubjectId(String(entry.subjectId))
                setSessionDate(dateForWeekday(entry.dayOfWeek))
              }
              setSessionId('')
              setSessions([])
              setRoster([])
              setAttendance({})
              setCorrections([])
            }}>
              <option value="">Choose a scheduled class or use a manual session</option>
              {timetableEntries.map((entry) => <option key={entry.id} value={entry.id}>
                {entry.dayOfWeek} · {entry.startTime?.slice(0, 5)} · {entry.subjectCode} {entry.subjectName} · Division {entry.division}{entry.labBatch ? ` · Batch ${entry.labBatch}` : ''}
              </option>)}
            </select>
            <label className="form-label" htmlFor="attendance-subject">Assigned subject and division</label>
            <select id="attendance-subject" className="form-select" value={subjectId} onChange={(event) => {
              setTimetableEntryId('')
              setSubjectId(event.target.value)
              setSessionId('')
              setSessions([])
              setRoster([])
              setAttendance({})
              setCorrections([])
              setError('')
              setNotice('')
            }}>
              {subjects.map((subject) => <option key={subject.id} value={subject.id}>
                {subject.code} · {subject.name} · Semester {subject.semester ?? 'not set'} · Division {subject.division || 'not set'}
              </option>)}
            </select>
            {selectedSubject && (!selectedSubject.semester || !selectedSubject.division) &&
              <p className="small text-danger mt-2 mb-0">This subject needs an administrator-configured semester and division before attendance is available.</p>}
          </div>
        </section>

        <section className="row g-4">
          <div className="col-12 col-lg-5">
            <div className="card border-0 shadow-sm h-100"><div className="card-body p-3 p-md-4">
              <h2 className="h5">{selectedTimetableEntry ? `Create session · ${selectedTimetableEntry.subjectCode}` : 'Create class session'}</h2>
              {selectedTimetableEntry && <p className="small text-secondary">Scheduled {selectedTimetableEntry.dayOfWeek}, {selectedTimetableEntry.startTime?.slice(0, 5)}–{selectedTimetableEntry.endTime?.slice(0, 5)} · Division {selectedTimetableEntry.division}{selectedTimetableEntry.labBatch ? ` · Batch ${selectedTimetableEntry.labBatch}` : ''} · {selectedTimetableEntry.venue || 'Venue not set'}</p>}
              <form onSubmit={createSession}>
                <div className="mb-3">
                  <label className="form-label" htmlFor="attendance-date">Class date</label>
                  <input id="attendance-date" className="form-control" type="date" value={sessionDate} onChange={(event) => setSessionDate(event.target.value)} required />
                </div>
                {!selectedTimetableEntry && <div className="row g-2 mb-3">
                  <div className="col"><label className="form-label" htmlFor="attendance-start">Start time (optional)</label><input id="attendance-start" className="form-control" type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} /></div>
                  <div className="col"><label className="form-label" htmlFor="attendance-end">End time (optional)</label><input id="attendance-end" className="form-control" type="time" value={endTime} onChange={(event) => setEndTime(event.target.value)} /></div>
                </div>}
                <button className="btn btn-primary" type="submit" disabled={!subjectId}>Create session</button>
              </form>
              <hr />
              <h2 className="h6">Previous sessions</h2>
              {!sessions.length ? <p className="text-secondary small mb-0">No sessions recorded for this subject.</p> :
                <div className="list-group">
                  {sessions.map((session) => <button type="button" key={session.id}
                    className={`list-group-item list-group-item-action ${sessionId === String(session.id) ? 'active' : ''}`}
                    onClick={() => setSessionId(String(session.id))}>
                    {displayDate(session.sessionDate, { dateStyle: 'medium' })} · {session.startTime || 'Time not set'}{session.labBatch ? ` · Batch ${session.labBatch}` : ''} · {session.status}
                  </button>)}
                </div>}
            </div></div>
          </div>

          <div className="col-12 col-lg-7">
            <section className="card border-0 shadow-sm h-100" aria-labelledby="attendance-roster-heading">
              <div className="card-body p-3 p-md-4">
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                  <h2 id="attendance-roster-heading" className="h5 mb-0">
                    {selectedSession ? `Attendance · ${displayDate(selectedSession.sessionDate, { dateStyle: 'medium' })}` : 'Active division roster'}
                  </h2>
                  {sessionId && <button type="button" className="btn btn-primary btn-sm" onClick={saveAttendance}
                    disabled={saving || !roster.length || Object.values(attendance).some((status) => !status)}>
                    {saving ? 'Saving...' : 'Save attendance'}
                  </button>}
                </div>
                {rosterLoading ? <div className="d-flex align-items-center gap-2 py-4" role="status"><span className="spinner-border spinner-border-sm" />Loading division roster...</div> :
                  !subjectId ? <EmptyState title="Choose an assigned subject" /> :
                  selectedSubject && (!selectedSubject.semester || !selectedSubject.division) ? <EmptyState title="Subject group is not configured" detail="An administrator must assign a semester and division to this subject." /> :
                  !roster.length ? <EmptyState title="No students in this division" detail="An administrator must assign active students to the subject's department, semester, and division." /> :
                  <div className="table-responsive">
                    <table className="table align-middle">
                      <thead><tr><th scope="col">Student</th><th scope="col">Enrollment</th><th scope="col">{sessionId ? 'Attendance' : 'Saved status'}</th></tr></thead>
                      <tbody>{roster.map((student) => <tr key={student.studentId}>
                        <th scope="row">{student.firstName} {student.lastName}</th>
                        <td>{student.enrollmentNumber}</td>
                        <td>{sessionId ?
                          <><label className="visually-hidden" htmlFor={`attendance-${student.studentId}`}>Attendance for {student.firstName} {student.lastName}</label>
                            <select id={`attendance-${student.studentId}`} className="form-select" value={attendance[student.studentId] || ''}
                              onChange={(event) => setAttendance((current) => ({ ...current, [student.studentId]: event.target.value }))}>
                              <option value="">Choose status</option><option value="PRESENT">Present</option><option value="ABSENT">Absent</option>
                            </select></> :
                          <span className="text-secondary">{student.status || '—'}</span>}
                        </td>
                      </tr>)}</tbody>
                    </table>
                  </div>}
                {!sessionId && roster.length > 0 && <p className="small text-secondary mb-0">This is the active student roster for the selected subject. Select or create a session to view or record marks.</p>}
                {sessionId && <p className="small text-secondary mb-0">Saving requires one mark for every active student in this division. Corrections are audited.</p>}
                {sessionId && corrections.length > 0 && <section className="mt-4" aria-labelledby="attendance-corrections-heading">
                  <h3 id="attendance-corrections-heading" className="h6">Recent correction history</h3>
                  <ul className="list-group list-group-flush">
                    {corrections.slice(0, 10).map((change) => <li className="list-group-item px-0 small" key={change.id}>
                      {change.studentName}: {change.previousStatus || 'Not recorded'} → {change.newStatus}
                      {' · '}{change.changedBy} · {displayDate(change.changedAt)}
                    </li>)}
                  </ul>
                </section>}
              </div>
            </section>
          </div>
        </section>
      </>}
  </main>
}
