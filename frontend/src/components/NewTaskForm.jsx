import React, { useEffect, useState } from 'react'
import { api } from '../api/client'

export default function NewTaskForm({ onCreated }) {
  const [patients, setPatients] = useState([])
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const [priority, setPriority] = useState('MEDIUM')
  const [dueDate, setDueDate] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.usersByRole('PATIENT').then(setPatients).catch(() => {})
  }, [])

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (!assigneeId) { setError('Choose a patient to assign this to.'); return }
    setSaving(true)
    try {
      await onCreated({
        title, description, assigneeId: Number(assigneeId), priority,
        dueDate: dueDate || null,
      })
      setTitle(''); setDescription(''); setAssigneeId(''); setPriority('MEDIUM'); setDueDate('')
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="panel new-task-form" onSubmit={submit}>
      <h3>Assign a new task</h3>
      {error && <p className="form-error">{error}</p>}
      <label>
        Title
        <input value={title} onChange={(e) => setTitle(e.target.value)} required placeholder="e.g. Take morning medication" />
      </label>
      <label>
        Details
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} placeholder="Optional instructions" />
      </label>
      <div className="form-row">
        <label>
          Patient
          <select value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)} required>
            <option value="">Select a patient…</option>
            {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
          </select>
        </label>
        <label>
          Priority
          <select value={priority} onChange={(e) => setPriority(e.target.value)}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
        </label>
        <label>
          Due date
          <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
        </label>
      </div>
      <button className="btn btn--primary" type="submit" disabled={saving}>
        {saving ? 'Assigning…' : 'Assign task'}
      </button>
    </form>
  )
}
