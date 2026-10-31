import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { getErrorMessage } from '../api/axios'
import { asList } from '../utils/data'

const emptyForm = {
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  committeeId: '',
}

export default function CoordinatorProvisioningPage() {
  const [form, setForm] = useState(emptyForm)
  const [committees, setCommittees] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let active = true
    api.get('/committees/manageable')
      .then((response) => { if (active) setCommittees(asList(response.data)) })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError, 'Committees could not be loaded.')) })
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
      const response = await api.post('/admin/coordinators', {
        ...form,
        committeeId: Number(form.committeeId),
      })
      setNotice(`Coordinator account ${response.data.username} was created and assigned to ${response.data.committeeName}. The password is not returned or stored in browser state. Share initial credentials only through an approved secure channel.`)
      setForm(emptyForm)
    } catch (requestError) {
      const fallback = requestError.response
        ? `Coordinator account could not be provisioned (HTTP ${requestError.response.status}). Try again or contact a CampusSync administrator.`
        : 'Could not reach the CampusSync API. Check that the backend is running and try again.'
      setError(getErrorMessage(requestError, fallback))
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="container page-content">
      <Link to="/admin" className="small text-decoration-none">← Administrator dashboard</Link>
      <div className="mb-4 mt-3">
        <h1 className="h2 mb-1">Provision a committee coordinator</h1>
        <p className="text-secondary mb-0">Creates a coordinator account and active membership in the selected committee. This action is administrator-only.</p>
      </div>
      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <section className="card border-0 shadow-sm">
        <div className="card-body p-3 p-md-4">
          {loading ? <div className="d-flex align-items-center gap-2" role="status"><span className="spinner-border spinner-border-sm" />Loading committees...</div> :
            <form onSubmit={submit}>
              <div className="row g-3">
                <Field name="firstName" label="First name" value={form.firstName} onChange={update} />
                <Field name="lastName" label="Last name" value={form.lastName} onChange={update} />
                <Field name="username" label="Username" value={form.username} onChange={update} />
                <Field name="email" label="Email address" type="email" value={form.email} onChange={update} />
                <Field name="password" label="Initial password (8–72 characters)" type="password" minLength="8" maxLength="72" value={form.password} onChange={update} />
                <div className="col-12 col-md-6">
                  <label className="form-label" htmlFor="coordinator-committee">Committee</label>
                  <select className="form-select" id="coordinator-committee" name="committeeId" value={form.committeeId} onChange={update} required disabled={!committees.length}>
                    <option value="">Choose an active committee</option>
                    {committees.map((committee) => <option key={committee.id} value={committee.id}>{committee.name} ({committee.code})</option>)}
                  </select>
                </div>
                {!committees.length && <div className="col-12"><div className="alert alert-warning mb-0">No active committees are available to assign. <Link to="/admin/committees" className="alert-link">Create or activate a committee</Link>.</div></div>}
                <div className="col-12">
                  <p className="small text-secondary">The password is sent only to the authenticated API request, hashed with BCrypt, and never returned. Use HTTPS in deployment and deliver initial credentials out of band.</p>
                  <button className="btn btn-primary" type="submit" disabled={saving || !committees.length}>{saving ? 'Provisioning...' : 'Create coordinator account'}</button>
                </div>
              </div>
            </form>}
        </div>
      </section>
      <p className="small text-secondary mt-3">For an existing coordinator account, the protected API supports assignment by account ID and committee ID; a regular student or faculty account cannot be elevated through that operation.</p>
    </main>
  )
}

function Field({ name, label, type = 'text', value, onChange, ...attributes }) {
  return <div className="col-12 col-md-6">
    <label className="form-label" htmlFor={`coordinator-${name}`}>{label}</label>
    <input id={`coordinator-${name}`} className="form-control" name={name} type={type} value={value} onChange={onChange} required {...attributes} />
  </div>
}
