import React, { useState, useCallback } from 'react'
import { api } from '../api/client'
import SidebarLayout from '../components/SidebarLayout'
import { Card } from '../components/Card'
import { Modal } from '../components/Modal'
import { useToast } from '../context/ToastContext'
import { Search, User, Receipt, CreditCard, DollarSign, Wallet, FileText, CheckCircle2 } from 'lucide-react'

function PaymentModal({ calculation, onClose, onSaved }) {
  const [discount, setDiscount] = useState(0)
  const [insurance, setInsurance] = useState(0)
  const [mode, setMode] = useState('CASH')
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  const netPayable = Math.max(0, calculation.totalAmount - discount - insurance)

  const handleSettle = async () => {
    setSaving(true)
    try {
      await api.settleBill({
        patientId: calculation.patientId,
        discountAmount: discount,
        insuranceCoveredAmount: insurance,
        paymentMode: mode,
      })
      toast.success('Bill settled successfully')
      onSaved()
      onClose()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal isOpen={true} onClose={onClose} title="Settle Bill">
      <div className="space-y-6">
        <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
          <h4 className="font-bold text-slate-800 m-0 mb-1">{calculation.patientName}</h4>
          <p className="text-sm text-slate-500 m-0">Gross Total: <strong className="text-slate-800">${Number(calculation.totalAmount).toFixed(2)}</strong></p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="form-label">Discount Amount ($)</label>
            <input className="input-field" type="number" min="0" value={discount} onChange={(e) => setDiscount(Number(e.target.value))} />
          </div>
          <div>
            <label className="form-label">Insurance Covered ($)</label>
            <input className="input-field" type="number" min="0" value={insurance} onChange={(e) => setInsurance(Number(e.target.value))} />
          </div>
        </div>
        
        <div>
          <label className="form-label">Payment Mode</label>
          <div className="grid grid-cols-3 gap-2">
            {[
              { id: 'CASH', label: 'Cash', icon: DollarSign },
              { id: 'CARD', label: 'Card', icon: CreditCard },
              { id: 'UPI', label: 'UPI / Digital', icon: Wallet },
            ].map((m) => {
              const Icon = m.icon
              return (
                <button 
                  key={m.id}
                  type="button"
                  className={`flex flex-col items-center justify-center gap-2 p-3 rounded-xl border-2 transition-all ${
                    mode === m.id 
                      ? 'border-primary bg-primary-light/10 text-primary' 
                      : 'border-slate-100 bg-slate-50 text-slate-500 hover:border-slate-200 hover:bg-slate-100'
                  }`}
                  onClick={() => setMode(m.id)}
                >
                  <Icon size={24} />
                  <span className="text-xs font-bold">{m.label}</span>
                </button>
              )
            })}
          </div>
        </div>

        <div className="bg-primary-dark text-white p-4 rounded-xl flex justify-between items-center shadow-md">
          <span className="font-medium text-primary-light">Net Payable</span>
          <span className="text-2xl font-bold tracking-tight">${netPayable.toFixed(2)}</span>
        </div>

        <div className="pt-4 flex justify-end gap-3 border-t border-slate-100 mt-2">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary bg-primary text-white" disabled={saving || netPayable < 0} onClick={handleSettle}>
            {saving ? 'Processing...' : 'Confirm Payment'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default function BillingDashboard() {
  const [query, setQuery] = useState('')
  const [patients, setPatients] = useState([])
  const [selectedPatient, setSelectedPatient] = useState(null)
  const [calculation, setCalculation] = useState(null)
  const [showPayment, setShowPayment] = useState(false)
  const toast = useToast()

  const handleSearch = useCallback(async (q) => {
    setQuery(q)
    if (!q.trim()) { setPatients([]); return }
    try {
      const res = await api.searchPatients({ q })
      setPatients(res)
    } catch { /* ignore */ }
  }, [])

  const handleSelect = async (p) => {
    setSelectedPatient(p)
    try {
      const calc = await api.calculateBill(p.id)
      setCalculation(calc)
    } catch (e) {
      setCalculation(null)
      toast.error(e.message)
    }
  }

  return (
    <SidebarLayout activeTab="BILLING" onTabChange={() => {}}>
      <div className="flex flex-col lg:flex-row gap-6 h-[calc(100vh-64px)] overflow-hidden animate-in fade-in duration-500">
        
        {/* Left Sidebar - Patient Search */}
        <Card padding="p-0" className="w-full lg:w-80 flex flex-col shrink-0 overflow-hidden border-none shadow-sm h-full">
          <div className="p-4 border-b border-slate-200 bg-slate-50">
            <h3 className="font-bold text-slate-800 m-0 mb-3 flex items-center gap-2">
              <User size={18} className="text-primary" /> Select Patient
            </h3>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
              <input 
                type="search" 
                className="input-field pl-9 py-2 text-sm" 
                placeholder="Search by name or phone..." 
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
                onClick={() => handleSelect(p)}
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

        {/* Right Area - Bill Preview */}
        <div className="flex-1 flex flex-col h-full overflow-hidden pb-6 pr-6">
          {!selectedPatient ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center text-slate-400 bg-surface rounded-2xl border border-slate-200 border-dashed">
              <div className="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mb-4">
                <Receipt size={40} className="text-slate-300" />
              </div>
              <h2 className="text-xl font-bold text-slate-800 mb-2">No Patient Selected</h2>
              <p className="max-w-xs text-sm">Search and select a patient from the list on the left to view and settle their bill.</p>
            </div>
          ) : (
            <Card padding="p-0" className="flex-1 flex flex-col overflow-hidden shadow-sm h-full">
              <div className="p-6 border-b border-slate-200 bg-white flex justify-between items-center">
                <div>
                  <h2 className="text-2xl font-bold text-slate-800 m-0">{selectedPatient.firstName} {selectedPatient.lastName}</h2>
                  <p className="text-sm text-slate-500 mt-1 font-medium">Patient ID: {selectedPatient.id}</p>
                </div>
                {calculation && calculation.items.length > 0 && (
                  <button 
                    className="btn btn-primary bg-primary text-white shadow-sm flex items-center gap-2"
                    onClick={() => setShowPayment(true)}
                  >
                    <CreditCard size={18} /> Proceed to Payment
                  </button>
                )}
              </div>
              
              <div className="flex-1 overflow-y-auto bg-slate-50/50 p-6">
                {calculation ? (
                  <div className="max-w-4xl mx-auto">
                    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                      <div className="p-4 border-b border-slate-200 bg-slate-50 flex items-center gap-2">
                        <FileText size={18} className="text-slate-400" />
                        <h3 className="font-bold text-slate-800 m-0">Itemized Bill</h3>
                      </div>
                      
                      {calculation.items.length === 0 ? (
                        <div className="p-12 text-center text-slate-500">
                          <CheckCircle2 size={48} className="mx-auto text-success/50 mb-4" />
                          <p className="text-lg font-medium text-slate-700">No pending charges</p>
                          <p className="text-sm">This patient has no billable items at this time.</p>
                        </div>
                      ) : (
                        <div>
                          <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                              <thead>
                                <tr className="bg-slate-50 border-b border-slate-200 text-xs uppercase tracking-wider text-slate-500">
                                  <th className="p-4 font-semibold">Description</th>
                                  <th className="p-4 font-semibold">Rate</th>
                                  <th className="p-4 font-semibold">Qty</th>
                                  <th className="p-4 font-semibold text-right">Total</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-slate-100">
                                {calculation.items.map((item, i) => (
                                  <tr key={i} className="hover:bg-slate-50/50 transition-colors">
                                    <td className="p-4 text-sm text-slate-800 font-medium">{item.description}</td>
                                    <td className="p-4 text-sm text-slate-600">${Number(item.amount).toFixed(2)}</td>
                                    <td className="p-4 text-sm text-slate-600">{item.quantity}</td>
                                    <td className="p-4 text-sm text-slate-800 font-bold text-right">${Number(item.total).toFixed(2)}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                          <div className="p-6 bg-slate-50 border-t border-slate-200 flex justify-end">
                            <div className="text-right">
                              <p className="text-sm text-slate-500 uppercase tracking-wider font-semibold mb-1">Total Amount</p>
                              <p className="text-3xl font-display font-bold text-slate-800 m-0">${Number(calculation.totalAmount).toFixed(2)}</p>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="h-full flex items-center justify-center text-slate-400">
                    <div className="animate-pulse flex flex-col items-center">
                      <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin mb-4"></div>
                      <p>Calculating bill...</p>
                    </div>
                  </div>
                )}
              </div>
            </Card>
          )}
        </div>
      </div>

      {showPayment && calculation && (
        <PaymentModal 
          calculation={calculation} 
          onClose={() => setShowPayment(false)} 
          onSaved={() => setCalculation(null)} 
        />
      )}
    </SidebarLayout>
  )
}