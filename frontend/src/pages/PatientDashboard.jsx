import React, { useState, useEffect } from 'react'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import SidebarLayout from '../components/SidebarLayout'
import { Card } from '../components/Card'
import TaskCard from '../components/TaskCard'
import TimelineDrawer from '../components/TimelineDrawer'
import { ClipboardList, Heart, ListChecks } from 'lucide-react'

export default function PatientDashboard() {
  const { user } = useAuth()
  const [tasks, setTasks] = useState([])
  const [activeTask, setActiveTask] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.listTasks().then(setTasks).catch((e) => setError(e.message)).finally(() => setLoading(false))
  }, [])

  const handleUpdate = async (taskId, payload) => {
    await api.updateProgress(taskId, payload)
    const refreshed = await api.listTasks()
    setTasks(refreshed)
  }

  if (loading) {
    return (
      <SidebarLayout activeTab="PATIENT" onTabChange={() => {}}>
        <div className="flex items-center justify-center h-96">
          <div className="text-center">
            <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
            <p className="text-sm text-slate-500 font-medium">Loading your care plan…</p>
          </div>
        </div>
      </SidebarLayout>
    )
  }

  return (
    <SidebarLayout activeTab="PATIENT" onTabChange={() => {}}>
      <div className="space-y-8 animate-in fade-in duration-500 pb-12">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-display font-bold text-slate-800 tracking-tight flex items-center gap-3">
              <Heart className="text-primary" size={28} /> Your Care Plan
            </h1>
            <p className="text-slate-500 mt-1">Everything assigned to you, in one place. Track progress on each task.</p>
          </div>
          <div className="flex items-center gap-3 bg-slate-50 px-4 py-2 rounded-xl border border-slate-200">
            <ListChecks size={18} className="text-primary" />
            <span className="text-sm font-semibold text-slate-600">
              {tasks.filter(t => t.status === 'COMPLETED').length} / {tasks.length} completed
            </span>
          </div>
        </div>

        {error && (
          <div className="p-4 bg-danger-light border border-danger/30 rounded-xl text-sm text-danger font-medium">
            {error}
          </div>
        )}

        {tasks.length === 0 ? (
          <Card className="text-center py-20">
            <div className="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
              <ClipboardList size={40} className="text-slate-300" />
            </div>
            <h3 className="text-xl font-bold text-slate-800 mb-2">No tasks yet</h3>
            <p className="text-slate-500 max-w-sm mx-auto">Your care team will add tasks here when needed. Check back later for updates.</p>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {tasks.map((task) => (
              <TaskCard key={task.id} task={task} canUpdate={true} onUpdate={handleUpdate} onOpenTimeline={setActiveTask} />
            ))}
          </div>
        )}

        <TimelineDrawer task={activeTask} onClose={() => setActiveTask(null)} />
      </div>
    </SidebarLayout>
  )
}