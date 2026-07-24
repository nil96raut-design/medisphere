import React, { useEffect, useState } from 'react'
import { api } from '../api/client'
import { Modal } from './Modal'
import { Clock, User, MessageSquare } from 'lucide-react'

const STATUS_COLOR = {
  NOT_STARTED: 'bg-slate-100 text-slate-600',
  IN_PROGRESS: 'bg-primary-light text-primary',
  COMPLETED: 'bg-success-light text-success',
  BLOCKED: 'bg-danger-light text-danger',
}

export default function TimelineDrawer({ task, onClose }) {
  const [notes, setNotes] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!task) return
    setLoading(true)
    api.getTimeline(task.id)
      .then(setNotes)
      .finally(() => setLoading(false))
  }, [task])

  if (!task) return null

  return (
    <Modal isOpen={true} onClose={onClose} title={task.title}>
      <div className="space-y-4">
        <p className="text-sm text-slate-500 m-0">Progress timeline for <strong className="text-slate-800">{task.assigneeName}</strong></p>

        {loading && (
          <div className="flex items-center justify-center py-12">
            <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
          </div>
        )}

        {!loading && notes.length === 0 && (
          <div className="py-12 text-center text-slate-400 bg-slate-50 rounded-xl border border-slate-200 border-dashed">
            <Clock size={32} className="mx-auto text-slate-300 mb-3" />
            <p className="text-sm font-medium m-0">No updates logged yet.</p>
          </div>
        )}

        {!loading && notes.length > 0 && (
          <div className="relative pl-6 border-l-2 border-slate-200 space-y-6">
            {notes.map((n) => (
              <div key={n.id} className="relative">
                {/* Timeline dot */}
                <div className="absolute -left-[calc(1.5rem+5px)] w-3 h-3 rounded-full bg-primary border-2 border-white shadow-sm"></div>
                
                <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 hover:border-slate-300 transition-colors">
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-semibold text-slate-800 text-sm flex items-center gap-1.5">
                      <User size={14} className="text-slate-400" />
                      {n.author?.fullName || 'Unknown'}
                    </span>
                    <span className="text-xs text-slate-500 font-medium">
                      {new Date(n.createdAt).toLocaleString()}
                    </span>
                  </div>
                  
                  <div className="flex items-center gap-3 mb-2">
                    <span className={`text-xs font-bold px-2 py-0.5 rounded-md ${STATUS_COLOR[n.status] || 'bg-slate-100 text-slate-600'}`}>
                      {n.status.replaceAll('_', ' ')}
                    </span>
                    <span className="text-xs text-slate-500 font-mono">{n.progressPercent}%</span>
                  </div>
                  
                  {n.note && (
                    <p className="text-sm text-slate-600 m-0 flex items-start gap-2 mt-2 pt-2 border-t border-slate-100">
                      <MessageSquare size={14} className="text-slate-400 mt-0.5 shrink-0" />
                      {n.note}
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Modal>
  )
}
