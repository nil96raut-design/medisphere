import React, { useState, useCallback } from 'react'
import { api } from '../api/client'
import { Card } from './Card'
import { useToast } from '../context/ToastContext'
import { Search, User, Clock, FileText, Pill, Stethoscope, Plus, Trash2, Calendar, ClipboardList, Activity } from 'lucide-react'

function PatientHistory({ records, onSelect }) {
  if (!records.length) {
    return (
      <div className="py-12 text-center text-slate-500 bg-slate-50 rounded-xl border border-slate-200 border-dashed">
        <Clock size={32} className="mx-auto text-slate-300 mb-3" />
        <p>No past encounters found.</p>
      </div>
    )
  }
  return (
    <div className="space-y-3">
      {records.map((r) => (
        <Card 
          key={r.id} 
          padding="p-4" 
          onClick={() => onSelect(r)}
          className="hover:border-primary/50 transition-colors group"
        >
          <div className="flex justify-between items-start mb-2">
            <span className="text-sm font-semibold text-slate-800 flex items-center gap-2">
              <Calendar size={14} className="text-primary" />
              {r.encounterDate}
            </span>
            <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-1 rounded-md">
              Dr. {r.doctorName}
            </span>
          </div>
          <div className="text-sm text-slate-600">
            <p className="mb-1"><strong className="text-slate-800">Dx:</strong> {r.diagnosis || '—'}</p>
            <p className="line-clamp-2 m-0"><strong className="text-slate-800">CC:</strong> {r.chiefComplaints}</p>
          </div>
        </Card>
      ))}
    </div>
  )
}

function PrescriptionBuilder({ items, setItems }) {
  const addRow = () => setItems([...items, { medicineName: '', dosage: '', frequency: '', duration: '', instructions: '' }])
  const removeRow = (i) => setItems(items.filter((_, idx) => idx !== i))
  const update = (i, field, value) => {
    const copy = items.map((row, idx) => idx === i ? { ...row, [field]: value } : row)
    setItems(copy)
  }
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between border-b border-slate-200 pb-2">
        <h4 className="font-semibold text-slate-800 flex items-center gap-2 m-0">
          <Pill size={18} className="text-primary" /> Prescriptions
        </h4>
        <button type="button" className="btn btn-secondary py-1 px-3 text-xs" onClick={addRow}>
          <Plus size={14} /> Add
        </button>
      </div>
      <div className="space-y-2">
        {items.map((row, i) => (
          <div key={i} className="flex flex-wrap md:flex-nowrap gap-2 items-start bg-slate-50 p-2 rounded-lg border border-slate-200">
            <input className="input-field py-1.5 px-3 text-sm flex-1 min-w-[120px]" placeholder="Medicine" value={row.medicineName} onChange={(e) => update(i, 'medicineName', e.target.value)} />
            <input className="input-field py-1.5 px-3 text-sm w-24" placeholder="Dosage" value={row.dosage} onChange={(e) => update(i, 'dosage', e.target.value)} />
            <input className="input-field py-1.5 px-3 text-sm w-28" placeholder="Frequency" value={row.frequency} onChange={(e) => update(i, 'frequency', e.target.value)} />
            <input className="input-field py-1.5 px-3 text-sm w-24" placeholder="Duration" value={row.duration} onChange={(e) => update(i, 'duration', e.target.value)} />
            <input className="input-field py-1.5 px-3 text-sm flex-1 min-w-[120px]" placeholder="Instructions" value={row.instructions} onChange={(e) => update(i, 'instructions', e.target.value)} />
            <button type="button" className="p-2 text-slate-400 hover:text-danger hover:bg-danger-light rounded-lg transition-colors" onClick={() => removeRow(i)}>
              <Trash2 size={16} />
            </button>
          </div>
        ))}
        {items.length === 0 && <p className="text-xs text-slate-500 italic">No prescriptions added.</p>}
      </div>
    </div>
  )
}

function ServiceRequestBuilder({ items, setItems }) {
  const addRow = () => setItems([...items, { serviceType: 'LAB_TEST', serviceDetails: '' }])
  const removeRow = (i) => setItems(items.filter((_, idx) => idx !== i))
  const update = (i, field, value) => {
    const copy = items.map((row, idx) => idx === i ? { ...row, [field]: value } : row)
    setItems(copy)
  }
  return (
    <div className="space-y-3 mt-6">
      <div className="flex items-center justify-between border-b border-slate-200 pb-2">
        <h4 className="font-semibold text-slate-800 flex items-center gap-2 m-0">
          <Stethoscope size={18} className="text-primary" /> Service Requests
        </h4>
        <button type="button" className="btn btn-secondary py-1 px-3 text-xs" onClick={addRow}>
          <Plus size={14} /> Add
        </button>
      </div>
      <div className="space-y-2">
        {items.map((row, i) => (
          <div key={i} className="flex gap-2 items-start bg-slate-50 p-2 rounded-lg border border-slate-200">
            <select className="input-field py-1.5 px-3 text-sm w-40 shrink-0" value={row.serviceType} onChange={(e) => update(i, 'serviceType', e.target.value)}>
              <option value="LAB_TEST">Lab Test</option>
              <option value="RADIOLOGY">Radiology</option>
              <option value="IPD_ADMISSION">IPD Admission</option>
            </select>
            <input className="input-field py-1.5 px-3 text-sm flex-1" placeholder="Details (e.g. CBC, Chest X-Ray)" value={row.serviceDetails} onChange={(e) => update(i, 'serviceDetails', e.target.value)} />
            <button type="button" className="p-2 text-slate-400 hover:text-danger hover:bg-danger-light rounded-lg transition-colors" onClick={() => removeRow(i)}>
              <Trash2 size={16} />
            </button>
          </div>
        ))}
        {items.length === 0 && <p className="text-xs text-slate-500 italic">No service requests added.</p>}
      </div>
    </div>
  )
}

export default function DoctorWorkbench() {
  const [query, setQuery] = useState('')
  const [patients, setPatients] = useState([])
  const [selectedPatient, setSelectedPatient] = useState(null)
  const [history, setHistory] = useState([])
  const [selectedRecord, setSelectedRecord] = useState(null)
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  const [encounterDate, setEncounterDate] = useState(new Date().toISOString().slice(0, 10))
  const [chiefComplaints, setChiefComplaints] = useState('')
  const [objectiveFindings, setObjectiveFindings] = useState('')
  const [diagnosis, setDiagnosis] = useState('')
  const [nextFollowUpDate, setNextFollowUpDate] = useState('')
  const [prescriptions, setPrescriptions] = useState([])
  const [serviceRequests, setServiceRequests] = useState([])

  const handleSearch = useCallback(async (q) => {
    setQuery(q)
    if (!q.trim()) { setPatients([]); return }
    try {
      const res = await api.searchPatients({ q })
      setPatients(Array.isArray(res) ? res : (res?.content || res?.items || []))
    } catch { setPatients([]) }
  }, [])

  const handleSelectPatient = async (p) => {
    setSelectedPatient(p)
    setSelectedRecord(null)
    try {
      const h = await api.getPatientHistory(p.id)
      setHistory(h)
    } catch (e) {
      setHistory([])
      toast.error(e.message)
    }
  }

  const handleSave = async () => {
    if (!selectedPatient || !chiefComplaints.trim()) return
    setSaving(true)
    try {
      await api.createMedicalRecord({
        patientId: selectedPatient.id,
        encounterDate,
        chiefComplaints,
        objectiveFindings,
        diagnosis,
        nextFollowUpDate: nextFollowUpDate || null,
        prescriptions: prescriptions.filter((p) => p.medicineName),
        serviceRequests: serviceRequests.filter((s) => s.serviceDetails),
      })
      toast.success('Encounter saved successfully')
      
      const h = await api.getPatientHistory(selectedPatient.id)
      setHistory(h)
      
      // Reset form
      setChiefComplaints('')
      setObjectiveFindings('')
      setDiagnosis('')
      setNextFollowUpDate('')
      setPrescriptions([])
      setServiceRequests([])
      setSelectedRecord(null)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="flex flex-col lg:flex-row gap-6 h-[calc(100vh-64px)] overflow-hidden animate-in fade-in duration-500">
      {/* Left Sidebar - Patient Search */}
      <Card padding="p-0" className="w-full lg:w-80 flex flex-col shrink-0 overflow-hidden border-none shadow-sm h-full">
        <div className="p-4 border-b border-slate-200 bg-slate-50">
          <h3 className="font-bold text-slate-800 m-0 mb-3 flex items-center gap-2">
            <User size={18} className="text-primary" /> Patients
          </h3>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input 
              type="search" 
              className="input-field pl-9 py-2 text-sm" 
              placeholder="Search patients…" 
              value={query} 
              onChange={(e) => handleSearch(e.target.value)} 
            />
          </div>
        </div>
        
        <div className="flex-1 overflow-y-auto p-2 no-scrollbar">
          {(!Array.isArray(patients) || patients.length === 0) && query && (
            <p className="text-center text-sm text-slate-500 mt-4">No patients found.</p>
          )}
          {(Array.isArray(patients) ? patients : (patients?.content || [])).map((p) => (
            <div 
              key={p.id} 
              className={`p-3 rounded-xl cursor-pointer transition-colors flex items-center gap-3 mb-1 ${
                selectedPatient?.id === p.id 
                  ? 'bg-primary text-white shadow-md' 
                  : 'hover:bg-slate-100 text-slate-700'
              }`} 
              onClick={() => handleSelectPatient(p)}
            >
              <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm shrink-0 ${
                selectedPatient?.id === p.id ? 'bg-white/20 text-white' : 'bg-slate-200 text-slate-600'
              }`}>
                {p.firstName[0]}{p.lastName[0]}
              </div>
              <div className="min-w-0">
                <p className="font-semibold text-sm truncate m-0 leading-tight">{p.firstName} {p.lastName}</p>
                <p className={`text-xs truncate m-0 mt-0.5 ${selectedPatient?.id === p.id ? 'text-white/80' : 'text-slate-500'}`}>
                  {p.phoneNumber}
                </p>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Right Area - Workbench */}
      <div className="flex-1 flex flex-col h-full overflow-hidden">
        {!selectedPatient ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-400">
            <div className="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mb-4">
              <ClipboardList size={40} className="text-slate-300" />
            </div>
            <h2 className="text-xl font-bold text-slate-800 mb-2">Workbench Empty</h2>
            <p className="max-w-xs">Search and select a patient from the list on the left to begin consultation.</p>
          </div>
        ) : (
          <div className="flex flex-col h-full overflow-hidden">
            <div className="mb-4 flex items-center gap-4">
              <div className="w-14 h-14 rounded-2xl bg-primary text-white flex items-center justify-center font-bold text-xl shadow-sm">
                {selectedPatient.firstName[0]}{selectedPatient.lastName[0]}
              </div>
              <div>
                <h2 className="text-2xl font-bold text-slate-800 m-0">{selectedPatient.firstName} {selectedPatient.lastName}</h2>
                <div className="flex gap-4 text-sm text-slate-500 mt-1 font-medium">
                  <span>ID: {selectedPatient.id}</span>
                  <span>Phone: {selectedPatient.phoneNumber}</span>
                  <span>DOB: {selectedPatient.dateOfBirth}</span>
                </div>
              </div>
            </div>

            <div className="flex flex-col xl:flex-row gap-6 flex-1 overflow-hidden">
              {/* History Column */}
              <Card padding="p-0" className="w-full xl:w-[400px] flex flex-col shrink-0 overflow-hidden">
                <div className="p-4 border-b border-slate-200 bg-slate-50">
                  <h3 className="font-bold text-slate-800 m-0 flex items-center gap-2">
                    <FileText size={18} className="text-primary" /> Encounter History
                  </h3>
                </div>
                <div className="flex-1 overflow-y-auto p-4 no-scrollbar bg-slate-50/50">
                  {selectedRecord ? (
                    <div className="animate-in slide-in-from-right-4 duration-300">
                      <button 
                        className="btn btn-ghost py-1.5 px-3 text-xs mb-4" 
                        onClick={() => setSelectedRecord(null)}
                      >
                        ← Back to list
                      </button>
                      <Card className="shadow-sm border-primary/20 bg-white">
                        <div className="space-y-3 text-sm text-slate-600">
                          <p><strong className="text-slate-800">Date:</strong> {selectedRecord.encounterDate}</p>
                          <p><strong className="text-slate-800">Doctor:</strong> {selectedRecord.doctorName}</p>
                          <div className="pt-2 border-t border-slate-100"></div>
                          <p><strong className="text-slate-800 block mb-1">Chief Complaints:</strong> {selectedRecord.chiefComplaints}</p>
                          {selectedRecord.objectiveFindings && <p><strong className="text-slate-800 block mb-1">Findings:</strong> {selectedRecord.objectiveFindings}</p>}
                          {selectedRecord.diagnosis && <p><strong className="text-slate-800">Diagnosis:</strong> {selectedRecord.diagnosis}</p>}
                          {selectedRecord.nextFollowUpDate && <p><strong className="text-slate-800">Follow-up:</strong> {selectedRecord.nextFollowUpDate}</p>}
                          
                          {selectedRecord.prescriptions?.length > 0 && (
                            <div className="pt-2 border-t border-slate-100">
                              <strong className="text-slate-800 flex items-center gap-1 mb-2"><Pill size={14}/> Prescriptions:</strong>
                              <ul className="space-y-1 pl-0 m-0 list-none">
                                {selectedRecord.prescriptions.map((p) => (
                                  <li key={p.id} className="bg-slate-50 p-2 rounded border border-slate-100 text-xs">
                                    <strong className="text-primary">{p.medicineName}</strong> — {p.dosage}, {p.frequency}, {p.duration}
                                    {p.instructions ? <span className="block mt-0.5 text-slate-500">{p.instructions}</span> : ''}
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}
                          
                          {selectedRecord.serviceRequests?.length > 0 && (
                            <div className="pt-2 border-t border-slate-100">
                              <strong className="text-slate-800 flex items-center gap-1 mb-2"><Activity size={14}/> Service Requests:</strong>
                              <ul className="space-y-1 pl-0 m-0 list-none">
                                {selectedRecord.serviceRequests.map((s) => (
                                  <li key={s.id} className="bg-slate-50 p-2 rounded border border-slate-100 text-xs">
                                    <strong>{s.serviceType}:</strong> {s.serviceDetails} 
                                    <span className={`ml-2 px-1.5 py-0.5 rounded text-[10px] font-bold ${s.status === 'COMPLETED' ? 'bg-success-light text-success' : 'bg-warning-light text-warning'}`}>{s.status}</span>
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </div>
                      </Card>
                    </div>
                  ) : (
                    <PatientHistory records={history} onSelect={setSelectedRecord} />
                  )}
                </div>
              </Card>

              {/* New Encounter Form */}
              <Card padding="p-0" className="flex-1 flex flex-col overflow-hidden shadow-sm">
                <div className="p-4 border-b border-slate-200 bg-white flex justify-between items-center">
                  <h3 className="font-bold text-slate-800 m-0 flex items-center gap-2">
                    <Activity size={18} className="text-primary" /> New Consultation
                  </h3>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-slate-500">Date:</span>
                    <input 
                      type="date" 
                      className="input-field py-1 px-2 text-sm w-36" 
                      value={encounterDate} 
                      onChange={(e) => setEncounterDate(e.target.value)} 
                    />
                  </div>
                </div>
                
                <div className="flex-1 overflow-y-auto p-6 no-scrollbar space-y-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="form-label">Chief Complaints <span className="text-danger">*</span></label>
                      <textarea 
                        className="input-field min-h-[100px]" 
                        value={chiefComplaints} 
                        onChange={(e) => setChiefComplaints(e.target.value)} 
                        placeholder="Patient symptoms and reasons for visit..."
                      />
                    </div>
                    <div>
                      <label className="form-label">Objective Findings</label>
                      <textarea 
                        className="input-field min-h-[100px]" 
                        value={objectiveFindings} 
                        onChange={(e) => setObjectiveFindings(e.target.value)} 
                        placeholder="Clinical observations, vitals..."
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="form-label">Diagnosis</label>
                      <input 
                        className="input-field" 
                        value={diagnosis} 
                        onChange={(e) => setDiagnosis(e.target.value)} 
                        placeholder="Primary diagnosis..."
                      />
                    </div>
                    <div>
                      <label className="form-label">Next Follow-up Date</label>
                      <input 
                        type="date" 
                        className="input-field" 
                        value={nextFollowUpDate} 
                        onChange={(e) => setNextFollowUpDate(e.target.value)} 
                      />
                    </div>
                  </div>

                  <div className="border-t border-slate-100 pt-6">
                    <PrescriptionBuilder items={prescriptions} setItems={setPrescriptions} />
                  </div>
                  
                  <div className="border-t border-slate-100 pt-2">
                    <ServiceRequestBuilder items={serviceRequests} setItems={setServiceRequests} />
                  </div>
                </div>
                
                <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-end">
                  <button 
                    className="btn btn-primary min-w-[200px]" 
                    disabled={saving || !chiefComplaints.trim()} 
                    onClick={handleSave}
                  >
                    {saving ? 'Saving...' : 'Save Encounter'}
                  </button>
                </div>
              </Card>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
