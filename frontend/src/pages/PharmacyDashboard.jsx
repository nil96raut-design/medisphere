import React, { useState, useEffect, useCallback } from 'react'
import { api } from '../api/client'
import SidebarLayout from '../components/SidebarLayout'
import { Card } from '../components/Card'
import { Modal } from '../components/Modal'
import { useToast } from '../context/ToastContext'
import { Pill, Plus, HandPlatter, AlertTriangle, Package, CalendarOff, Search, User } from 'lucide-react'

function AddStockForm({ onClose, onSaved }) {
  const [name, setName] = useState('')
  const [batch, setBatch] = useState('')
  const [expiry, setExpiry] = useState('')
  const [qty, setQty] = useState(1)
  const [price, setPrice] = useState('')
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  const handleSave = async () => {
    setSaving(true)
    try {
      await api.addStock({
        medicineName: name,
        batchNumber: batch,
        expiryDate: expiry,
        quantity: qty,
        unitPrice: price ? Number(price) : null,
      })
      toast.success('Stock added successfully')
      onSaved()
      onClose()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal isOpen={true} onClose={onClose} title="Add Stock">
      <div className="space-y-4">
        <div>
          <label className="form-label">Medicine Name *</label>
          <input className="input-field" value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Paracetamol 500mg" />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="form-label">Batch Number *</label>
            <input className="input-field" value={batch} onChange={(e) => setBatch(e.target.value)} placeholder="e.g. BATCH-A123" />
          </div>
          <div>
            <label className="form-label">Expiry Date *</label>
            <input className="input-field" type="date" value={expiry} onChange={(e) => setExpiry(e.target.value)} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="form-label">Quantity</label>
            <input className="input-field" type="number" min="1" value={qty} onChange={(e) => setQty(Number(e.target.value))} />
          </div>
          <div>
            <label className="form-label">Unit Price ($)</label>
            <input className="input-field" type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} placeholder="0.00" />
          </div>
        </div>
        <div className="pt-4 flex justify-end gap-3 border-t border-slate-100 mt-2">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving || !name || !batch || !expiry} onClick={handleSave}>
            {saving ? 'Saving...' : 'Add to Inventory'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

function DispenseForm({ onClose, onSaved }) {
  const [stockId, setStockId] = useState('')
  const [qty, setQty] = useState(1)
  const [patientId, setPatientId] = useState('')
  const [patientQuery, setPatientQuery] = useState('')
  const [patients, setPatients] = useState([])
  const [inventory, setInventory] = useState([])
  const [saving, setSaving] = useState(false)
  const toast = useToast()

  useEffect(() => {
    api.getPharmacyInventory().then(setInventory).catch(() => {})
  }, [])

  const handlePatientSearch = async (q) => {
    setPatientQuery(q)
    if (!q.trim()) { setPatients([]); return }
    try {
      const res = await api.searchPatients(q)
      setPatients(res)
    } catch { /* ignore */ }
  }

  const handleSave = async () => {
    setSaving(true)
    try {
      await api.dispenseMedicine({ patientId: Number(patientId), medicineStockId: Number(stockId), quantity: qty })
      toast.success('Medicine dispensed successfully')
      onSaved()
      onClose()
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal isOpen={true} onClose={onClose} title="Dispense Medicine">
      <div className="space-y-4">
        <div>
          <label className="form-label">Patient</label>
          <div className="relative mb-2">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input 
              className="input-field pl-9" 
              type="search" 
              placeholder="Search patient by name or phone..." 
              value={patientQuery} 
              onChange={(e) => handlePatientSearch(e.target.value)} 
            />
          </div>
          <select 
            className="input-field min-h-[120px]" 
            size={Math.min(patients.length + 1, 5)} 
            value={patientId} 
            onChange={(e) => setPatientId(e.target.value)}
          >
            <option value="" disabled>Select patient from results...</option>
            {patients.map((p) => (
              <option key={p.id} value={p.id}>{p.firstName} {p.lastName} ({p.phoneNumber})</option>
            ))}
          </select>
        </div>
        
        <div>
          <label className="form-label">Medicine</label>
          <select className="input-field" value={stockId} onChange={(e) => setStockId(e.target.value)}>
            <option value="">Select medicine from stock...</option>
            {inventory.filter((s) => !s.expired && s.availableQuantity > 0).map((s) => (
              <option key={s.id} value={s.id}>
                {s.medicineName} ({s.availableQuantity} available, batch: {s.batchNumber})
              </option>
            ))}
          </select>
        </div>
        
        <div>
          <label className="form-label">Quantity</label>
          <input className="input-field w-32" type="number" min="1" value={qty} onChange={(e) => setQty(Number(e.target.value))} />
        </div>
        
        <div className="pt-4 flex justify-end gap-3 border-t border-slate-100 mt-2">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving || !stockId || !patientId} onClick={handleSave}>
            {saving ? 'Dispensing...' : 'Dispense'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default function PharmacyDashboard() {
  const [inventory, setInventory] = useState([])
  const [lowStock, setLowStock] = useState([])
  const [showAdd, setShowAdd] = useState(false)
  const [showDispense, setShowDispense] = useState(false)
  const toast = useToast()

  const refresh = useCallback(async () => {
    try {
      const [inv, low] = await Promise.all([api.getPharmacyInventory(), api.getLowStock()])
      setInventory(inv)
      setLowStock(low)
    } catch (e) {
      toast.error(e.message)
    }
  }, [toast])

  useEffect(() => { refresh() }, [refresh])

  const expiredItems = inventory.filter((s) => s.expired)
  const lowItems = lowStock.filter((s) => !s.expired)

  return (
    <SidebarLayout activeTab="PHARMACY" onTabChange={() => {}}>
      <div className="space-y-8 animate-in fade-in duration-500 pb-12">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-display font-bold text-slate-800 tracking-tight">Pharmacy</h1>
            <p className="text-slate-500 mt-1">Manage inventory, stock levels, and dispensing.</p>
          </div>
          <div className="flex gap-3">
            <button className="btn btn-secondary" onClick={() => setShowAdd(true)}>
              <Plus size={18} /> Add Stock
            </button>
            <button className="btn btn-primary bg-primary text-white" onClick={() => setShowDispense(true)}>
              <HandPlatter size={18} /> Dispense
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {lowItems.length > 0 && (
            <div className="bg-warning-light border border-warning/30 p-4 rounded-xl flex items-start gap-4 animate-in fade-in slide-in-from-top-2">
              <AlertTriangle className="text-warning mt-0.5 shrink-0" size={24} />
              <div>
                <h4 className="font-bold text-warning-dark m-0">Low Stock Alert</h4>
                <p className="text-sm text-warning-dark/80 mt-1 mb-0"><strong>{lowItems.length}</strong> item(s) have fallen below the reorder level and need restocking.</p>
              </div>
            </div>
          )}

          {expiredItems.length > 0 && (
            <div className="bg-danger-light border border-danger/30 p-4 rounded-xl flex items-start gap-4 animate-in fade-in slide-in-from-top-2">
              <CalendarOff className="text-danger mt-0.5 shrink-0" size={24} />
              <div>
                <h4 className="font-bold text-danger-dark m-0">Expired Stock Alert</h4>
                <p className="text-sm text-danger-dark/80 mt-1 mb-0"><strong>{expiredItems.length}</strong> item(s) have expired and should be removed from inventory.</p>
              </div>
            </div>
          )}
        </div>

        <div>
          <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
            <Package size={20} className="text-primary" /> Inventory ({inventory.length})
          </h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {inventory.map((s) => (
              <Card 
                key={s.id} 
                padding="p-0"
                className={`flex flex-col overflow-hidden border-l-4 ${
                  s.expired 
                    ? 'border-l-danger bg-danger-light/10' 
                    : s.lowStock 
                      ? 'border-l-warning bg-warning-light/10' 
                      : 'border-l-primary'
                }`}
              >
                <div className="p-4 flex-1">
                  <div className="flex justify-between items-start mb-2">
                    <h4 className="font-bold text-slate-800 m-0 flex items-center gap-2">
                      <Pill size={16} className="text-slate-400" /> {s.medicineName}
                    </h4>
                    {s.expired ? (
                      <span className="status-tag bg-danger text-white">EXPIRED</span>
                    ) : s.lowStock ? (
                      <span className="status-tag bg-warning text-white">LOW STOCK</span>
                    ) : (
                      <span className="status-tag bg-success-light text-success">OK</span>
                    )}
                  </div>
                  
                  <div className="grid grid-cols-2 gap-y-2 mt-4 text-sm text-slate-600">
                    <div>
                      <p className="text-xs text-slate-400 m-0 uppercase tracking-wider font-semibold mb-0.5">Quantity</p>
                      <p className="font-bold text-slate-800 m-0 text-lg">{s.availableQuantity}</p>
                    </div>
                    <div>
                      <p className="text-xs text-slate-400 m-0 uppercase tracking-wider font-semibold mb-0.5">Price</p>
                      <p className="font-bold text-slate-800 m-0 text-lg">${s.unitPrice}</p>
                    </div>
                    <div>
                      <p className="text-xs text-slate-400 m-0 uppercase tracking-wider font-semibold mb-0.5 mt-2">Batch</p>
                      <p className="font-medium m-0">{s.batchNumber}</p>
                    </div>
                    <div>
                      <p className="text-xs text-slate-400 m-0 uppercase tracking-wider font-semibold mb-0.5 mt-2">Expiry Date</p>
                      <p className={`font-medium m-0 ${s.expired ? 'text-danger' : ''}`}>{s.expiryDate}</p>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
            
            {inventory.length === 0 && (
              <div className="col-span-full py-16 text-center text-slate-400 bg-surface rounded-2xl border border-slate-200 border-dashed">
                <Package size={48} className="mx-auto text-slate-200 mb-4" />
                <p className="text-lg font-medium text-slate-600">Inventory is empty</p>
                <p>Click "Add Stock" above to populate the pharmacy inventory.</p>
              </div>
            )}
          </div>
        </div>

        {showAdd && <AddStockForm onClose={() => setShowAdd(false)} onSaved={refresh} />}
        {showDispense && <DispenseForm onClose={() => setShowDispense(false)} onSaved={refresh} />}
      </div>
    </SidebarLayout>
  )
}