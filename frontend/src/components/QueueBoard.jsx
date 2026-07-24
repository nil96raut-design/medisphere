import React, { useState, useEffect, useCallback } from 'react'
import { api } from '../api/client'
import { Card } from './Card'
import SidebarLayout from './SidebarLayout'
import { useToast } from '../context/ToastContext'
import { Stethoscope, Clock, CheckCircle, XCircle, User, ArrowRight, Loader2 } from 'lucide-react'

const STATUS_STYLES = {
  SCHEDULED: { label: 'Scheduled', bg: 'bg-blue-50 text-blue-600 border-blue-200' },
  CHECKED_IN: { label: 'Checked In', bg: 'bg-amber-50 text-amber-600 border-amber-200' },
  IN_CONSULTATION: { label: 'In Consultation', bg: 'bg-purple-50 text-purple-600 border-purple-200' },
  COMPLETED: { label: 'Completed', bg: 'bg-emerald-50 text-emerald-600 border-emerald-200' },
  CANCELLED: { label: 'Cancelled', bg: 'bg-slate-50 text-slate-500 border-slate-200' },
}

export default function QueueBoard() {
  const [doctors, setDoctors] = useState([])
  const [selectedDoctor, setSelectedDoctor] = useState(null)
  const [queue, setQueue] = useState([])
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  useEffect(() => {
    api.getAvailableDoctors().then(setDoctors).catch(() => {})
  }, [])

  const loadQueue = useCallback((doctorId) => {
    setLoading(true)
    api.getQueue(doctorId)
      .then(setQueue)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const selectDoctor = (doc) => {
    setSelectedDoctor(doc)
    loadQueue(doc.id)
  }

  const act = async (apptId, status, msg) => {
    try {
      await api.updateAppointmentStatus(apptId, { status })
      toast.success(msg)
      if (selectedDoctor) loadQueue(selectedDoctor.id)
    } catch (e) {
      toast.error(e.message)
    }
  }

  const activeQueue = queue.filter((e) => e.status !== 'COMPLETED' && e.status !== 'CANCELLED')

  return (
    <SidebarLayout activeTab="QUEUE" onTabChange={() => {}}>
      <div className="space-y-8 animate-in fade-in duration-500 pb-12">
        <div>
          <h1 className="text-3xl font-display font-bold text-slate-800 tracking-tight">Queue Management</h1>
          <p className="text-slate-500 mt-1">Select a doctor to view and manage today's patient queue.</p>
        </div>

        <div className="flex flex-wrap gap-2 bg-slate-50 p-2 rounded-xl border border-slate-200">
          {doctors.map((doc) => (
            <button
              key={doc.id}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                selectedDoctor?.id === doc.id
                  ? 'bg-white text-primary shadow-sm border border-slate-200'
                  : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100 border border-transparent'
              }`}
              onClick={() => selectDoctor(doc)}
            >
              <Stethoscope size={16} className={selectedDoctor?.id === doc.id ? 'text-primary' : 'text-slate-400'} />
              {doc.fullName}
            </button>
          ))}
        </div>

        {!selectedDoctor && (
          <div className="text-center py-20 bg-surface rounded-2xl border border-slate-200 border-dashed">
            <div className="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <User size={32} className="text-slate-300" />
            </div>
            <h3 className="text-lg font-bold text-slate-800 mb-2">Select a Doctor</h3>
            <p className="text-slate-500 max-w-sm mx-auto">Choose a doctor from the list above to view their patient queue for today.</p>
          </div>
        )}

        {selectedDoctor && (
          <>
            <Card padding="p-5" className="border-primary/20 bg-gradient-to-br from-primary/5 to-transparent">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center">
                  <Stethoscope className="text-primary" size={28} />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-slate-800 m-0">{selectedDoctor.fullName}</h2>
                  <p className="text-sm text-slate-500 mt-0.5">
                    {selectedDoctor.specialization || 'General'}
                    {selectedDoctor.consultationFee && ` · Fee: $${selectedDoctor.consultationFee}`}
                  </p>
                </div>
              </div>
            </Card>

            {loading && (
              <div className="flex items-center justify-center py-16 text-slate-400">
                <Loader2 size={24} className="animate-spin mr-3" />
                <span className="text-lg font-medium">Loading queue...</span>
              </div>
            )}

            {!loading && activeQueue.length === 0 && (
              <div className="text-center py-20 bg-surface rounded-2xl border border-slate-200 border-dashed">
                <div className="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
                  <CheckCircle size={32} className="text-slate-300" />
                </div>
                <h3 className="text-lg font-bold text-slate-800 mb-2">Queue is Clear</h3>
                <p className="text-slate-500">No pending appointments for this doctor today.</p>
              </div>
            )}

            {!loading && activeQueue.length > 0 && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {activeQueue.map((entry, idx) => {
                  const s = STATUS_STYLES[entry.status] || STATUS_STYLES.SCHEDULED
                  return (
                    <Card key={entry.appointmentId} padding="p-0" className="flex flex-col overflow-hidden border-slate-200 hover:border-primary/50 transition-all hover:shadow-md group">
                      <div className="p-5 flex-1">
                        <div className="flex justify-between items-start mb-4">
                          <div className="flex items-center gap-3">
                            <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold text-xl">
                              {entry.tokenNumber || '—'}
                            </div>
                            <div>
                              <h3 className="font-bold text-slate-800 m-0 text-lg">{entry.patientName}</h3>
                              <div className="flex items-center gap-1 text-xs text-slate-500 mt-0.5">
                                <Clock size={12} /> {entry.startTime}
                              </div>
                            </div>
                          </div>
                          <span className={`text-xs font-bold px-2.5 py-1 rounded-lg border ${s.bg}`}>
                            {s.label}
                          </span>
                        </div>

                        <div className="flex gap-1">
                          {entry.status === 'SCHEDULED' && (
                            <button className="btn btn-primary text-sm flex-1 bg-primary text-white" onClick={() => act(entry.appointmentId, 'CHECKED_IN', `${entry.patientName} checked in`)}>
                              <CheckCircle size={16} /> Check In
                            </button>
                          )}
                          {entry.status === 'CHECKED_IN' && (
                            <button className="btn btn-primary text-sm flex-1 bg-primary text-white" onClick={() => act(entry.appointmentId, 'IN_CONSULTATION', `Consultation started for ${entry.patientName}`)}>
                              <ArrowRight size={16} /> Start Consultation
                            </button>
                          )}
                          {entry.status === 'IN_CONSULTATION' && (
                            <button className="btn btn-primary text-sm flex-1 bg-success text-white" onClick={() => act(entry.appointmentId, 'COMPLETED', `Consultation completed for ${entry.patientName}`)}>
                              <CheckCircle size={16} /> Complete
                            </button>
                          )}
                        </div>
                        {entry.status !== 'IN_CONSULTATION' && (
                          <button className="text-xs text-slate-400 hover:text-danger mt-2 transition-colors" onClick={() => act(entry.appointmentId, 'CANCELLED', `Appointment cancelled for ${entry.patientName}`)}>
                            <XCircle size={14} className="inline mr-1" />Cancel
                          </button>
                        )}
                      </div>
                    </Card>
                  )
                })}
              </div>
            )}
          </>
        )}
      </div>
    </SidebarLayout>
  )
}
