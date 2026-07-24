import React, { useState, useEffect, useCallback } from 'react'
import { api } from '../api/client'

function AddStockForm({ onClose, onSaved }) {
  const [name, setName] = useState('')
  const [batch, setBatch] = useState('')
  const [expiry, setExpiry] = useState('')
  const [qty, setQty] = useState(1)
  const [price, setPrice] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleSave = async () => {
    setSaving(true)
    setError('')
    try {
      await api.addStock({
        medicineName: name,
        batchNumber: batch,
        expiryDate: expiry,
        quantity: qty,
        unitPrice: price ? Number(price) : null,
      })
      onSaved()
      onClose()
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Add Stock</h3>
        {error && <p className="form-error">{error}</p>}
        <div className="form-group">
          <label>Medicine Name</label>
          <input value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="form-group">
          <label>Batch Number</label>
          <input value={batch} onChange={(e) => setBatch(e.target.value)} />
        </div>
        <div className="form-group">
          <label>Expiry Date</label>
          <input type="date" value={expiry} onChange={(e) => setExpiry(e.target.value)} />
        </div>
        <div className="form-group">
          <label>Quantity</label>
          <input type="number" min="1" value={qty} onChange={(e) => setQty(Number(e.target.value))} />
        </div>
        <div className="form-group">
          <label>Unit Price ($)</label>
          <input type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} />
        </div>
        <div className="modal-actions">
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn--primary" disabled={saving || !name || !batch || !expiry} onClick={handleSave}>
            {saving ? 'Saving…' : 'Add to Inventory'}
          </button>
        </div>
      </div>
    </div>
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
  const [error, setError] = useState('')

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
    setError('')
    try {
      await api.dispenseMedicine({ patientId: Number(patientId), medicineStockId: Number(stockId), quantity: qty })
      onSaved()
      onClose()
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Dispense Medicine</h3>
        {error && <p className="form-error">{error}</p>}
        <div className="form-group">
          <label>Patient</label>
          <input type="search" placeholder="Search patient…" value={patientQuery} onChange={(e) => handlePatientSearch(e.target.value)} />
          <select size={Math.min(patients.length + 1, 5)} value={patientId} onChange={(e) => setPatientId(e.target.value)}>
            <option value="">Select patient…</option>
            {patients.map((p) => (
              <option key={p.id} value={p.id}>{p.firstName} {p.lastName} ({p.phoneNumber})</option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Medicine</label>
          <select value={stockId} onChange={(e) => setStockId(e.target.value)}>
            <option value="">Select…</option>
            {inventory.filter((s) => !s.expired && s.availableQuantity > 0).map((s) => (
              <option key={s.id} value={s.id}>
                {s.medicineName} ({s.availableQuantity} available, batch: {s.batchNumber})
              </option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Quantity</label>
          <input type="number" min="1" value={qty} onChange={(e) => setQty(Number(e.target.value))} />
        </div>
        <div className="modal-actions">
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn--primary" disabled={saving || !stockId || !patientId} onClick={handleSave}>
            {saving ? 'Dispensing…' : 'Dispense'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function PharmacyDashboard() {
  const [inventory, setInventory] = useState([])
  const [lowStock, setLowStock] = useState([])
  const [showAdd, setShowAdd] = useState(false)
  const [showDispense, setShowDispense] = useState(false)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    try {
      const [inv, low] = await Promise.all([api.getPharmacyInventory(), api.getLowStock()])
      setInventory(inv)
      setLowStock(low)
    } catch (e) {
      setError(e.message)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  const expiredItems = inventory.filter((s) => s.expired)
  const lowItems = lowStock.filter((s) => !s.expired)

  return (
    <div className="pharmacy-dashboard">
      {error && <p className="form-error">{error}</p>}
      <div className="pharmacy-actions">
        <button className="btn btn--primary" onClick={() => setShowAdd(true)}>+ Add Stock</button>
        <button className="btn" onClick={() => setShowDispense(true)}>Dispense</button>
      </div>

      {lowItems.length > 0 && (
        <div className="alert alert--warning">
          <strong>{lowItems.length}</strong> item(s) below reorder level
        </div>
      )}

      {expiredItems.length > 0 && (
        <div className="alert alert--danger">
          <strong>{expiredItems.length}</strong> item(s) expired in inventory
        </div>
      )}

      <h3>Inventory ({inventory.length})</h3>
      <div className="stock-table">
        <div className="stock-table__header">
          <span>Medicine</span>
          <span>Batch</span>
          <span>Expiry</span>
          <span>Qty</span>
          <span>Price</span>
          <span>Status</span>
        </div>
        {inventory.map((s) => (
          <div key={s.id} className={`stock-row ${s.expired ? 'stock-row--expired' : ''} ${s.lowStock ? 'stock-row--low' : ''}`}>
            <span>{s.medicineName}</span>
            <span>{s.batchNumber}</span>
            <span>{s.expiryDate}</span>
            <span>{s.availableQuantity}</span>
            <span>${s.unitPrice}</span>
            <span>{s.expired ? 'EXPIRED' : s.lowStock ? 'LOW STOCK' : 'OK'}</span>
          </div>
        ))}
      </div>

      {showAdd && <AddStockForm onClose={() => setShowAdd(false)} onSaved={refresh} />}
      {showDispense && <DispenseForm onClose={() => setShowDispense(false)} onSaved={refresh} />}
    </div>
  )
}
