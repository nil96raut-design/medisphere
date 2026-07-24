import React, { useState, useCallback } from 'react'
import { api } from '../api/client'

function PaymentModal({ calculation, onClose, onSaved }) {
  const [discount, setDiscount] = useState(0)
  const [insurance, setInsurance] = useState(0)
  const [mode, setMode] = useState('CASH')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const netPayable = Math.max(0, calculation.totalAmount - discount - insurance)

  const handleSettle = async () => {
    setSaving(true)
    setError('')
    try {
      await api.settleBill({
        patientId: calculation.patientId,
        discountAmount: discount,
        insuranceCoveredAmount: insurance,
        paymentMode: mode,
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
      <div className="modal modal--wide" onClick={(e) => e.stopPropagation()}>
        <h3>Payment — {calculation.patientName}</h3>
        {error && <p className="form-error">{error}</p>}
        <table className="bill-table">
          <thead>
            <tr><th>Item</th><th>Rate</th><th>Qty</th><th>Total</th></tr>
          </thead>
          <tbody>
            {calculation.items.map((item, i) => (
              <tr key={i}>
                <td>{item.description}</td>
                <td>${Number(item.amount).toFixed(2)}</td>
                <td>{item.quantity}</td>
                <td>${Number(item.total).toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr><td colSpan="3"><strong>Total</strong></td><td><strong>${Number(calculation.totalAmount).toFixed(2)}</strong></td></tr>
          </tfoot>
        </table>

        <div className="form-row">
          <div className="form-group">
            <label>Discount ($)</label>
            <input type="number" min="0" value={discount} onChange={(e) => setDiscount(Number(e.target.value))} />
          </div>
          <div className="form-group">
            <label>Insurance ($)</label>
            <input type="number" min="0" value={insurance} onChange={(e) => setInsurance(Number(e.target.value))} />
          </div>
          <div className="form-group">
            <label>Payment Mode</label>
            <select value={mode} onChange={(e) => setMode(e.target.value)}>
              <option value="CASH">Cash</option>
              <option value="CARD">Card</option>
              <option value="UPI">UPI</option>
            </select>
          </div>
        </div>

        <div className="bill-net">
          <strong>Net Payable: ${netPayable.toFixed(2)}</strong>
        </div>

        <div className="modal-actions">
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn--primary" disabled={saving || netPayable < 0} onClick={handleSettle}>
            {saving ? 'Processing…' : `Settle $${netPayable.toFixed(2)}`}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function BillingDashboard() {
  const [query, setQuery] = useState('')
  const [patients, setPatients] = useState([])
  const [selectedPatient, setSelectedPatient] = useState(null)
  const [calculation, setCalculation] = useState(null)
  const [showPayment, setShowPayment] = useState(false)
  const [error, setError] = useState('')

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
    setError('')
    try {
      const calc = await api.calculateBill(p.id)
      setCalculation(calc)
    } catch (e) {
      setCalculation(null)
      setError(e.message)
    }
  }

  return (
    <div className="billing-dashboard">
      <h2>Billing</h2>
      {error && <p className="form-error">{error}</p>}
      <input type="search" className="search-input" placeholder="Search patient…" value={query} onChange={(e) => handleSearch(e.target.value)} />
      <div className="patient-list">
        {patients.map((p) => (
          <div key={p.id} className={`patient-item ${selectedPatient?.id === p.id ? 'is-selected' : ''}`} onClick={() => handleSelect(p)}>
            <strong>{p.firstName} {p.lastName}</strong>
            <small className="muted">{p.phoneNumber}</small>
          </div>
        ))}
      </div>

      {calculation && (
        <div className="bill-preview">
          <h3>{calculation.patientName}</h3>
          <table className="bill-table">
            <thead>
              <tr><th>Item</th><th>Rate</th><th>Qty</th><th>Total</th></tr>
            </thead>
            <tbody>
              {calculation.items.map((item, i) => (
                <tr key={i}>
                  <td>{item.description}</td>
                  <td>${Number(item.amount).toFixed(2)}</td>
                  <td>{item.quantity}</td>
                  <td>${Number(item.total).toFixed(2)}</td>
                </tr>
              ))}
              {calculation.items.length === 0 && (
                <tr><td colSpan="4" className="muted">No billable items found.</td></tr>
              )}
            </tbody>
            <tfoot>
              <tr><td colSpan="3"><strong>Total</strong></td><td><strong>${Number(calculation.totalAmount).toFixed(2)}</strong></td></tr>
            </tfoot>
          </table>
          <button className="btn btn--primary" onClick={() => setShowPayment(true)} disabled={calculation.items.length === 0}>
            Proceed to Payment
          </button>
        </div>
      )}

      {showPayment && calculation && (
        <PaymentModal calculation={calculation} onClose={() => setShowPayment(false)} onSaved={() => setCalculation(null)} />
      )}
    </div>
  )
}
