import { useCallback, useEffect, useMemo, useState } from 'react'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList } from '../utils/data'

const emptyForm = () => ({
  classType: 'LECTURE',
  subjectId: '',
  departmentId: '',
  semester: '5',
  division: 'B',
  labBatch: '',
  dayOfWeek: 'MONDAY',
  startTime: '',
  endTime: '',
  venue: '',
  label: '',
  academicYear: '2026-27',
  effectiveFrom: '2026-08-10',
  active: true,
})

const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']

export default function TimetableManagementPage() {
  const [entries, setEntries] = useState([])
  const [departments, setDepartments] = useState([])
  const [subjects, setSubjects] = useState([])
  const [facultyProfiles, setFacultyProfiles] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [batchGroup, setBatchGroup] = useState({ departmentId: '', semester: '5', division: 'B' })
  const [batchStudents, setBatchStudents] = useState([])
  const [batchValues, setBatchValues] = useState({})
  const [batchLoading, setBatchLoading] = useState(false)
  const [batchSavingId, setBatchSavingId] = useState(null)
  const [batchError, setBatchError] = useState('')
  const [batchNotice, setBatchNotice] = useState('')

  const load = useCallback(async () => {
    try {
      const [entryResponse, departmentResponse, subjectResponse, facultyResponse] = await Promise.all([
        api.get('/timetable/management'),
        api.get('/departments'),
        api.get('/subjects'),
        api.get('/faculty'),
      ])
      setEntries(asList(entryResponse.data))
      setDepartments(asList(departmentResponse.data))
      setSubjects(asList(subjectResponse.data))
      setFacultyProfiles(asList(facultyResponse.data))
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Timetable management data could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void Promise.resolve().then(load) }, [load])

  const filteredSubjects = useMemo(() => subjects
    .filter((subject) => subject.facultyId)
    .filter((subject) => !form.departmentId || String(subject.departmentId) === String(form.departmentId)),
  [subjects, form.departmentId])

  const changeField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setError('')
  }

  const changeBatchGroup = (name, value) => {
    setBatchGroup((current) => ({ ...current, [name]: value }))
    setBatchStudents([])
    setBatchValues({})
    setBatchError('')
    setBatchNotice('')
  }

  const edit = (entry) => {
    setEditingId(entry.id)
    setForm({
      classType: entry.classType,
      subjectId: entry.subjectId ? String(entry.subjectId) : '',
      departmentId: String(entry.departmentId),
      semester: String(entry.semester),
      division: entry.division,
      labBatch: entry.labBatch || '',
      dayOfWeek: entry.dayOfWeek,
      startTime: String(entry.startTime).slice(0, 5),
      endTime: String(entry.endTime).slice(0, 5),
      venue: entry.venue || '',
      label: entry.label || '',
      academicYear: entry.academicYear,
      effectiveFrom: entry.effectiveFrom,
      active: entry.active,
    })
    setNotice('')
  }

  const cancelEdit = () => {
    setEditingId(null)
    setForm(emptyForm())
    setError('')
  }

  const save = async (event) => {
    event.preventDefault()
    setError('')
    setNotice('')
    setSaving(true)
    const payload = {
      ...form,
      subjectId: form.classType === 'BREAK' ? null : Number(form.subjectId),
      departmentId: form.classType === 'BREAK' ? Number(form.departmentId) : null,
      semester: Number(form.semester),
      labBatch: form.classType === 'LAB' ? form.labBatch || null : null,
    }
    try {
      if (editingId) {
        await api.put(`/timetable/management/${editingId}`, payload)
        setNotice('Timetable entry updated.')
      } else {
        await api.post('/timetable/management', payload)
        setNotice('Timetable entry added.')
      }
      cancelEdit()
      await load()
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Timetable entry could not be saved.'))
    } finally {
      setSaving(false)
    }
  }

  const updateActive = async (entry) => {
    const payload = {
      subjectId: entry.subjectId,
      departmentId: entry.subjectId ? null : entry.departmentId,
      semester: entry.semester,
      division: entry.division,
      labBatch: entry.labBatch || null,
      dayOfWeek: entry.dayOfWeek,
      startTime: entry.startTime,
      endTime: entry.endTime,
      venue: entry.venue,
      classType: entry.classType,
      label: entry.label,
      academicYear: entry.academicYear,
      effectiveFrom: entry.effectiveFrom,
      active: !entry.active,
    }
    setError('')
    try {
      await api.put(`/timetable/management/${entry.id}`, payload)
      setNotice(`Timetable entry ${entry.active ? 'deactivated' : 'activated'}.`)
      await load()
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Timetable entry status could not be changed.'))
    }
  }

  const loadBatchStudents = async () => {
    setBatchLoading(true)
    setBatchError('')
    setBatchNotice('')
    setBatchStudents([])
    setBatchValues({})
    try {
      if (!batchGroup.departmentId || !batchGroup.semester || !batchGroup.division.trim()) {
        setBatchError('Choose a department, semester, and division before loading students.')
        return
      }
      let page = 0
      let totalPages = 1
      const allStudents = []
      while (page < totalPages) {
        const response = await api.get('/students', {
          params: {
            departmentId: Number(batchGroup.departmentId),
            semester: Number(batchGroup.semester),
            page,
            size: 25,
          },
        })
        const pageData = response.data || {}
        allStudents.push(...asList(pageData.content))
        totalPages = Math.max(1, Number(pageData.totalPages) || 1)
        page += 1
      }
      const divisionStudents = allStudents
        .filter((student) => String(student.division || '').toUpperCase() === batchGroup.division.trim().toUpperCase())
        .filter((student) => String(student.academicStatus || '').toUpperCase() === 'ACTIVE')
      setBatchStudents(divisionStudents)
      setBatchValues(Object.fromEntries(divisionStudents.map((student) => [student.id, student.labBatch || ''])))
    } catch (requestError) {
      setBatchError(getErrorMessage(requestError, 'Students for this division could not be loaded.'))
    } finally {
      setBatchLoading(false)
    }
  }

  const saveStudentBatch = async (studentId) => {
    setBatchSavingId(studentId)
    setBatchError('')
    setBatchNotice('')
    try {
      const response = await api.patch(`/students/${studentId}/lab-batch`, {
        labBatch: batchValues[studentId] || null,
      })
      setBatchStudents((current) => current.map((student) => student.id === studentId
        ? { ...student, labBatch: response.data.labBatch }
        : student))
      setBatchNotice('Student lab batch assignment saved.')
    } catch (requestError) {
      setBatchError(getErrorMessage(requestError, 'Student lab batch could not be saved.'))
    } finally {
      setBatchSavingId(null)
    }
  }

  return <main className="container page-content">
    <div className="mb-4">
      <h1 className="h2 mb-1">Timetable management</h1>
      <p className="text-secondary mb-0">Manage Monday–Friday schedules. Subject faculty, department, semester, and division come from the assigned subject profile.</p>
    </div>
    {notice && <div className="alert alert-success" role="status">{notice}</div>}
    {error && <ErrorAlert message={error} onRetry={load} />}

    <section className="card border-0 shadow-sm mb-4">
      <div className="card-body p-3 p-md-4">
        <div className="d-flex justify-content-between gap-3 mb-3">
          <div><h2 className="h5 mb-1">{editingId ? 'Edit timetable entry' : 'Add timetable entry'}</h2>
            <p className="small text-secondary mb-0">Schedule data is separate from attendance. Attendance sessions are created only when faculty records a conducted class.</p></div>
          {editingId && <button type="button" className="btn btn-sm btn-outline-secondary align-self-start" onClick={cancelEdit}>Cancel</button>}
        </div>
        <form onSubmit={save}>
          <div className="row g-3">
            <div className="col-12 col-md-4"><label className="form-label" htmlFor="timetable-type">Class type</label>
              <select id="timetable-type" className="form-select" name="classType" value={form.classType} onChange={(event) => {
                changeField(event)
                if (event.target.value === 'BREAK') setForm((current) => ({ ...current, classType: 'BREAK', subjectId: '' }))
              }}>
                <option value="LECTURE">Lecture</option><option value="LAB">Lab</option><option value="BREAK">Break</option>
              </select></div>
            {form.classType === 'LAB' && <div className="col-12 col-md-4"><label className="form-label" htmlFor="timetable-batch">Lab batch</label>
                <select id="timetable-batch" className="form-select" name="labBatch" value={form.labBatch} onChange={changeField}>
                  <option value="">All of division / no batch split</option>
                  {['B1', 'B2', 'B3', 'B4'].map((batch) => <option key={batch} value={batch}>{batch}</option>)}
                </select></div>}
            {form.classType === 'BREAK' ? <>
              <div className="col-12 col-md-4"><label className="form-label" htmlFor="timetable-department">Department</label><select id="timetable-department" className="form-select" name="departmentId" value={form.departmentId} onChange={changeField} required><option value="">Select department</option>{departments.filter((department) => department.active).map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}</select></div>
              <div className="col-6 col-md-2"><label className="form-label" htmlFor="timetable-semester">Semester</label><input id="timetable-semester" className="form-control" name="semester" type="number" min="1" max="8" value={form.semester} onChange={changeField} required /></div>
              <div className="col-6 col-md-2"><label className="form-label" htmlFor="timetable-division">Division</label><input id="timetable-division" className="form-control" name="division" value={form.division} onChange={changeField} maxLength="20" required /></div>
              <div className="col-12"><label className="form-label" htmlFor="timetable-label">Break label</label><input id="timetable-label" className="form-control" name="label" value={form.label} onChange={changeField} maxLength="150" required placeholder="e.g. Lunch break" /></div>
            </> : <div className="col-12 col-md-8"><label className="form-label" htmlFor="timetable-subject">Assigned subject</label><select id="timetable-subject" className="form-select" name="subjectId" value={form.subjectId} onChange={(event) => {
              const subject = subjects.find((item) => String(item.id) === event.target.value)
              setForm((current) => ({
                ...current,
                subjectId: event.target.value,
                departmentId: subject ? String(subject.departmentId) : '',
                semester: subject?.semester ? String(subject.semester) : current.semester,
                division: subject?.division || current.division,
              }))
            }} required><option value="">Select assigned subject</option>{filteredSubjects.map((subject) => {
              const assignedFaculty = facultyProfiles.find((item) => String(item.id) === String(subject.facultyId))
              return <option key={subject.id} value={subject.id}>{subject.code} · {subject.name} · {assignedFaculty?.fullName || `Faculty profile ${subject.facultyId}`} (ID {subject.facultyId}) · Semester {subject.semester ?? '—'} · Division {subject.division || '—'}</option>
            })}</select>{!filteredSubjects.length && <p className="small text-warning mt-2 mb-0">No faculty-assigned subjects are available for this department. Check existing subject allocations before adding entries.</p>}</div>}
            <div className="col-6 col-md-3"><label className="form-label" htmlFor="timetable-day">Day</label><select id="timetable-day" className="form-select" name="dayOfWeek" value={form.dayOfWeek} onChange={changeField}>{weekdays.map((day) => <option key={day} value={day}>{day.charAt(0) + day.slice(1).toLowerCase()}</option>)}</select></div>
            <div className="col-6 col-md-3"><label className="form-label" htmlFor="timetable-venue">Room / venue</label><input id="timetable-venue" className="form-control" name="venue" value={form.venue} onChange={changeField} maxLength="255" required={form.classType !== 'BREAK'} /></div>
            <div className="col-6 col-md-3"><label className="form-label" htmlFor="timetable-start">Start time</label><input id="timetable-start" className="form-control" name="startTime" type="time" value={form.startTime} onChange={changeField} required /></div>
            <div className="col-6 col-md-3"><label className="form-label" htmlFor="timetable-end">End time</label><input id="timetable-end" className="form-control" name="endTime" type="time" value={form.endTime} onChange={changeField} required /></div>
            <div className="col-6 col-md-3"><label className="form-label" htmlFor="timetable-year">Academic year</label><input id="timetable-year" className="form-control" name="academicYear" value={form.academicYear} onChange={changeField} maxLength="20" required /></div>
            <div className="col-6 col-md-3"><label className="form-label" htmlFor="timetable-effective">Effective from</label><input id="timetable-effective" className="form-control" name="effectiveFrom" type="date" value={form.effectiveFrom} onChange={changeField} required /></div>
            {editingId && <div className="col-12"><div className="form-check"><input id="timetable-active" className="form-check-input" type="checkbox" checked={Boolean(form.active)} onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))} /><label className="form-check-label" htmlFor="timetable-active">Active</label></div></div>}
            <div className="col-12"><button className="btn btn-primary" type="submit" disabled={saving || loading}>{saving ? 'Saving...' : editingId ? 'Save entry' : 'Add entry'}</button></div>
          </div>
        </form>
      </div>
    </section>

    <section className="card border-0 shadow-sm mb-4" aria-labelledby="faculty-profiles-heading">
      <div className="card-body p-3 p-md-4">
        <h2 id="faculty-profiles-heading" className="h5 mb-1">Existing faculty profiles</h2>
        <p className="small text-secondary">Use these verified profile IDs when checking subject assignments. Only names and profile IDs are shown here.</p>
        {!facultyProfiles.length ? <p className="small text-secondary mb-0">No faculty profiles are available to this administrator account.</p> :
          <div className="table-responsive"><table className="table table-sm mb-0">
            <thead><tr><th>Profile ID</th><th>Faculty name</th></tr></thead>
            <tbody>{facultyProfiles.map((profile) => <tr key={profile.id}>
              <td>{profile.id}</td><td>{profile.fullName}</td>
            </tr>)}</tbody>
          </table></div>}
      </div>
    </section>

    <section className="card border-0 shadow-sm mb-4" aria-labelledby="student-lab-batches-heading">
      <div className="card-body p-3 p-md-4">
        <h2 id="student-lab-batches-heading" className="h5 mb-1">Student lab-batch assignments</h2>
        <p className="small text-secondary">Assign each active student in the selected division to B1–B4 so student timetables and scheduled attendance rosters show only their own lab sessions.</p>
        {batchError && <ErrorAlert message={batchError} onRetry={loadBatchStudents} />}
        {batchNotice && <div className="alert alert-success" role="status">{batchNotice}</div>}
        <div className="row g-3 align-items-end mb-3">
          <div className="col-12 col-md-4"><label className="form-label" htmlFor="batch-department">Department</label>
            <select id="batch-department" className="form-select" value={batchGroup.departmentId}
              onChange={(event) => changeBatchGroup('departmentId', event.target.value)}>
              <option value="">Select department</option>{departments.filter((department) => department.active).map((department) =>
                <option key={department.id} value={department.id}>{department.name}</option>)}
            </select></div>
          <div className="col-6 col-md-2"><label className="form-label" htmlFor="batch-semester">Semester</label>
            <input id="batch-semester" className="form-control" type="number" min="1" max="8" value={batchGroup.semester}
              onChange={(event) => changeBatchGroup('semester', event.target.value)} /></div>
          <div className="col-6 col-md-2"><label className="form-label" htmlFor="batch-division">Division</label>
            <input id="batch-division" className="form-control" maxLength="20" value={batchGroup.division}
              onChange={(event) => changeBatchGroup('division', event.target.value)} /></div>
          <div className="col-12 col-md-4"><button className="btn btn-outline-primary" type="button" onClick={loadBatchStudents} disabled={batchLoading}>
            {batchLoading ? 'Loading students...' : 'Load active students'}</button></div>
        </div>
        {batchLoading ? <p className="small text-secondary mb-0">Loading student profiles…</p> : batchStudents.length === 0
          ? <p className="small text-secondary mb-0">Load students to view existing profiles for the selected group. No student records are created here.</p>
          : <div className="table-responsive"><table className="table table-sm align-middle mb-0">
            <thead><tr><th>Student</th><th>Enrollment number</th><th>Lab batch</th><th /></tr></thead>
            <tbody>{batchStudents.map((student) => <tr key={student.id}>
              <td>{student.firstName} {student.lastName}</td><td>{student.enrollmentNumber}</td>
              <td><select className="form-select form-select-sm" aria-label={`Lab batch for ${student.firstName} ${student.lastName}`}
                value={batchValues[student.id] || ''} onChange={(event) => setBatchValues((current) => ({ ...current, [student.id]: event.target.value }))}>
                <option value="">Unassigned</option>{['B1', 'B2', 'B3', 'B4'].map((batch) => <option key={batch} value={batch}>{batch}</option>)}
              </select></td>
              <td><button className="btn btn-sm btn-outline-primary" type="button" disabled={batchSavingId === student.id}
                onClick={() => saveStudentBatch(student.id)}>{batchSavingId === student.id ? 'Saving...' : 'Save'}</button></td>
            </tr>)}</tbody>
          </table></div>}
      </div>
    </section>

    <section aria-labelledby="timetable-entries-heading">
      <h2 id="timetable-entries-heading" className="h5 mb-3">Configured timetable entries</h2>
      {loading ? <LoadingSpinner label="Loading timetable entries..." /> : error ? null : !entries.length
        ? <EmptyState title="No timetable entries" detail="Add verified timetable entries above. No schedule data has been generated automatically." />
        : <div className="table-responsive card border-0 shadow-sm"><table className="table align-middle mb-0">
          <thead><tr><th>Day / Time</th><th>Subject / Activity</th><th>Faculty</th><th>Department / Group</th><th>Venue</th><th>Effective</th><th>Status</th><th /></tr></thead>
          <tbody>{entries.map((entry) => <tr key={entry.id}>
            <td>{entry.dayOfWeek}<span className="d-block small text-secondary">{String(entry.startTime).slice(0, 5)}–{String(entry.endTime).slice(0, 5)}</span></td>
            <td>{entry.subjectCode ? `${entry.subjectCode} · ${entry.subjectName}` : entry.label || entry.classType}<span className="d-block small text-secondary">{entry.classType} · AY {entry.academicYear}</span></td>
            <td>{entry.facultyName ? `${entry.facultyName} · ID ${entry.facultyId}` : '—'}</td>
            <td>{entry.departmentName}<span className="d-block small text-secondary">Semester {entry.semester} · Division {entry.division}{entry.labBatch ? ` · Batch ${entry.labBatch}` : ''}</span></td>
            <td>{entry.venue || '—'}</td><td>{entry.effectiveFrom}</td>
            <td><span className={`badge ${entry.active ? 'text-bg-success' : 'text-bg-secondary'}`}>{entry.active ? 'Active' : 'Inactive'}</span></td>
            <td className="text-nowrap"><button className="btn btn-sm btn-outline-primary me-2" onClick={() => edit(entry)}>Edit</button><button className="btn btn-sm btn-outline-secondary" onClick={() => updateActive(entry)}>{entry.active ? 'Deactivate' : 'Activate'}</button></td>
          </tr>)}</tbody>
        </table></div>}
    </section>
  </main>
}
