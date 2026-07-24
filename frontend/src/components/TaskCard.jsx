import React, { useState } from 'react'
import PulseBar from './PulseBar'
import { Card } from './Card'
import { Calendar, User, UserCheck, ChevronDown, ChevronUp, Clock } from 'lucide-react'

const STATUS_LABEL = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  BLOCKED: 'Blocked',
}

const PRIORITY_LABEL = {
  LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', URGENT: 'Urgent',
}

const PRIORITY_COLOR = {
  LOW: 'bg-slate-100 text-slate-600',
  MEDIUM: 'bg-blue-50 text-blue-600',
  HIGH: 'bg-warning-light text-warning',
  URGENT: 'bg-danger-light text-danger',
}

const STATUS_COLOR = {
  NOT_STARTED: 'bg-slate-100 text-slate-600',
  IN_PROGRESS: 'bg-primary-light text-primary',
  COMPLETED: 'bg-success-light text-success',
  BLOCKED: 'bg-danger-light text-danger',
}

export default function TaskCard({ task, canUpdate, onUpdate, onOpenTimeline }) {
  const [editing, setEditing] = useState(false)
  const [status, setStatus] = useState(task.status)
  const [percent, setPercent] = useState(task.progressPercent)
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await onUpdate(task.id, { status, progressPercent: Number(percent), note })
      setEditing(false)
      setNote('')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card padding="p-0" className="flex flex-col overflow-hidden group hover:shadow-md transition-shadow border-slate-200">
      {/* Priority & Due Date Header */}
      <div className="flex justify-between items-center px-5 pt-4 pb-2">
        <span className={`text-xs font-bold px-2.5 py-1 rounded-md uppercase tracking-wider ${PRIORITY_COLOR[task.priority]}`}>
          {PRIORITY_LABEL[task.priority]}
        </span>
        {task.dueDate && (
          <span className="text-xs font-medium text-slate-500 flex items-center gap-1">
            <Calendar size={12} /> Due {task.dueDate}
          </span>
        )}
      </div>

      {/* Title & Description */}
      <div className="px-5 pb-3">
        <h3 className="font-bold text-slate-800 m-0 text-base leading-tight mb-1">{task.title}</h3>
        {task.description && <p className="text-sm text-slate-500 line-clamp-2 m-0">{task.description}</p>}
      </div>

      {/* Meta */}
      <div className="px-5 pb-3 flex items-center gap-3 text-xs text-slate-500 font-medium">
        <span className="flex items-center gap-1"><User size={12} /> {task.assigneeName}</span>
        <span className="text-slate-300">·</span>
        <span className="flex items-center gap-1"><UserCheck size={12} /> {task.assignedByName}</span>
      </div>

      {/* Pulse Bar */}
      <div className="px-5 pb-2">
        <PulseBar percent={task.progressPercent} status={task.status} compact />
      </div>

      {/* Status Row */}
      <div className="px-5 pb-4 flex items-center justify-between">
        <span className={`text-xs font-bold px-2.5 py-1 rounded-md ${STATUS_COLOR[task.status]}`}>
          {STATUS_LABEL[task.status]}
        </span>
        <button 
          className="text-xs font-semibold text-primary hover:text-primary-dark transition-colors flex items-center gap-1" 
          onClick={() => onOpenTimeline(task)}
        >
          <Clock size={12} /> View timeline
        </button>
      </div>

      {/* Update Section */}
      {canUpdate && (
        <div className="border-t border-slate-100">
          {editing ? (
            <form className="p-4 bg-slate-50 space-y-3 animate-in fade-in slide-in-from-top-2 duration-200" onSubmit={submit}>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="form-label text-xs">Status</label>
                  <select className="input-field py-1.5 text-sm" value={status} onChange={(e) => setStatus(e.target.value)}>
                    {Object.entries(STATUS_LABEL).map(([val, label]) => (
                      <option key={val} value={val}>{label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label text-xs">Progress: {percent}%</label>
                  <input 
                    type="range" 
                    min="0" 
                    max="100" 
                    value={percent}
                    onChange={(e) => setPercent(e.target.value)} 
                    className="w-full accent-primary mt-1"
                  />
                </div>
              </div>
              <div>
                <label className="form-label text-xs">Note (optional)</label>
                <input 
                  className="input-field py-1.5 text-sm" 
                  type="text" 
                  value={note} 
                  placeholder="What changed?" 
                  onChange={(e) => setNote(e.target.value)} 
                />
              </div>
              <div className="flex justify-end gap-2 pt-1">
                <button type="button" className="btn btn-ghost py-1.5 text-xs" onClick={() => setEditing(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary py-1.5 text-xs" disabled={saving}>
                  {saving ? 'Saving...' : 'Save update'}
                </button>
              </div>
            </form>
          ) : (
            <button 
              className="w-full p-3 text-sm font-semibold text-primary bg-slate-50/70 hover:bg-primary-light/20 transition-colors flex items-center justify-center gap-2"
              onClick={() => setEditing(true)}
            >
              Log an update <ChevronDown size={16} />
            </button>
          )}
        </div>
      )}
    </Card>
  )
}
