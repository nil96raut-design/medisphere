import React from 'react'
import { useAuth } from '../context/AuthContext'

const ROLE_LABEL = { PATIENT: 'Patient', DOCTOR: 'Doctor', RECEPTIONIST: 'Care team', ADMIN: 'Hospital admin' }

export default function Navbar() {
  const { user, logout } = useAuth()

  return (
    <header className="navbar">
      <div className="navbar__brand">
        <span className="navbar__pulse-dot" />
        <span className="navbar__wordmark">HealthTrack</span>
      </div>
      {user && (
        <div className="navbar__user">
          <div className="navbar__user-info">
            <span className="navbar__name">{user.fullName}</span>
            <span className="navbar__role">{ROLE_LABEL[user.role]}</span>
          </div>
          <button className="btn btn--ghost" onClick={logout}>Sign out</button>
        </div>
      )}
    </header>
  )
}
