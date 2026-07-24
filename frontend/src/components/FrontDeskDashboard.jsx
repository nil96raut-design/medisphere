import React, { useState, useEffect } from 'react'
import { api } from '../api/client'

export default function FrontDeskDashboard() {
  const [query, setQuery] = useState('')
  const [patients, setPatients] = useState([])
  const [selectedPatient, setSelectedPatient] = useState(null)

  // Registration form state
  const [regForm, setRegForm] = useState({
    firstName: '', lastName: '', gender: '', dateOfBirth: '', phoneNumber: '', email: '', emergencyContact: '', insuranceProvider: '', policyNumber: ''
  })
  const [regLoading, setRegLoading] = useState(false)
  const [regError, setRegError] = useState('')
  const [regSuccess, setRegSuccess] = useState('')

  // Triage form state
  const [triageForm, setTriageForm] = useState({
    bloodPressure: '', temperatureCelsius: '', pulseRate: '', weightKg: ''
  })
  const [triageLoading, setTriageLoading] = useState(false)
  const [triageError, setTriageError] = useState('')
  const [triageSuccess, setTriageSuccess] = useState('')

  // Appointment booking state
  const [doctors, setDoctors] = useState([])
  const [apptForm, setApptForm] = useState({
    patientId: '', doctorId: '', appointmentDate: '', startTime: '', endTime: ''
  })
  const [apptLoading, setApptLoading] = useState(false)
  const [apptError, setApptError] = useState('')
  const [apptSuccess, setApptSuccess] = useState('')

  // Search effect
  useEffect(() => {
    const delay = setTimeout(() => {
      api.searchPatients(query)
        .then(setPatients)
        .catch(console.error)
    }, 300)
    return () => clearTimeout(delay)
  }, [query])

  // Load doctors on mount
  useEffect(() => {
    api.getAvailableDoctors()
      .then(setDoctors)
      .catch(err => setApptError(`Failed to load doctors: ${err.message}`))
  }, [])

  const handleRegSubmit = async (e) => {
    e.preventDefault()
    setRegLoading(true)
    setRegError('')
    setRegSuccess('')
    try {
      await api.registerPatient(regForm)
      setRegSuccess('Patient registered successfully')
      setRegForm({ firstName: '', lastName: '', gender: '', dateOfBirth: '', phoneNumber: '', email: '', emergencyContact: '', insuranceProvider: '', policyNumber: '' })
      api.searchPatients(query).then(setPatients).catch(console.error)
    } catch (err) {
      setRegError(err.message)
    } finally {
      setRegLoading(false)
    }
  }

  const handleTriageSubmit = async (e) => {
    e.preventDefault()
    if (!selectedPatient) return
    setTriageLoading(true)
    setTriageError('')
    setTriageSuccess('')
    try {
      await api.logTriage(selectedPatient.id, {
        bloodPressure: triageForm.bloodPressure || null,
        temperatureCelsius: triageForm.temperatureCelsius ? parseFloat(triageForm.temperatureCelsius) : null,
        pulseRate: triageForm.pulseRate ? parseInt(triageForm.pulseRate) : null,
        weightKg: triageForm.weightKg ? parseFloat(triageForm.weightKg) : null
      })
      setTriageSuccess('Vitals logged successfully')
      setTriageForm({ bloodPressure: '', temperatureCelsius: '', pulseRate: '', weightKg: '' })
    } catch (err) {
      setTriageError(err.message)
    } finally {
      setTriageLoading(false)
    }
  }

  const handleApptSubmit = async (e) => {
    e.preventDefault()
    setApptLoading(true)
    setApptError('')
    setApptSuccess('')
    try {
      await api.bookAppointment({
        patientId: parseInt(apptForm.patientId),
        doctorId: parseInt(apptForm.doctorId),
        appointmentDate: apptForm.appointmentDate,
        startTime: apptForm.startTime,
        endTime: apptForm.endTime
      })
      setApptSuccess('Appointment booked successfully')
      setApptForm({ patientId: '', doctorId: '', appointmentDate: '', startTime: '', endTime: '' })
    } catch (err) {
      setApptError(err.message)
    } finally {
      setApptLoading(false)
    }
  }

  const toggleApptPatient = (p) => {
    setSelectedPatient(p)
    setApptForm(prev => ({ ...prev, patientId: p.id }))
    setTriageSuccess('')
    setTriageError('')
  }

  return (
    <div className="front-desk">
      {/* ---------- Patient Registration ---------- */}
      <h2>Patient Registration</h2>
      <div className="auth-card" style={{ maxWidth: '800px', margin: '0 auto 2rem auto' }}>
        {regError && <p className="form-error">{regError}</p>}
        {regSuccess && <p style={{ color: 'green' }}>{regSuccess}</p>}
        <form onSubmit={handleRegSubmit} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <label>First Name <input value={regForm.firstName} onChange={e => setRegForm({...regForm, firstName: e.target.value})} required /></label>
          <label>Last Name <input value={regForm.lastName} onChange={e => setRegForm({...regForm, lastName: e.target.value})} required /></label>
          <label>Gender <input value={regForm.gender} onChange={e => setRegForm({...regForm, gender: e.target.value})} /></label>
          <label>Date of Birth <input type="date" value={regForm.dateOfBirth} onChange={e => setRegForm({...regForm, dateOfBirth: e.target.value})} /></label>
          <label>Phone Number <input value={regForm.phoneNumber} onChange={e => setRegForm({...regForm, phoneNumber: e.target.value})} required /></label>
          <label>Email <input type="email" value={regForm.email} onChange={e => setRegForm({...regForm, email: e.target.value})} /></label>
          <label>Emergency Contact <input value={regForm.emergencyContact} onChange={e => setRegForm({...regForm, emergencyContact: e.target.value})} /></label>
          <label>Insurance Provider <input value={regForm.insuranceProvider} onChange={e => setRegForm({...regForm, insuranceProvider: e.target.value})} /></label>
          <label>Policy Number <input value={regForm.policyNumber} onChange={e => setRegForm({...regForm, policyNumber: e.target.value})} /></label>
          <div style={{ gridColumn: '1 / -1' }}>
            <button className="btn btn--primary" type="submit" disabled={regLoading}>
              {regLoading ? 'Registering...' : 'Register Patient'}
            </button>
          </div>
        </form>
      </div>

      {/* ---------- Patient Search & Triage ---------- */}
      <h2>Patient Search & Triage</h2>
      <div className="search-row">
        <input
          type="search"
          className="search-input patient-search"
          placeholder="Search patients by name or phone..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      <div className="task-grid">
        {patients.map(p => (
          <div key={p.id} className="task-card" style={{ cursor: 'pointer' }} onClick={() => toggleApptPatient(p)}>
            <h3>{p.firstName} {p.lastName}</h3>
            <p className="muted">Phone: {p.phoneNumber}</p>
          </div>
        ))}
      </div>
      {patients.length === 0 && <p className="muted" style={{ textAlign: 'center', marginTop: '2rem' }}>No patients found.</p>}

      {/* ---------- Appointment Booking ---------- */}
      <h2 style={{ marginTop: '2.5rem' }}>Book Appointment</h2>
      <div className="auth-card" style={{ maxWidth: '600px', margin: '0 auto 2rem auto' }}>
        {apptError && <p className="form-error">{apptError}</p>}
        {apptSuccess && <p style={{ color: 'green' }}>{apptSuccess}</p>}
        <form onSubmit={handleApptSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <label>Patient ID
            <input type="number" value={apptForm.patientId} onChange={e => setApptForm({...apptForm, patientId: e.target.value})} required placeholder={selectedPatient ? `Selected: ${selectedPatient.firstName} ${selectedPatient.lastName}` : 'Enter patient ID or click a patient above'} />
          </label>
          <label>Doctor
            <select value={apptForm.doctorId} onChange={e => setApptForm({...apptForm, doctorId: e.target.value})} required>
              <option value="">Select doctor...</option>
              {doctors.map(d => (
                <option key={d.id} value={d.id}>{d.fullName} — {d.specialization}</option>
              ))}
            </select>
          </label>
          <label>Date
            <input type="date" value={apptForm.appointmentDate} onChange={e => setApptForm({...apptForm, appointmentDate: e.target.value})} required />
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <label>Start Time
              <input type="time" value={apptForm.startTime} onChange={e => setApptForm({...apptForm, startTime: e.target.value})} required />
            </label>
            <label>End Time
              <input type="time" value={apptForm.endTime} onChange={e => setApptForm({...apptForm, endTime: e.target.value})} required />
            </label>
          </div>
          <button className="btn btn--primary btn--block" type="submit" disabled={apptLoading}>
            {apptLoading ? 'Booking...' : 'Book Appointment'}
          </button>
        </form>
      </div>

      {/* ---------- Triage Slide-out Overlay ---------- */}
      {selectedPatient && (
        <div className="drawer-overlay" onClick={() => setSelectedPatient(null)}>
          <div className="drawer triage-overlay" onClick={e => e.stopPropagation()}>
            <div className="drawer__header">
              <h2>Triage: {selectedPatient.firstName} {selectedPatient.lastName}</h2>
              <button className="btn btn--ghost" onClick={() => setSelectedPatient(null)}>Close</button>
            </div>
            <div className="drawer__body">
              {triageError && <p className="form-error">{triageError}</p>}
              {triageSuccess && <p style={{ color: 'green' }}>{triageSuccess}</p>}
              <form onSubmit={handleTriageSubmit}>
                <label>Blood Pressure (e.g. 120/80)
                  <input value={triageForm.bloodPressure} onChange={e => setTriageForm({...triageForm, bloodPressure: e.target.value})} />
                </label>
                <label>Temperature (°C)
                  <input type="number" step="0.1" value={triageForm.temperatureCelsius} onChange={e => setTriageForm({...triageForm, temperatureCelsius: e.target.value})} />
                </label>
                <label>Pulse Rate (bpm)
                  <input type="number" value={triageForm.pulseRate} onChange={e => setTriageForm({...triageForm, pulseRate: e.target.value})} />
                </label>
                <label>Weight (kg)
                  <input type="number" step="0.1" value={triageForm.weightKg} onChange={e => setTriageForm({...triageForm, weightKg: e.target.value})} />
                </label>
                <button className="btn btn--primary btn--block" type="submit" disabled={triageLoading}>
                  {triageLoading ? 'Logging...' : 'Log Vitals'}
                </button>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
