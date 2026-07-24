import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function HospitalRegister() {
  const { hospitalSignup } = useAuth()
  const navigate = useNavigate()
  
  const [hospitalName, setHospitalName] = useState('')
  const [licenseNumber, setLicenseNumber] = useState('')
  const [adminFullName, setAdminFullName] = useState('')
  const [adminEmail, setAdminEmail] = useState('')
  const [adminPassword, setAdminPassword] = useState('')
  
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await hospitalSignup({
        hospitalName,
        licenseNumber,
        adminFullName,
        adminEmail,
        adminPassword,
      })
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="auth-brand">
          <span className="navbar__pulse-dot" />
          <span className="navbar__wordmark">HealthTrack</span>
        </div>
        <h1>Register your Hospital</h1>
        <p className="muted">Create an isolated workspace for your care team.</p>

        {error && <p className="form-error">{error}</p>}

        <form onSubmit={submit}>
          <label>
            Hospital Name
            <input value={hospitalName} onChange={(e) => setHospitalName(e.target.value)} required />
          </label>
          <label>
            License Number
            <input value={licenseNumber} onChange={(e) => setLicenseNumber(e.target.value)} required />
          </label>
          <label>
            Admin Full Name
            <input value={adminFullName} onChange={(e) => setAdminFullName(e.target.value)} required />
          </label>
          <label>
            Admin Email
            <input type="email" value={adminEmail} onChange={(e) => setAdminEmail(e.target.value)} required />
          </label>
          <label>
            Admin Password
            <input type="password" value={adminPassword} onChange={(e) => setAdminPassword(e.target.value)} required minLength={6} />
          </label>
          
          <button className="btn btn--primary btn--block" type="submit" disabled={loading}>
            {loading ? 'Registering…' : 'Register Hospital'}
          </button>
        </form>

        <p className="muted auth-switch">
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
