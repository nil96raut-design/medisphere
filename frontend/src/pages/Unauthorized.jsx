import React from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Unauthorized() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const handleBack = () => {
    const from = location.state?.from?.pathname || '/dashboard'
    navigate(from, { replace: true })
  }

  return (
    <div className="unauthorized-screen">
      <div className="unauthorized-card">
        <div className="unauthorized-icon">🚫</div>
        <h1>Access Denied</h1>
        <p className="muted">
          You don't have permission to access this page.
          {user && <span> Your role: <strong>{user.role}</strong></span>}
        </p>
        <div className="unauthorized-actions">
          <Link to="/" className="btn btn--primary" onClick={handleBack}>
            Go Back
          </Link>
          <button className="btn btn--ghost" onClick={logout}>
            Sign Out
          </button>
        </div>
        <p className="muted unauthorized-note">
          Contact your hospital administrator if you believe this is an error.
        </p>
      </div>
    </div>
  )
}