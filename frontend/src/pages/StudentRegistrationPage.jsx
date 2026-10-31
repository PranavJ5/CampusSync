import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { asList } from '../utils/data'

const initialForm = {
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  enrollmentNumber: '',
  departmentId: '',
  currentSemester: '',
  admissionYear: String(new Date().getFullYear()),
}

export default function StudentRegistrationPage() {
  const [form, setForm] = useState(initialForm)
  const [departments, setDepartments] = useState([])
  const [loadingDepartments, setLoadingDepartments] = useState(true)
  const [departmentError, setDepartmentError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const navigate = useNavigate()

  const loadDepartments = (isActive = () => true) => {
    return api.get('/public/departments')
      .then((response) => { if (isActive()) setDepartments(asList(response.data)) })
      .catch((requestError) => {
        if (isActive()) setDepartmentError(getErrorMessage(requestError, 'Departments could not be loaded.'))
      })
      .finally(() => { if (isActive()) setLoadingDepartments(false) })
  }

  const retryDepartments = () => {
    setLoadingDepartments(true)
    setDepartmentError('')
    loadDepartments()
  }

  useEffect(() => {
    let active = true
    loadDepartments(() => active)
    return () => { active = false }
  }, [])

  const update = ({ target: { name, value } }) => {
    setForm((current) => ({ ...current, [name]: value }))
    setError('')
    setSuccess('')
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)
    try {
      await api.post('/auth/register', {
        ...form,
        departmentId: Number(form.departmentId),
        currentSemester: Number(form.currentSemester),
        admissionYear: Number(form.admissionYear),
      })
      setSuccess('Student account created. Sign in with your new email and password.')
      setForm(initialForm)
      navigate('/login/student', { replace: true, state: { notice: 'Account created. You can now sign in.' } })
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Your account could not be created. Check the form and try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login-page d-flex align-items-center py-4">
      <section className="card shadow-sm border-0 login-card mx-auto w-100">
        <div className="card-body p-4 p-md-5">
          <Link to="/login" className="small text-decoration-none">← Back to sign-in options</Link>
          <h1 className="h2 mt-3 mb-2">Create a student account</h1>
          <p className="text-secondary mb-4">Account access is assigned by CampusSync. Administrator registration is not available here.</p>
          {error && <div className="alert alert-danger" role="alert">{error}</div>}
          {success && <div className="alert alert-success" role="status">{success}</div>}
          <form onSubmit={submit}>
            <div className="row g-3">
              <Field name="firstName" label="First name" value={form.firstName} onChange={update} autoComplete="given-name" />
              <Field name="lastName" label="Last name" value={form.lastName} onChange={update} autoComplete="family-name" />
              <Field name="username" label="Username" value={form.username} onChange={update} autoComplete="username" />
              <Field name="email" label="Email address" type="email" value={form.email} onChange={update} autoComplete="email" />
              <Field name="password" label="Password (8–72 characters)" type="password" value={form.password} onChange={update} autoComplete="new-password" minLength="8" maxLength="72" />
              <Field name="enrollmentNumber" label="Enrollment number" value={form.enrollmentNumber} onChange={update} />
              <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="signup-department">Department</label>
                <select id="signup-department" className="form-select" name="departmentId" value={form.departmentId} onChange={update} required disabled={loadingDepartments || !departments.length}>
                  <option value="">{loadingDepartments ? 'Loading departments...' : 'Choose a department'}</option>
                  {departments.map((department) => <option key={department.id} value={department.id}>{department.name} ({department.code})</option>)}
                </select>
                {departmentError && <div className="small text-danger mt-2" role="alert">{departmentError} <button type="button" className="btn btn-link btn-sm p-0 align-baseline" onClick={retryDepartments}>Retry</button></div>}
                {!loadingDepartments && !departmentError && !departments.length && <div className="small text-warning mt-2" role="status">No active departments are available. Contact your campus administrator.</div>}
              </div>
              <Field name="currentSemester" label="Current semester" type="number" min="1" max="8" value={form.currentSemester} onChange={update} />
              <Field name="admissionYear" label="Admission year" type="number" min="1900" max="2100" value={form.admissionYear} onChange={update} />
              <div className="col-12">
                <button className="btn btn-primary w-100" type="submit" disabled={submitting || loadingDepartments || !departments.length}>
                  {submitting ? 'Creating account...' : 'Create student account'}
                </button>
              </div>
            </div>
          </form>
        </div>
      </section>
    </main>
  )
}

function Field({ name, label, type = 'text', value, onChange, ...attributes }) {
  return (
    <div className="col-12 col-md-6">
      <label className="form-label" htmlFor={`signup-${name}`}>{label}</label>
      <input id={`signup-${name}`} className="form-control" name={name} type={type} value={value} onChange={onChange} required {...attributes} />
    </div>
  )
}
