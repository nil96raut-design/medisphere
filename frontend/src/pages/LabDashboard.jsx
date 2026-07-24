import React, { useState, useEffect, useCallback } from 'react'
import { api, BASE_URL } from '../api/client'
import SidebarLayout from '../components/SidebarLayout'
import { Card } from '../components/Card'
import { Modal } from '../components/Modal'
import { useToast } from '../context/ToastContext'
import { FlaskConical, ClipboardCheck, FileText, CheckCircle, Activity, ArrowRight } from 'lucide-react'

function ResultEntryForm({ order, onClose, onSaved }) {
  const [resultValues, setResultValues] = useState(order.resultValues || '')
  const [technicianNotes, setTechnicianNotes] = useState(order.technicianNotes || '')
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  const handleSave = async () => {
    setSaving(true)
    try {
      await api.enterLabResults(order.id, { resultValues, technicianNotes })
      toast.success('Results entered successfully')
      onSaved()
      onClose()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal isOpen={true} onClose={onClose} title={`Enter Results — ${order.testName}`}>
      <div className="space-y-4">
        <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl mb-2">
          <p className="text-sm font-semibold text-slate-800 m-0">Patient: {order.patientName}</p>
          <p className="text-xs text-slate-500 m-0 mt-1">Requested by: {order.requestedByName}</p>
        </div>
        
        <div>
          <label className="form-label">Result Values *</label>
          <textarea 
            className="input-field min-h-[120px]" 
            value={resultValues} 
            onChange={(e) => setResultValues(e.target.value)} 
            placeholder="Enter test results, reference ranges, and measurements..." 
          />
        </div>
        
        <div>
          <label className="form-label">Technician Notes</label>
          <textarea 
            className="input-field min-h-[80px]" 
            value={technicianNotes} 
            onChange={(e) => setTechnicianNotes(e.target.value)}
            placeholder="Any anomalies or observations during testing..."
          />
        </div>
        
        <div className="pt-4 flex justify-end gap-3 border-t border-slate-100 mt-2">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving || !resultValues.trim()} onClick={handleSave}>
            {saving ? 'Saving...' : 'Save Results'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default function LabDashboard() {
  const [orders, setOrders] = useState([])
  const [statusFilter, setStatusFilter] = useState('ORDERED')
  const [resultOrder, setResultOrder] = useState(null)
  const toast = useToast()

  const refresh = useCallback(async () => {
    try {
      const data = await api.getLabOrders(statusFilter)
      setOrders(data)
    } catch (e) {
      toast.error(e.message)
    }
  }, [statusFilter, toast])

  useEffect(() => { refresh() }, [refresh])

  const handleSample = async (id) => {
    try {
      await api.collectLabSample(id, { technicianNotes: 'Sample collected' })
      toast.success('Sample marked as collected')
      refresh()
    } catch (e) {
      toast.error(e.message)
    }
  }

  const FILTERS = [
    { key: 'ORDERED', label: 'Pending Collection', icon: Activity },
    { key: 'SAMPLE_COLLECTED', label: 'In Analysis', icon: FlaskConical },
    { key: 'RESULT_READY', label: 'Completed', icon: CheckCircle },
  ]

  return (
    <SidebarLayout activeTab="LAB" onTabChange={() => {}}>
      <div className="space-y-8 animate-in fade-in duration-500 pb-12">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-display font-bold text-slate-800 tracking-tight">Lab Queue</h1>
            <p className="text-slate-500 mt-1">Manage test orders, sample collection, and results entry.</p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2 bg-slate-50 p-2 rounded-xl border border-slate-200">
          {FILTERS.map((f) => {
            const Icon = f.icon
            const isActive = statusFilter === f.key
            return (
              <button 
                key={f.key} 
                className={`flex-1 md:flex-none flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                  isActive 
                    ? 'bg-white text-primary shadow-sm border border-slate-200' 
                    : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100 border border-transparent'
                }`}
                onClick={() => setStatusFilter(f.key)}
              >
                <Icon size={16} className={isActive ? 'text-primary' : 'text-slate-400'} />
                {f.label}
              </button>
            )
          })}
        </div>

        {orders.length === 0 ? (
          <div className="text-center py-20 bg-surface rounded-2xl border border-slate-200 border-dashed">
            <div className="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <ClipboardCheck size={32} className="text-slate-300" />
            </div>
            <h3 className="text-lg font-bold text-slate-800 mb-2">No orders found</h3>
            <p className="text-slate-500 max-w-sm mx-auto">
              There are currently no lab orders in the '{FILTERS.find(f => f.key === statusFilter)?.label}' status.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {orders.map((o) => (
              <Card key={o.id} padding="p-0" className="flex flex-col group overflow-hidden border-slate-200 hover:border-primary/50">
                <div className="p-5 flex-1">
                  <div className="flex justify-between items-start mb-3">
                    <h3 className="font-bold text-slate-800 m-0 text-lg flex items-start gap-2">
                      <FlaskConical size={18} className="text-primary mt-1 shrink-0" /> 
                      <span className="leading-tight">{o.testName}</span>
                    </h3>
                  </div>
                  
                  <div className="space-y-2 mt-4 text-sm text-slate-600">
                    <p className="m-0"><strong className="text-slate-800">Patient:</strong> {o.patientName}</p>
                    <p className="m-0"><strong className="text-slate-800">Requested by:</strong> {o.requestedByName}</p>
                    <p className="m-0"><strong className="text-slate-800">Date:</strong> {o.createdAt?.slice(0, 10)}</p>
                  </div>
                </div>
                
                <div className="bg-slate-50 border-t border-slate-100 p-3 flex">
                  {o.status === 'ORDERED' && (
                    <button className="btn btn-secondary w-full text-sm py-2" onClick={() => handleSample(o.id)}>
                      <CheckCircle size={16} /> Mark Sample Collected
                    </button>
                  )}
                  {o.status === 'SAMPLE_COLLECTED' && (
                    <button className="btn btn-primary w-full text-sm py-2 bg-primary text-white" onClick={() => setResultOrder(o)}>
                      <FileText size={16} /> Enter Results
                    </button>
                  )}
                  {o.status === 'RESULT_READY' && (
                    <a 
                      href={`${BASE_URL}/lab/orders/${o.id}/report`} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="btn w-full bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 text-sm py-2 flex items-center justify-center gap-2"
                    >
                      View Report <ArrowRight size={16} className="text-slate-400" />
                    </a>
                  )}
                </div>
              </Card>
            ))}
          </div>
        )}

        {resultOrder && (
          <ResultEntryForm order={resultOrder} onClose={() => setResultOrder(null)} onSaved={refresh} />
        )}
      </div>
    </SidebarLayout>
  )
}