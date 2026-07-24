import React, { useState, useEffect } from 'react'
import { api } from '../api/client'
import SidebarLayout from '../components/SidebarLayout'
import { Card } from '../components/Card'
import { Modal } from '../components/Modal'
import { useToast } from '../context/ToastContext'
import { UserPlus, CalendarPlus, Search, Phone, User, Activity, Clock, Shield } from 'lucide-react'

export default function FrontDeskDashboard() {
  const [query, setQuery] = useState('')
  const [patients, setPatients] = useState([])
  const [selectedPatient, setSelectedPatient] = useState(null)
  
  const [activeModal, setActiveModal] = useState(null) // 'REGISTER', 'APPOINTMENT', 'TRIAGE'

  const [regForm, setRegForm] = useState({
    firstName: '', lastName: '', gender: '', dateOfBirth: '', phoneNumber: '', email: '', emergencyContact: '', insuranceProvider: '', policyNumber: ''
  })
  const [regLoading, setRegLoading] = useState(false)

  const [triageForm, setTriageForm] = useState({
    bloodPressure: '', temperatureCelsius: '', pulseRate: '', weightKg: ''
  })
  const [triageLoading, setTriageLoading] = useState(false)

  const [doctors, setDoctors] = useState([])
  const [apptForm, setApptForm] = useState({
    patientId: '', doctorId: '', appointmentDate: '', startTime: '', endTime: ''
  })
  const [apptLoading, setApptLoading] = useState(false)
  
  const toast = useToast()

  useEffect(() => {
    const delay = setTimeout(() => {
      api.searchPatients({ q: query })
        .then(setPatients)
        .catch(() => {})
    }, 300)
    return () => clearTimeout(delay)
  }, [query])

  useEffect(() => {
    api.usersByRole('DOCTOR').then(setDoctors).catch(() => {})
  }, [])

  const handleRegSubmit = async (e) => {
    e.preventDefault()
    setRegLoading(true)
    try {
      await api.registerPatient(regForm)
      toast.success('Patient registered successfully')
      setRegForm({ firstName: '', lastName: '', gender: '', dateOfBirth: '', phoneNumber: '', email: '', emergencyContact: '', insuranceProvider: '', policyNumber: '' })
      api.searchPatients({ q: query }).then(setPatients).catch(() => {})
      setActiveModal(null)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setRegLoading(false)
    }
  }

  const handleTriageSubmit = async (e) => {
    e.preventDefault()
    if (!selectedPatient) return
    setTriageLoading(true)
    try {
      await api.logTriage(selectedPatient.id, {
        bloodPressure: triageForm.bloodPressure || null,
        temperatureCelsius: triageForm.temperatureCelsius ? parseFloat(triageForm.temperatureCelsius) : null,
        pulseRate: triageForm.pulseRate ? parseInt(triageForm.pulseRate) : null,
        weightKg: triageForm.weightKg ? parseFloat(triageForm.weightKg) : null
      })
      toast.success('Vitals logged successfully')
      setTriageForm({ bloodPressure: '', temperatureCelsius: '', pulseRate: '', weightKg: '' })
      setActiveModal(null)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setTriageLoading(false)
    }
  }

  const handleApptSubmit = async (e) => {
    e.preventDefault()
    setApptLoading(true)
    try {
      await api.bookAppointment({
        patientId: parseInt(apptForm.patientId),
        doctorId: parseInt(apptForm.doctorId),
        appointmentDate: apptForm.appointmentDate,
        startTime: apptForm.startTime,
        endTime: apptForm.endTime
      })
      toast.success('Appointment booked successfully')
      setApptForm({ patientId: '', doctorId: '', appointmentDate: '', startTime: '', endTime: '' })
      setActiveModal(null)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setApptLoading(false)
    }
  }

  const openTriage = (p) => {
    setSelectedPatient(p)
    setActiveModal('TRIAGE')
  }

  const openApptForPatient = (p) => {
    setSelectedPatient(p)
    setApptForm(prev => ({ ...prev, patientId: p.id }))
    setActiveModal('APPOINTMENT')
  }

  return (
    <SidebarLayout activeTab="FRONT_DESK" onTabChange={() => {}}>
      <div className="space-y-8 animate-in fade-in duration-500 pb-12">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-display font-bold text-slate-800 tracking-tight">Front Desk</h1>
            <p className="text-slate-500 mt-1">Manage patient flow, registrations, and appointments.</p>
          </div>
          <div className="flex gap-3">
            <button 
              onClick={() => setActiveModal('REGISTER')}
              className="btn btn-primary bg-primary text-white"
            >
              <UserPlus size={18} /> Register Patient
            </button>
            <button 
              onClick={() => {
                setSelectedPatient(null)
                setApptForm(prev => ({ ...prev, patientId: '' }))
                setActiveModal('APPOINTMENT')
              }}
              className="btn btn-secondary"
            >
              <CalendarPlus size={18} /> Book Appointment
            </button>
          </div>
        </div>

        <div className="bg-surface rounded-2xl border border-slate-200 shadow-sm overflow-hidden flex flex-col h-[calc(100vh-220px)] min-h-[500px]">
          <div className="p-4 border-b border-slate-200 bg-slate-50">
            <div className="relative max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
              <input
                type="search"
                className="input-field pl-10 py-2.5"
                placeholder="Search patients by name or phone..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
          </div>
          
          <div className="flex-1 p-6 overflow-y-auto no-scrollbar">
            {patients.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-slate-400">
                <div className="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mb-4">
                  <User size={32} className="text-slate-300" />
                </div>
                <p className="text-lg font-medium text-slate-600 mb-1">
                  {query ? 'No patients found' : 'Search for a patient'}
                </p>
                <p className="text-sm">
                  {query ? 'Try a different search term or register a new patient.' : 'Enter a name or phone number above.'}
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {patients.map(p => (
                  <Card key={p.id} padding="p-0" className="flex flex-col group overflow-hidden border-slate-200 hover:border-primary/50">
                    <div className="p-4 flex-1">
                      <div className="flex justify-between items-start mb-2">
                        <h3 className="font-bold text-slate-800 m-0 text-lg flex items-center gap-2">
                          <User size={18} className="text-primary" /> {p.firstName} {p.lastName}
                        </h3>
                        <span className="text-xs font-bold bg-slate-100 text-slate-500 px-2 py-1 rounded-md">ID: {p.id}</span>
                      </div>
                      <div className="space-y-1 mt-3">
                        <p className="text-sm text-slate-600 flex items-center gap-2 m-0">
                          <Phone size={14} className="text-slate-400" /> {p.phoneNumber}
                        </p>
                        <p className="text-sm text-slate-600 flex items-center gap-2 m-0">
                          <Clock size={14} className="text-slate-400" /> DOB: {p.dateOfBirth || 'Unknown'}
                        </p>
                        {p.insuranceProvider && (
                          <p className="text-sm text-slate-600 flex items-center gap-2 m-0">
                            <Shield size={14} className="text-slate-400" /> {p.insuranceProvider}
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="bg-slate-50 border-t border-slate-100 p-3 flex gap-2">
                      <button 
                        className="btn btn-secondary py-1.5 text-xs flex-1"
                        onClick={() => openTriage(p)}
                      >
                        <Activity size={14} /> Triage
                      </button>
                      <button 
                        className="btn btn-primary py-1.5 text-xs flex-1 bg-primary text-white"
                        onClick={() => openApptForPatient(p)}
                      >
                        <CalendarPlus size={14} /> Book
                      </button>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Patient Registration Modal */}
      {activeModal === 'REGISTER' && (
        <Modal isOpen={true} onClose={() => setActiveModal(null)} title="Register New Patient">
          <form onSubmit={handleRegSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div><label className="form-label">First Name *</label><input className="input-field" value={regForm.firstName} onChange={e => setRegForm({...regForm, firstName: e.target.value})} required /></div>
              <div><label className="form-label">Last Name *</label><input className="input-field" value={regForm.lastName} onChange={e => setRegForm({...regForm, lastName: e.target.value})} required /></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div><label className="form-label">Gender</label><input className="input-field" value={regForm.gender} onChange={e => setRegForm({...regForm, gender: e.target.value})} /></div>
              <div><label className="form-label">Date of Birth</label><input className="input-field" type="date" value={regForm.dateOfBirth} onChange={e => setRegForm({...regForm, dateOfBirth: e.target.value})} /></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div><label className="form-label">Phone Number *</label><input className="input-field" value={regForm.phoneNumber} onChange={e => setRegForm({...regForm, phoneNumber: e.target.value})} required /></div>
              <div><label className="form-label">Email</label><input className="input-field" type="email" value={regForm.email} onChange={e => setRegForm({...regForm, email: e.target.value})} /></div>
            </div>
            <div>
              <label className="form-label">Emergency Contact</label>
              <input className="input-field" value={regForm.emergencyContact} onChange={e => setRegForm({...regForm, emergencyContact: e.target.value})} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div><label className="form-label">Insurance Provider</label><input className="input-field" value={regForm.insuranceProvider} onChange={e => setRegForm({...regForm, insuranceProvider: e.target.value})} /></div>
              <div><label className="form-label">Policy Number</label><input className="input-field" value={regForm.policyNumber} onChange={e => setRegForm({...regForm, policyNumber: e.target.value})} /></div>
            </div>
            <div className="pt-4 flex justify-end gap-3 border-t border-slate-100 mt-6">
              <button type="button" className="btn btn-ghost" onClick={() => setActiveModal(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={regLoading}>
                {regLoading ? 'Registering...' : 'Register Patient'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Book Appointment Modal */}
      {activeModal === 'APPOINTMENT' && (
        <Modal isOpen={true} onClose={() => setActiveModal(null)} title="Book Appointment">
          <form onSubmit={handleApptSubmit} className="space-y-4">
            <div>
              <label className="form-label">Patient ID *</label>
              <input 
                type="number" 
                className="input-field bg-slate-50" 
                value={apptForm.patientId} 
                onChange={e => setApptForm({...apptForm, patientId: e.target.value})} 
                required 
                placeholder={selectedPatient ? `Selected: ${selectedPatient.firstName} ${selectedPatient.lastName} (ID: ${selectedPatient.id})` : 'Enter Patient ID'}
                readOnly={!!selectedPatient}
              />
            </div>
            <div>
              <label className="form-label">Doctor *</label>
              <select className="input-field" value={apptForm.doctorId} onChange={e => setApptForm({...apptForm, doctorId: e.target.value})} required>
                <option value="">Select doctor...</option>
                {doctors.map(d => (
                  <option key={d.id} value={d.id}>{d.fullName} — {d.specialization || 'General'}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label">Appointment Date *</label>
              <input className="input-field" type="date" value={apptForm.appointmentDate} onChange={e => setApptForm({...apptForm, appointmentDate: e.target.value})} required />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div><label className="form-label">Start Time *</label><input className="input-field" type="time" value={apptForm.startTime} onChange={e => setApptForm({...apptForm, startTime: e.target.value})} required /></div>
              <div><label className="form-label">End Time *</label><input className="input-field" type="time" value={apptForm.endTime} onChange={e => setApptForm({...apptForm, endTime: e.target.value})} required /></div>
            </div>
            <div className="pt-4 flex justify-end gap-3 border-t border-slate-100 mt-6">
              <button type="button" className="btn btn-ghost" onClick={() => setActiveModal(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={apptLoading}>
                {apptLoading ? 'Booking...' : 'Book Appointment'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Triage Modal */}
      {activeModal === 'TRIAGE' && selectedPatient && (
        <Modal isOpen={true} onClose={() => setActiveModal(null)} title={`Triage: ${selectedPatient.firstName} ${selectedPatient.lastName}`}>
          <form onSubmit={handleTriageSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="form-label">Blood Pressure</label>
                <input className="input-field" placeholder="e.g. 120/80" value={triageForm.bloodPressure} onChange={e => setTriageForm({...triageForm, bloodPressure: e.target.value})} />
              </div>
              <div>
                <label className="form-label">Temperature (°C)</label>
                <input className="input-field" type="number" step="0.1" placeholder="e.g. 37.2" value={triageForm.temperatureCelsius} onChange={e => setTriageForm({...triageForm, temperatureCelsius: e.target.value})} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="form-label">Pulse Rate (bpm)</label>
                <input className="input-field" type="number" placeholder="e.g. 72" value={triageForm.pulseRate} onChange={e => setTriageForm({...triageForm, pulseRate: e.target.value})} />
              </div>
              <div>
                <label className="form-label">Weight (kg)</label>
                <input className="input-field" type="number" step="0.1" placeholder="e.g. 70.5" value={triageForm.weightKg} onChange={e => setTriageForm({...triageForm, weightKg: e.target.value})} />
              </div>
            </div>
            <div className="pt-4 flex justify-end gap-3 border-t border-slate-100 mt-6">
              <button type="button" className="btn btn-ghost" onClick={() => setActiveModal(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={triageLoading}>
                {triageLoading ? 'Logging...' : 'Log Vitals'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </SidebarLayout>
  )
}