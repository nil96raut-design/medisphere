import React, { useState, useEffect, useCallback } from 'react'
import { api, BASE_URL } from '../api/client'

function ResultEntryForm({ order, onClose, onSaved }) {
  const [resultValues, setResultValues] = useState(order.resultValues || '')
  const [technicianNotes, setTechnicianNotes] = useState(order.technicianNotes || '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleSave = async () => {
    setSaving(true)
    setError('')
    try {
      await api.enterLabResults(order.id, { resultValues, technicianNotes })
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
        <h3>Enter Results — {order.testName}</h3>
        <p className="muted">{order.patientName}</p>
        {error && <p className="form-error">{error}</p>}
        <div className="form-group">
          <label>Result Values</label>
          <textarea rows="5" value={resultValues} onChange={(e) => setResultValues(e.target.value)} placeholder="Test results, reference ranges, notes…" />
        </div>
        <div className="form-group">
          <label>Technician Notes</label>
          <textarea rows="2" value={technicianNotes} onChange={(e) => setTechnicianNotes(e.target.value)} />
        </div>
        <div className="modal-actions">
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn--primary" disabled={saving || !resultValues.trim()} onClick={handleSave}>
            {saving ? 'Saving…' : 'Save Results'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function LabDashboard() {
  const [orders, setOrders] = useState([])
  const [statusFilter, setStatusFilter] = useState('ORDERED')
  const [resultOrder, setResultOrder] = useState(null)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    try {
      const data = await api.getLabOrders(statusFilter)
      setOrders(data)
    } catch (e) {
      setError(e.message)
    }
  }, [statusFilter])

  useEffect(() => { refresh() }, [refresh])

  const handleSample = async (id) => {
    try {
      await api.collectLabSample(id, { technicianNotes: 'Sample collected' })
      refresh()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="lab-dashboard">
      <h2>Lab Queue</h2>
      {error && <p className="form-error">{error}</p>}
      <div className="filter-row">
        {['ORDERED', 'SAMPLE_COLLECTED', 'RESULT_READY'].map((s) => (
          <button key={s} className={`filter-chip ${statusFilter === s ? 'is-active' : ''}`} onClick={() => setStatusFilter(s)}>
            {s.replace('_', ' ')}
          </button>
        ))}
      </div>
      <div className="lab-orders">
        {orders.map((o) => (
          <div key={o.id} className="order-card">
            <div className="order-card__header">
              <strong>{o.testName}</strong>
              <span className="badge badge--lab">{o.status}</span>
            </div>
            <p className="muted">{o.patientName} — Requested by {o.requestedByName}</p>
            <p><small>{o.createdAt?.slice(0, 10)}</small></p>
            {o.status === 'ORDERED' && (
              <button className="btn btn--sm" onClick={() => handleSample(o.id)}>Collect Sample</button>
            )}
            {o.status === 'SAMPLE_COLLECTED' && (
              <button className="btn btn--sm btn--primary" onClick={() => setResultOrder(o)}>Enter Results</button>
            )}
            {o.status === 'RESULT_READY' && (
              <a className="btn btn--sm" href={`${BASE_URL}/lab/orders/${o.id}/report`} target="_blank" rel="noopener noreferrer">
                View Report
              </a>
            )}
          </div>
        ))}
        {orders.length === 0 && <p className="muted">No orders in this status.</p>}
      </div>
      {resultOrder && (
        <ResultEntryForm order={resultOrder} onClose={() => setResultOrder(null)} onSaved={refresh} />
      )}
    </div>
  )
}
