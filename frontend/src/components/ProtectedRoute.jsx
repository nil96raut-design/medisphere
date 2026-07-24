import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute({ children, allowedRoles }) {
  const { user, isAdmin } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (allowedRoles && !allowedRoles.includes(user.role) && !isAdmin) {
    return <Navigate to="/unauthorized" replace />
  }

  return children
}

export function PublicOnlyRoute({ children }) {
  const { user } = useAuth()
  if (user) {
    switch (user.role) {
      case 'ADMIN': return <Navigate to="/dashboard" replace />
      case 'DOCTOR': return <Navigate to="/doctor" replace />
      case 'RECEPTIONIST': return <Navigate to="/frontdesk" replace />
      case 'NURSE': return <Navigate to="/ipd" replace />
      case 'PHARMACIST': return <Navigate to="/pharmacy" replace />
      case 'LAB_TECH': return <Navigate to="/lab" replace />
      case 'PATIENT': return <Navigate to="/patient" replace />
      default: return <Navigate to="/dashboard" replace />
    }
  }
  return children
}