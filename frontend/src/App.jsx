import React, { lazy, Suspense } from 'react'
import { ToastProvider } from './context/ToastContext'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import HospitalRegister from './pages/HospitalRegister'
import Unauthorized from './pages/Unauthorized'
import { ProtectedRoute, PublicOnlyRoute } from './components/ProtectedRoute'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const DoctorDashboard = lazy(() => import('./pages/DoctorDashboard'))
const FrontDeskDashboard = lazy(() => import('./pages/FrontDeskDashboard'))
const IPDDashboard = lazy(() => import('./pages/IPDDashboard'))
const LabDashboard = lazy(() => import('./pages/LabDashboard'))
const PharmacyDashboard = lazy(() => import('./pages/PharmacyDashboard'))
const BillingDashboard = lazy(() => import('./pages/BillingDashboard'))
const PatientDashboard = lazy(() => import('./pages/PatientDashboard'))
const UserManagement = lazy(() => import('./pages/UserManagement'))
const QueueBoard = lazy(() => import('./components/QueueBoard'))

function PrivateRoute({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" replace />
}

function Routed() {
  return (
    <Suspense fallback={<div className="loading">Loading…</div>}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<PublicOnlyRoute><Login /></PublicOnlyRoute>} />
        <Route path="/register" element={<PublicOnlyRoute><Register /></PublicOnlyRoute>} />
        <Route path="/hospital-register" element={<PublicOnlyRoute><HospitalRegister /></PublicOnlyRoute>} />
        <Route path="/unauthorized" element={<Unauthorized />} />

        {/* Admin routes */}
        <Route path="/dashboard" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['ADMIN']}><Dashboard /></ProtectedRoute></PrivateRoute>
        } />
        <Route path="/billing" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['ADMIN', 'RECEPTIONIST']}><BillingDashboard /></ProtectedRoute></PrivateRoute>
        } />
        <Route path="/users" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['ADMIN']}><UserManagement /></ProtectedRoute></PrivateRoute>
        } />

        {/* Doctor routes */}
        <Route path="/doctor" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['DOCTOR', 'ADMIN']}><DoctorDashboard /></ProtectedRoute></PrivateRoute>
        } />

        {/* Front Desk / Staff routes */}
        <Route path="/frontdesk" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['RECEPTIONIST', 'ADMIN']}><FrontDeskDashboard /></ProtectedRoute></PrivateRoute>
        } />
        <Route path="/queue" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['RECEPTIONIST', 'DOCTOR', 'ADMIN']}><QueueBoard /></ProtectedRoute></PrivateRoute>
        } />

        {/* Nurse / IPD routes */}
        <Route path="/ipd" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['NURSE', 'DOCTOR', 'ADMIN']}><IPDDashboard /></ProtectedRoute></PrivateRoute>
        } />

        {/* Lab routes */}
        <Route path="/lab" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['LAB_TECH', 'ADMIN']}><LabDashboard /></ProtectedRoute></PrivateRoute>
        } />

        {/* Pharmacy routes */}
        <Route path="/pharmacy" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['PHARMACIST', 'ADMIN']}><PharmacyDashboard /></ProtectedRoute></PrivateRoute>
        } />

        {/* Patient routes */}
        <Route path="/patient" element={
          <PrivateRoute><ProtectedRoute allowedRoles={['PATIENT']}><PatientDashboard /></ProtectedRoute></PrivateRoute>
        } />

        {/* Fallback routes for legacy paths */}
        <Route path="/tasks" element={<Navigate to="/dashboard" replace />} />
        <Route path="/consultation" element={<Navigate to="/doctor" replace />} />
        <Route path="/ward" element={<Navigate to="/ipd" replace />} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <Routed />
      </AuthProvider>
    </ToastProvider>
  )
}