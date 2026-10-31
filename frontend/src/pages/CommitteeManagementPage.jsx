import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList } from '../utils/data'

const blankCommittee = () => ({
  departmentId: '',
  code: '',
  name: '',
  description: '',
  category: '',
  establishedYear: '',
  facultyCoordinatorId: '',
  active: true,
})

async function loadStudents() {
  const students = []
  let page = 0
  let last = false
  while (!last) {
    const response = await api.get('/students', { params: { page, size: 25 } })
    students.push(...asList(response.data))
    last = response.data?.last ?? true
    page += 1
  }
  return students
}

export default function CommitteeManagementPage() {
  const [committees, setCommittees] = useState([])
  const [departments, setDepartments] = useState([])
  const [faculty, setFaculty] = useState([])
  const [students, setStudents] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [members, setMembers] = useState([])
  const [history, setHistory] = useState([])
  const [showHistory, setShowHistory] = useState(false)
  const [memberUserId, setMemberUserId] = useState('')
  const [coordinatorUserId, setCoordinatorUserId] = useState('')
  const [form, setForm] = useState(blankCommittee)
  const [editing, setEditing] = useState(false)
  const [loading, setLoading] = useState(true)
  const [membersLoading, setMembersLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [membershipSaving, setMembershipSaving] = useState(false)
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')
  const [notice, setNotice] = useState('')

  const loadCommitteeMembers = useCallback(async (committeeId) => {
    setMembersLoading(true)
    try {
      const response = await api.get(`/committees/${committeeId}/members`)
      setMembers(asList(response.data))
      setHistory([])
      setShowHistory(false)
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Committee memberships could not be loaded.'))
    } finally {
      setMembersLoading(false)
    }
  }, [])

  const load = useCallback(async (preferredId = '') => {
    try {
      const [committeeResponse, departmentResponse, facultyResponse, studentList] = await Promise.all([
        api.get('/committees'),
        api.get('/departments'),
        api.get('/faculty'),
        loadStudents(),
      ])
      const nextCommittees = asList(committeeResponse.data)
      setCommittees(nextCommittees)
      setDepartments(asList(departmentResponse.data))
      setFaculty(asList(facultyResponse.data))
      setStudents(studentList)
      const nextId = nextCommittees.some((item) => String(item.id) === String(preferredId))
        ? String(preferredId)
        : ''
      setSelectedId(nextId)
      if (nextId) await loadCommitteeMembers(nextId)
      else {
        setMembers([])
        setHistory([])
        setShowHistory(false)
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Committee management data could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [loadCommitteeMembers])

  useEffect(() => { void Promise.resolve().then(() => load('')) }, [load])

  const selectedCommittee = committees.find((committee) => String(committee.id) === selectedId)
  const facultyById = useMemo(() => new Map(faculty.map((item) => [String(item.id), item])), [faculty])
  const activeMemberIds = useMemo(() => new Set(members.map((member) => String(member.userId))), [members])
  const selectedFaculty = selectedCommittee
    ? facultyById.get(String(selectedCommittee.facultyCoordinatorId))
    : null
  const eligibleStudents = students.filter((student) =>
    String(student.academicStatus || '').toUpperCase() === 'ACTIVE')

  const startCreate = () => {
    setSelectedId('')
    setEditing(false)
    setForm(blankCommittee())
    setMembers([])
    setHistory([])
    setFormError('')
    setNotice('')
  }

  const selectCommittee = async (committeeId) => {
    setSelectedId(committeeId)
    setEditing(false)
    setFormError('')
    setNotice('')
    if (!committeeId) {
      setForm(blankCommittee())
      setMembers([])
      setHistory([])
      return
    }
    const item = committees.find((committee) => String(committee.id) === committeeId)
    if (item) setForm(formFromCommittee(item))
    await loadCommitteeMembers(committeeId)
  }

  const editCommittee = () => {
    if (selectedCommittee) {
      setForm(formFromCommittee(selectedCommittee))
      setEditing(true)
      setFormError('')
      setNotice('')
    }
  }

  const updateField = ({ target: { name, value, type, checked } }) => {
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setFormError('')
    setNotice('')
  }

  const saveCommittee = async (event) => {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    setNotice('')
    const payload = {
      departmentId: form.departmentId ? Number(form.departmentId) : null,
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description.trim() || null,
      status: form.active ? 'ACTIVE' : 'INACTIVE',
      category: form.category.trim(),
      establishedYear: form.establishedYear ? Number(form.establishedYear) : null,
      facultyCoordinatorId: Number(form.facultyCoordinatorId),
      logoPath: selectedCommittee?.logoPath || null,
      active: form.active,
    }
    try {
      const response = editing
        ? await api.put(`/committees/${selectedId}`, payload)
        : await api.post('/committees', payload)
      const savedId = String(response.data.id)
      setNotice(editing ? 'Committee changes saved.' : 'Committee created.')
      setEditing(false)
      await load(savedId)
    } catch (requestError) {
      setFormError(getErrorMessage(requestError, 'Committee changes could not be saved.'))
    } finally {
      setSaving(false)
    }
  }

  const addMembership = async (event, memberRole) => {
    event.preventDefault()
    const userId = memberRole === 'COORDINATOR' ? coordinatorUserId : memberUserId
    if (!selectedId || !userId) return
    setMembershipSaving(true)
    setError('')
    setNotice('')
    try {
      await api.post(`/committees/${selectedId}/members`, {
        userId: Number(userId),
        memberRole,
      })
      setNotice(memberRole === 'COORDINATOR' ? 'Student coordinator assigned.' : 'Student member added.')
      setCoordinatorUserId('')
      setMemberUserId('')
      await loadCommitteeMembers(selectedId)
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Committee membership could not be saved.'))
    } finally {
      setMembershipSaving(false)
    }
  }

  const removeMembership = async (member) => {
    setError('')
    setNotice('')
    try {
      await api.delete(`/committees/${selectedId}/members/${member.userId}`)
      setNotice(`${member.firstName} ${member.lastName} was removed. Membership history is preserved.`)
      await loadCommitteeMembers(selectedId)
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Committee membership could not be removed.'))
    }
  }

  const toggleHistory = async () => {
    if (showHistory) {
      setShowHistory(false)
      return
    }
    try {
      const response = await api.get(`/committees/${selectedId}/members/history`)
      setHistory(asList(response.data))
      setShowHistory(true)
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Membership history could not be loaded.'))
    }
  }

  if (loading) return <LoadingSpinner label="Loading committee management..." />

  return (
    <main className="container page-content">
      <Link to="/admin" className="small text-decoration-none">← Administrator dashboard</Link>
      <div className="d-flex flex-column flex-md-row justify-content-between gap-3 mt-3 mb-4">
        <div>
          <h1 className="h2 mb-1">Committee management</h1>
          <p className="text-secondary mb-0">Manage committees, faculty coordinators, student coordinators, and membership.</p>
        </div>
        <button className="btn btn-primary align-self-start" type="button" onClick={startCreate}>Create committee</button>
      </div>

      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {error && <ErrorAlert message={error} onRetry={() => load(selectedId)} />}
      {!committees.length && !error && !selectedId && !form.name && (
        <EmptyState title="No committees yet" detail="Create a committee and assign its faculty coordinator to make it available for events." />
      )}

      <div className="row g-4">
        <section className="col-12 col-xl-4" aria-labelledby="committee-list-heading">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 id="committee-list-heading" className="h5 mb-3">Committees</h2>
              {!committees.length ? <p className="text-secondary mb-0">No committee records are available.</p> :
                <div className="list-group">
                  {committees.map((committee) => (
                    <button
                      className={`list-group-item list-group-item-action ${String(committee.id) === selectedId ? 'active' : ''}`}
                      type="button"
                      key={committee.id}
                      onClick={() => selectCommittee(String(committee.id))}
                    >
                      <span className="d-flex justify-content-between gap-2">
                        <strong>{committee.name}</strong>
                        <span className={`badge ${committee.active ? 'text-bg-success' : 'text-bg-secondary'}`}>
                          {committee.active ? 'Active' : 'Inactive'}
                        </span>
                      </span>
                      <span className="d-block small">{committee.code} · {committee.category}</span>
                    </button>
                  ))}
                </div>}
            </div>
          </div>
        </section>

        <section className="col-12 col-xl-8" aria-labelledby="committee-form-heading">
          <div className="card border-0 shadow-sm">
            <div className="card-body p-3 p-md-4">
              <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                <div>
                  <h2 id="committee-form-heading" className="h5 mb-1">
                    {selectedCommittee ? (editing ? 'Edit committee' : selectedCommittee.name) : 'Create committee'}
                  </h2>
                  {selectedCommittee && !editing && <p className="small text-secondary mb-0">{selectedCommittee.code} · {selectedCommittee.category}</p>}
                </div>
                {selectedCommittee && !editing && <button className="btn btn-outline-primary btn-sm" type="button" onClick={editCommittee}>Edit</button>}
              </div>
              {!selectedCommittee || editing ? (
                <form onSubmit={saveCommittee}>
                  <div className="row g-3">
                    <Field name="name" label="Committee name" value={form.name} onChange={updateField} required maxLength={150} />
                    <Field name="code" label="Committee code" value={form.code} onChange={updateField} required maxLength={50} />
                    <Field name="category" label="Category" value={form.category} onChange={updateField} required maxLength={50} />
                    <Field name="establishedYear" label="Established year" type="number" value={form.establishedYear} onChange={updateField} min={1800} max={9999} />
                    <div className="col-12 col-md-6">
                      <label className="form-label" htmlFor="committee-department">Department (optional)</label>
                      <select id="committee-department" className="form-select" name="departmentId" value={form.departmentId} onChange={updateField}>
                        <option value="">No department association</option>
                        {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
                      </select>
                    </div>
                    <div className="col-12 col-md-6">
                      <label className="form-label" htmlFor="committee-faculty-coordinator">Faculty coordinator</label>
                      <select id="committee-faculty-coordinator" className="form-select" name="facultyCoordinatorId" value={form.facultyCoordinatorId} onChange={updateField} required>
                        <option value="">Choose one active faculty coordinator</option>
                        {faculty.filter((item) => String(item.employmentStatus).toUpperCase() === 'ACTIVE').map((item) => (
                          <option key={item.id} value={item.id}>{item.fullName} · {item.institutionalEmail}</option>
                        ))}
                      </select>
                      {!faculty.some((item) => String(item.employmentStatus).toUpperCase() === 'ACTIVE') &&
                        <div className="form-text text-danger">Provision an active faculty profile before creating a committee.</div>}
                    </div>
                    <div className="col-12">
                      <label className="form-label" htmlFor="committee-description">Description</label>
                      <textarea id="committee-description" className="form-control" rows="3" maxLength={65535} name="description" value={form.description} onChange={updateField} />
                    </div>
                    <div className="col-12">
                      <div className="form-check form-switch">
                        <input id="committee-active" className="form-check-input" type="checkbox" name="active" checked={form.active} onChange={updateField} />
                        <label className="form-check-label" htmlFor="committee-active">Committee is active</label>
                      </div>
                    </div>
                    {formError && <div className="col-12"><div className="alert alert-danger mb-0" role="alert">{formError}</div></div>}
                    <div className="col-12 d-flex gap-2">
                      <button className="btn btn-primary" type="submit" disabled={saving || !faculty.length}>{saving ? 'Saving...' : editing ? 'Save changes' : 'Create committee'}</button>
                      {editing && <button className="btn btn-outline-secondary" type="button" onClick={() => { setEditing(false); setForm(formFromCommittee(selectedCommittee)) }}>Cancel</button>}
                    </div>
                  </div>
                </form>
              ) : (
                <div className="row g-3">
                  <Detail label="Status" value={selectedCommittee.active ? 'Active' : 'Inactive'} />
                  <Detail label="Department" value={departments.find((item) => item.id === selectedCommittee.departmentId)?.name || 'Not associated'} />
                  <Detail label="Established year" value={selectedCommittee.establishedYear || 'Not specified'} />
                  <Detail label="Faculty coordinator" value={selectedFaculty?.fullName || selectedCommittee.facultyCoordinatorName || 'Not assigned'} />
                  <div className="col-12"><p className="mb-0 text-secondary">{selectedCommittee.description || 'No description provided.'}</p></div>
                  <div className="col-12 border-top pt-3">
                    <h3 className="h6">Membership and student coordinators</h3>
                    {membersLoading ? <div role="status">Loading memberships...</div> : !members.length ? (
                      <p className="text-secondary mb-0">No active memberships. Add students or assign a student coordinator below.</p>
                    ) : (
                      <div className="table-responsive">
                        <table className="table table-sm align-middle">
                          <thead><tr><th>Member</th><th>Account</th><th>Membership role</th><th>Joined</th><th /></tr></thead>
                          <tbody>{members.map((member) => (
                            <tr key={member.id}>
                              <td>{member.firstName} {member.lastName}</td>
                              <td>{member.username}</td>
                              <td>{member.memberRole === 'COORDINATOR' ? `Coordinator (${member.accountRole})` : member.memberRole}</td>
                              <td>{member.joinedOn || '—'}</td>
                              <td className="text-end">
                                {member.userId !== selectedFaculty?.userId &&
                                  <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => removeMembership(member)}>Remove</button>}
                              </td>
                            </tr>
                          ))}</tbody>
                        </table>
                      </div>
                    )}
                    <button className="btn btn-sm btn-outline-secondary mt-2" type="button" onClick={toggleHistory}>
                      {showHistory ? 'Hide membership history' : 'Show membership history'}
                    </button>
                    {showHistory && <MembershipHistory history={history} />}
                  </div>
                  {selectedCommittee.active && (
                    <>
                      <div className="col-12 col-lg-6">
                        <form onSubmit={(event) => addMembership(event, 'COORDINATOR')}>
                          <label className="form-label" htmlFor="student-coordinator">Assign student coordinator</label>
                          <div className="input-group">
                            <select className="form-select" id="student-coordinator" value={coordinatorUserId} onChange={(event) => setCoordinatorUserId(event.target.value)} required>
                              <option value="">Select a student</option>
                              {eligibleStudents.filter((student) => !activeMemberIds.has(String(student.userId))).map((student) => (
                                <option key={student.userId} value={student.userId}>{student.firstName} {student.lastName} · {student.username}</option>
                              ))}
                            </select>
                            <button className="btn btn-outline-primary" type="submit" disabled={membershipSaving || !coordinatorUserId}>Assign</button>
                          </div>
                        </form>
                      </div>
                      <div className="col-12 col-lg-6">
                        <form onSubmit={(event) => addMembership(event, 'MEMBER')}>
                          <label className="form-label" htmlFor="committee-member">Add student member</label>
                          <div className="input-group">
                            <select className="form-select" id="committee-member" value={memberUserId} onChange={(event) => setMemberUserId(event.target.value)} required>
                              <option value="">Select a student</option>
                              {eligibleStudents.filter((student) => !activeMemberIds.has(String(student.userId))).map((student) => (
                                <option key={student.userId} value={student.userId}>{student.firstName} {student.lastName} · {student.username}</option>
                              ))}
                            </select>
                            <button className="btn btn-outline-primary" type="submit" disabled={membershipSaving || !memberUserId}>Add</button>
                          </div>
                        </form>
                      </div>
                    </>
                  )}
                  {!selectedCommittee.active && <div className="col-12"><div className="alert alert-secondary mb-0">Activate this committee before assigning new members.</div></div>}
                </div>
              )}
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}

function formFromCommittee(committee) {
  return {
    departmentId: committee.departmentId ? String(committee.departmentId) : '',
    code: committee.code || '',
    name: committee.name || '',
    description: committee.description || '',
    category: committee.category || '',
    establishedYear: committee.establishedYear ? String(committee.establishedYear) : '',
    facultyCoordinatorId: committee.facultyCoordinatorId ? String(committee.facultyCoordinatorId) : '',
    active: Boolean(committee.active),
  }
}

function Field({ name, label, type = 'text', value, onChange, ...attributes }) {
  return (
    <div className="col-12 col-md-6">
      <label className="form-label" htmlFor={`committee-${name}`}>{label}</label>
      <input id={`committee-${name}`} className="form-control" name={name} type={type} value={value} onChange={onChange} {...attributes} />
    </div>
  )
}

function Detail({ label, value }) {
  return <div className="col-12 col-md-6"><span className="small text-secondary d-block">{label}</span><span>{value}</span></div>
}

function MembershipHistory({ history }) {
  if (!history.length) return <p className="small text-secondary mt-2 mb-0">No membership history is available.</p>
  return (
    <ul className="list-group list-group-flush mt-2">
      {history.map((member) => (
        <li className="list-group-item px-0" key={member.id}>
          {member.firstName} {member.lastName} · {member.memberRole} · {member.joinedOn || '—'} to {member.leftOn || 'Present'}
          {!member.active && <span className="badge text-bg-secondary ms-2">Inactive</span>}
        </li>
      ))}
    </ul>
  )
}
