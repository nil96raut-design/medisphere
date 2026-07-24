import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { api } from '../api/client'
import { Modal } from './Modal'
import { Card } from './Card'
import { useToast } from '../context/ToastContext'
import { Bed, UserPlus, Activity, FileCheck, Stethoscope, Clock, CheckCircle2 } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'

function BedGrid({ beds, admissions, onAdmit, onBedClick }) {
  // Merge available beds and occupied beds from admissions to form a visual grid
  const allVisualBeds = useMemo(() => {
    const grid = []
    
    // Add occupied beds from active admissions
    admissions.forEach(a => {
      grid.push({
        id: `occ-${a.id}`, // pseudo id for grid
        wardName: a.wardName,
        bedNumber: a.bedNumber,
        status: 'OCCUPIED',
        admission: a,
        patientName: a.patientName
      })
    })

    // Add available beds
    beds.forEach(b => {
      grid.push({
        ...b,
        status: 'AVAILABLE'
      })
    })
    
    // Sort logically by ward name then bed number
    return grid.sort((a, b) => {
      if (a.wardName === b.wardName) {
        return parseInt(a.bedNumber) - parseInt(b.bedNumber)
      }
      return a.wardName.localeCompare(b.wardName)
    })
  }, [beds, admissions])

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-bold text-slate-800 m-0">Ward Layout</h3>
        <div className="flex gap-4 text-sm font-medium">
          <span className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-success"></div>Available</span>
          <span className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-danger"></div>Occupied</span>
          <span className="flex items-center gap-2"><div className="w-3 h-3 rounded-full bg-warning"></div>Cleaning</span>
        </div>
      </div>
      
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
        <AnimatePresence>
          {allVisualBeds.map((b) => (
            <motion.div
              layout
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              key={b.id} 
              onClick={() => {
                if (b.status === 'AVAILABLE') onAdmit(b)
                else onBedClick(b.admission)
              }}
              className={`relative p-4 rounded-xl border-2 cursor-pointer transition-all duration-200 hover:scale-105 group flex flex-col items-center gap-2
                ${b.status === 'AVAILABLE' ? 'bg-success-light/30 border-success/30 hover:border-success/60' : ''}
                ${b.status === 'OCCUPIED' ? 'bg-danger-light/30 border-danger/30 hover:border-danger/60' : ''}
                ${b.status === 'CLEANING' ? 'bg-warning-light/30 border-warning/30 hover:border-warning/60' : ''}
              `}
            >
              <Bed 
                size={28} 
                className={
                  b.status === 'AVAILABLE' ? 'text-success' : 
                  b.status === 'OCCUPIED' ? 'text-danger' : 'text-warning'
                } 
              />
              <div className="text-center">
                <p className="font-bold text-slate-800 text-sm m-0 leading-tight">{b.wardName}</p>
                <p className="font-mono text-xs text-slate-500 m-0">#{b.bedNumber}</p>
              </div>

              {b.status === 'OCCUPIED' && (
                <div className="absolute -top-12 left-1/2 -translate-x-1/2 bg-slate-800 text-white text-xs py-1 px-3 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap pointer-events-none z-10 shadow-lg">
                  {b.patientName}
                  <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-2 h-2 bg-slate-800 rotate-45"></div>
                </div>
              )}
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
      
      {allVisualBeds.length === 0 && (
        <div className="py-12 text-center text-slate-400">
          <p>No beds available in this facility.</p>
        </div>
      )}
    </div>
  )
}

function AdmissionForm({ bed, patients, doctors, onClose, onSaved }) {
  const [patientId, setPatientId] = useState('')
  const [doctorId, setDoctorId] = useState('')
  const [admissionDate, setAdmissionDate] = useState(new Date().toISOString().slice(0, 10))
  const [initialDiagnosis, setInitialDiagnosis] = useState('')
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  const handleSave = async () => {
    setSaving(true)
    try {
      await api.admitPatient({
        patientId: Number(patientId),
        doctorId: Number(doctorId),
        bedId: bed.id,
        admissionDate,
        initialDiagnosis,
      })
      toast.success('Patient admitted successfully')
      onSaved()
      onClose()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal isOpen={true} onClose={onClose} title={`Admit to ${bed.wardName} #${bed.bedNumber}`}>
      <div className="space-y-4">
        <div>
          <label className="form-label">Select Patient</label>
          <select className="input-field" value={patientId} onChange={(e) => setPatientId(e.target.value)}>
            <option value="">Choose a patient...</option>
            {patients.map((p) => (
              <option key={p.id} value={p.id}>{p.firstName} {p.lastName} ({p.phoneNumber})</option>
            ))}
          </select>
        </div>
        
        <div>
          <label className="form-label">Attending Doctor</label>
          <select className="input-field" value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
            <option value="">Assign a doctor...</option>
            {doctors.filter((d) => d.role === 'DOCTOR').map((d) => (
              <option key={d.id} value={d.id}>{d.fullName}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="form-label">Admission Date</label>
          <input className="input-field" type="date" value={admissionDate} onChange={(e) => setAdmissionDate(e.target.value)} />
        </div>

        <div>
          <label className="form-label">Initial Diagnosis</label>
          <textarea 
            className="input-field min-h-[100px]" 
            value={initialDiagnosis} 
            onChange={(e) => setInitialDiagnosis(e.target.value)} 
            placeholder="Describe the reason for admission..."
          />
        </div>

        <div className="pt-4 flex justify-end gap-3 border-t border-slate-100">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving || !patientId || !doctorId} onClick={handleSave}>
            {saving ? 'Admitting...' : 'Confirm Admission'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

function ActiveAdmissions({ admissions, onNursingLog, onDischarge }) {
  if (!admissions.length) {
    return (
      <div className="py-12 text-center text-slate-500 bg-surface rounded-xl border border-slate-200 border-dashed">
        <Bed size={32} className="mx-auto text-slate-300 mb-3" />
        <p>No active admissions at the moment.</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mt-4">
      {admissions.map((a) => (
        <Card key={a.id} className="flex flex-col border-l-4 border-l-primary hover:scale-[1.01]">
          <div className="flex justify-between items-start mb-3">
            <div>
              <h4 className="font-bold text-slate-800 m-0 text-lg">{a.patientName}</h4>
              <p className="text-xs font-semibold text-primary m-0 mt-0.5">{a.wardName} #{a.bedNumber}</p>
            </div>
            <span className="status-tag bg-primary-light text-primary">{a.status}</span>
          </div>
          
          <div className="text-sm text-slate-600 flex-1 mb-4">
            <p className="line-clamp-2 m-0" title={a.initialDiagnosis}>
              {a.initialDiagnosis || 'No initial diagnosis provided.'}
            </p>
            <p className="text-xs text-slate-400 mt-2 flex items-center gap-1">
              <Clock size={12} /> Admitted {a.admissionDate}
            </p>
          </div>
          
          <div className="flex gap-2 mt-auto border-t border-slate-100 pt-3">
            <button className="btn btn-secondary text-sm flex-1" onClick={() => onNursingLog(a)}>
              <Activity size={16} /> Log Vitals
            </button>
            <button className="btn btn-danger text-sm flex-1" onClick={() => onDischarge(a)}>
              <FileCheck size={16} /> Discharge
            </button>
          </div>
        </Card>
      ))}
    </div>
  )
}

function NursingLogForm({ admission, onClose, onSaved }) {
  const [vitals, setVitals] = useState('')
  const [meds, setMeds] = useState('')
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  const handleSave = async () => {
    setSaving(true)
    try {
      await api.addNursingLog(admission.id, {
        vitalsRecorded: vitals,
        medicineAdministered: meds,
        nursingNotes: notes,
      })
      toast.success('Nursing log recorded')
      onSaved()
      onClose()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal isOpen={true} onClose={onClose} title={`Nursing Log — ${admission.patientName}`}>
      <div className="space-y-4">
        <div>
          <label className="form-label">Vitals Recorded</label>
          <textarea 
            className="input-field min-h-[80px]" 
            value={vitals} 
            onChange={(e) => setVitals(e.target.value)} 
            placeholder="BP: 120/80, Pulse: 72, Temp: 98.6°F, SpO2: 99%..." 
          />
        </div>
        <div>
          <label className="form-label">Medicine Administered</label>
          <textarea 
            className="input-field min-h-[80px]" 
            value={meds} 
            onChange={(e) => setMeds(e.target.value)} 
            placeholder="Paracetamol 500mg PO, NS 100ml/hr..." 
          />
        </div>
        <div>
          <label className="form-label">Nursing Notes</label>
          <textarea 
            className="input-field min-h-[100px]" 
            value={notes} 
            onChange={(e) => setNotes(e.target.value)} 
            placeholder="Patient resting comfortably, no complaints of pain..." 
          />
        </div>
        <div className="pt-4 flex justify-end gap-3 border-t border-slate-100">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving || (!vitals && !meds && !notes)} onClick={handleSave}>
            {saving ? 'Saving...' : 'Save Log'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

function DischargeForm({ admission, onClose, onSaved }) {
  const [summary, setSummary] = useState('')
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  const handleSave = async () => {
    setSaving(true)
    try {
      await api.dischargePatient(admission.id, { dischargeSummary: summary })
      toast.success(`${admission.patientName} has been discharged`)
      onSaved()
      onClose()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal isOpen={true} onClose={onClose} title={`Discharge — ${admission.patientName}`}>
      <div className="space-y-4">
        <div className="p-3 bg-primary-light/50 border border-primary/20 rounded-xl text-sm text-teal-800 flex items-start gap-3">
          <CheckCircle2 className="mt-0.5 text-primary shrink-0" size={18} />
          <p className="m-0 leading-relaxed">
            You are about to discharge this patient. This will free up <strong>{admission.wardName} #{admission.bedNumber}</strong>. Please provide a detailed discharge summary.
          </p>
        </div>

        <div>
          <label className="form-label">Discharge Summary & Instructions</label>
          <textarea 
            className="input-field min-h-[150px]" 
            value={summary} 
            onChange={(e) => setSummary(e.target.value)} 
            placeholder="Treatment outcome, follow-up instructions, prescribed medications to take home..." 
          />
        </div>
        <div className="pt-4 flex justify-end gap-3 border-t border-slate-100">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-danger" disabled={saving || !summary.trim()} onClick={handleSave}>
            {saving ? 'Processing...' : 'Confirm Discharge'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default function WardVisualizer() {
  const [beds, setBeds] = useState([])
  const [admissions, setAdmissions] = useState([])
  const [patients, setPatients] = useState([])
  const [doctors, setDoctors] = useState([])
  const [admitBed, setAdmitBed] = useState(null)
  const [nursingAdmission, setNursingAdmission] = useState(null)
  const [dischargeAdmission, setDischargeAdmission] = useState(null)
  const [activeBedModal, setActiveBedModal] = useState(null)
  const toast = useToast()

  const refresh = useCallback(async () => {
    try {
      const [b, a] = await Promise.all([api.getAvailableBeds(), api.getActiveAdmissions()])
      setBeds(b)
      setAdmissions(a)
    } catch (e) {
      toast.error("Failed to load ward data")
    }
  }, [toast])

  useEffect(() => { refresh() }, [refresh])

  useEffect(() => {
    api.usersByRole('DOCTOR').then(setDoctors).catch(() => {})
  }, [])

  // Auto-search patients when admit form opens
  useEffect(() => {
    if (admitBed && patients.length === 0) {
      api.searchPatients('').then(setPatients).catch(() => {})
    }
  }, [admitBed, patients.length])

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500 pb-20">
      
      <BedGrid 
        beds={beds} 
        admissions={admissions} 
        onAdmit={(bed) => setAdmitBed(bed)} 
        onBedClick={(admission) => setActiveBedModal(admission)}
      />

      <div className="pt-8 border-t border-slate-200">
        <h3 className="text-xl font-display font-bold text-slate-800 m-0">Active Admissions</h3>
        <p className="text-sm text-slate-500 mt-1 mb-4">Patients currently admitted and receiving care.</p>
        <ActiveAdmissions 
          admissions={admissions} 
          onNursingLog={setNursingAdmission} 
          onDischarge={setDischargeAdmission} 
        />
      </div>

      {/* View Occupied Bed Modal */}
      {activeBedModal && (
        <Modal isOpen={true} onClose={() => setActiveBedModal(null)} title={`Bed Details`}>
          <div className="space-y-4">
             <div className="flex justify-between items-start">
               <div>
                 <h3 className="text-lg font-bold text-slate-800">{activeBedModal.patientName}</h3>
                 <p className="text-sm text-slate-500">Admitted on {activeBedModal.admissionDate}</p>
               </div>
               <span className="status-tag bg-danger-light text-danger">OCCUPIED</span>
             </div>
             <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 text-sm text-slate-700">
                <strong className="block text-slate-800 mb-1">Diagnosis:</strong>
                {activeBedModal.initialDiagnosis || 'No details provided.'}
             </div>
             <div className="flex gap-2 pt-4 border-t border-slate-100">
                <button className="btn btn-secondary flex-1" onClick={() => {
                  setNursingAdmission(activeBedModal)
                  setActiveBedModal(null)
                }}>
                  <Activity size={18} /> Nursing Log
                </button>
                <button className="btn btn-danger flex-1" onClick={() => {
                  setDischargeAdmission(activeBedModal)
                  setActiveBedModal(null)
                }}>
                  <FileCheck size={18} /> Discharge
                </button>
             </div>
          </div>
        </Modal>
      )}

      {admitBed && (
        <AdmissionForm bed={admitBed} patients={patients} doctors={doctors} onClose={() => setAdmitBed(null)} onSaved={refresh} />
      )}
      {nursingAdmission && (
        <NursingLogForm admission={nursingAdmission} onClose={() => setNursingAdmission(null)} onSaved={refresh} />
      )}
      {dischargeAdmission && (
        <DischargeForm admission={dischargeAdmission} onClose={() => setDischargeAdmission(null)} onSaved={refresh} />
      )}
    </div>
  )
}
