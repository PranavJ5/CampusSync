import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { asList } from '../utils/data'

const emptyForm = {
  username: '',
  email: '',
  password: '',
  firstName: '',
  lastName: '',
  departmentId: '',
  employeeNumber: '',
  designation: '',
  employmentStatus: 'ACTIVE',
  qualification: '',
  contactNumber: '',
  dateOfJoining: '',
}

export default function FacultyProvisioningPage() {
  const [form, setForm] = useState(emptyForm)
  const [departments, setDepartments] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadDepartments = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const response = await api.get('/departments')
      setDepartments(asList(response.data).filter((department) => department.active))
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Departments could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    api.get('/departments')
      .then((response) => {
        if (active) setDepartments(asList(response.data).filter((department) => department.active))
      })
      .catch((requestError) => {
        if (active) setError(getErrorMessage(requestError, 'Departments could not be loaded.'))
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const update = ({ target: { name, value } }) => {
    setForm((current) => ({ ...current, [name]: value }))
    setError('')
    setNotice('')
  }

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')
    try {
      const response = await api.post('/faculty/provision', {
        ...form,
        departmentId: Number(form.departmentId),
        dateOfJoining: form.dateOfJoining || null,
        contactNumber: form.contactNumber || null,
        qualification: form.qualification || null,
      })
      setNotice(`Faculty account ${response.data.institutionalEmail} was created. Its password is not returned; deliver the initial credential only through an approved secure channel.`)
      setForm(emptyForm)
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Faculty account could not be provisioned.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="container page-content">
      <Link to="/admin" className="small text-decoration-none">Administrator dashboard</Link>
      <div className="mb-4 mt-3">
        <h1 className="h2 mb-1">Provision a faculty account</h1>
        <p className="text-secondary mb-0">Creates a FACULTY login and its department profile. Only administrators can provision faculty.</p>
      </div>
      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      {error && !loading && !departments.length && <button className="btn btn-outline-primary mb-3" type="button" onClick={loadDepartments}>Retry loading departments</button>}
      <section className="card border-0 shadow-sm">
        <div className="card-body p-3 p-md-4">
          {loading ? <div className="d-flex align-items-center gap-2" role="status"><span className="spinner-border spinner-border-sm" />Loading departments...</div> :
            <form onSubmit={submit}>
              <div className="row g-3">
                <Field name="firstName" label="First name" value={form.firstName} onChange={update} />
                <Field name="lastName" label="Last name" value={form.lastName} onChange={update} />
                <Field name="username" label="Username" value={form.username} onChange={update} />
                <Field name="email" label="Institutional email" type="email" value={form.email} onChange={update} />
                <Field name="password" label="Initial password (8–72 characters)" type="password" minLength="8" maxLength="72" value={form.password} onChange={update} />
                <div className="col-12 col-md-6">
                  <label className="form-label" htmlFor="faculty-department">Department</label>
                  <select className="form-select" id="faculty-department" name="departmentId" value={form.departmentId} onChange={update} required disabled={!departments.length}>
                    <option value="">Choose an active department</option>
                    {departments.map((department) => <option key={department.id} value={department.id}>{department.name} ({department.code})</option>)}
                  </select>
                </div>
                <Field name="employeeNumber" label="Employee number" value={form.employeeNumber} onChange={update} />
                <Field name="designation" label="Designation" value={form.designation} onChange={update} />
                <Field name="employmentStatus" label="Employment status" value={form.employmentStatus} onChange={update} />
                <Field name="qualification" label="Qualification (optional)" value={form.qualification} onChange={update} required={false} />
                <Field name="contactNumber" label="Contact number (optional)" type="tel" value={form.contactNumber} onChange={update} required={false} />
                <Field name="dateOfJoining" label="Date of joining (optional)" type="date" value={form.dateOfJoining} onChange={update} required={false} />
                {!departments.length && <div className="col-12"><div className="alert alert-warning mb-0">No active departments are available.</div></div>}
                <div className="col-12">
                  <p className="small text-secondary">The password is BCrypt-hashed and never returned. Use HTTPS and communicate initial credentials out of band. After provisioning, an administrator can assign subjects and student divisions through the protected subject and student APIs.</p>
                  <button className="btn btn-primary" type="submit" disabled={saving || !departments.length}>{saving ? 'Provisioning...' : 'Create faculty account'}</button>
                </div>
              </div>
            </form>}
        </div>
      </section>
    </main>
  )
}

function Field({ name, label, type = 'text', value, onChange, required = true, ...attributes }) {
  return <div className="col-12 col-md-6">
    <label className="form-label" htmlFor={`faculty-${name}`}>{label}</label>
    <input id={`faculty-${name}`} className="form-control" name={name} type={type} value={value} onChange={onChange} required={required} {...attributes} />
  </div>
}
