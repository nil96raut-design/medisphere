import React, { useState, useEffect, useMemo } from 'react'
import { api } from '../api/client'
import TaskCard from '../components/TaskCard'
import NewTaskForm from '../components/NewTaskForm'
import TimelineDrawer from '../components/TimelineDrawer'
import PulseBar from '../components/PulseBar'
import SidebarLayout from '../components/SidebarLayout'
import { Card } from '../components/Card'
import { useAuth } from '../context/AuthContext'
import { 
  Users, Calendar, DollarSign, Bed, 
  UserPlus, CalendarPlus, Activity, TrendingUp
} from 'lucide-react'

const COPY = {
  ADMIN: { greeting: 'Hospital Overview', sub: 'Hospital-wide activity and key metrics.' },
  DOCTOR: { greeting: "Today's Caseload", sub: 'Tasks assigned across your patients.' },
  RECEPTIONIST: { greeting: 'Care Coordination', sub: 'Tasks across the care team.' },
  NURSE: { greeting: 'Ward Activity', sub: 'Inpatient tasks and nursing logs.' },
  PHARMACIST: { greeting: 'Pharmacy Queue', sub: 'Prescriptions and dispensing.' },
  LAB_TECH: { greeting: 'Lab Queue', sub: 'Orders and results processing.' },
  PATIENT: { greeting: 'My Care Plan', sub: 'Everything assigned to you, in one place.' },
}

export default function Dashboard() {
  const { user, isAdmin, isDoctor, isReceptionist } = useAuth()
  const [tasks, setTasks] = useState([])
  const [pageTasks, setPageTasks] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState('ALL')
  const [activeTask, setActiveTask] = useState(null)
  const [activeTab, setActiveTab] = useState(isAdmin ? 'DASHBOARD' : 'TASKS')
  const [analytics, setAnalytics] = useState(null)

  const canAssign = isAdmin || isDoctor || isReceptionist
  const copy = COPY[user?.role] || { greeting: 'Dashboard', sub: '' }

  const refresh = () => {
    api.listTasks().then(setTasks).catch((e) => setError(e.message))
  }

  const refreshPage = (pageToLoad = page, q = query) => {
    setLoading(true)
    api.searchTasks({ q, page: pageToLoad, size: 12 })
      .then((result) => {
        setPageTasks(result.content)
        setTotalPages(result.totalPages)
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    refresh()
    if (isAdmin) {
      api.getAnalytics().then(setAnalytics).catch(() => {})
    }
  }, [])
  useEffect(() => { refreshPage(page, query) }, [page, query])

  const handleQueryChange = (q) => {
    setQuery(q)
    setPage(0)
  }

  const filtered = useMemo(
    () => filter === 'ALL' ? pageTasks : pageTasks.filter((t) => t.status === filter),
    [pageTasks, filter]
  )

  const overallPercent = useMemo(() => {
    if (tasks.length === 0) return 0
    const sum = tasks.reduce((acc, t) => acc + t.progressPercent, 0)
    return Math.round(sum / tasks.length)
  }, [tasks])

  const counts = useMemo(() => {
    const c = { NOT_STARTED: 0, IN_PROGRESS: 0, COMPLETED: 0, BLOCKED: 0 }
    tasks.forEach((t) => { c[t.status] = (c[t.status] || 0) + 1 })
    return c
  }, [tasks])

  const handleCreate = async (payload) => {
    await api.createTask(payload)
    refresh()
    refreshPage()
  }

  const handleUpdate = async (taskId, payload) => {
    await api.updateProgress(taskId, payload)
    refresh()
    refreshPage()
  }

  const FILTERS = ['ALL', 'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED']
  const FILTER_LABEL = {
    ALL: 'All', NOT_STARTED: 'Not started', IN_PROGRESS: 'In progress',
    COMPLETED: 'Completed', BLOCKED: 'Blocked',
  }

  return (
    <SidebarLayout activeTab={activeTab} onTabChange={setActiveTab}>
      <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-display font-bold text-slate-800 tracking-tight">{copy.greeting}</h1>
            <p className="text-slate-500 mt-1">{copy.sub}</p>
          </div>
          
          {activeTab === 'TASKS' && (
            <Card padding="p-4" className="min-w-[280px]">
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Overall Progress</span>
                <span className="text-sm font-bold text-primary">{overallPercent}%</span>
              </div>
              <PulseBar percent={overallPercent} status={overallPercent === 100 ? 'COMPLETED' : 'IN_PROGRESS'} />
              <div className="flex flex-wrap gap-x-4 gap-y-1 mt-3 text-xs text-slate-500">
                <span><strong className="text-slate-800">{counts.NOT_STARTED}</strong> pending</span>
                <span><strong className="text-slate-800">{counts.IN_PROGRESS}</strong> active</span>
                <span><strong className="text-slate-800">{counts.COMPLETED}</strong> done</span>
              </div>
            </Card>
          )}
        </div>

        {activeTab === 'DASHBOARD' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <Card padding="p-5" className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center shrink-0">
                  <Users size={24} />
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-500">Total Patients</p>
                  <p className="text-2xl font-bold text-slate-800">{analytics?.totalPatients ?? '—'}</p>
                </div>
              </Card>
              <Card padding="p-5" className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-full bg-teal-50 text-teal-500 flex items-center justify-center shrink-0">
                  <Calendar size={24} />
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-500">Today's Appts</p>
                  <p className="text-2xl font-bold text-slate-800">{analytics?.todayAppointments ?? '—'}</p>
                </div>
              </Card>
              <Card padding="p-5" className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-full bg-emerald-50 text-emerald-500 flex items-center justify-center shrink-0">
                  <DollarSign size={24} />
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-500">Revenue (Today)</p>
                  <p className="text-2xl font-bold text-slate-800">${Number(analytics?.todayRevenue || 0).toLocaleString()}</p>
                </div>
              </Card>
              <Card padding="p-5" className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-full bg-purple-50 text-purple-500 flex items-center justify-center shrink-0">
                  <Bed size={24} />
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-500">Active Beds</p>
                  <p className="text-2xl font-bold text-slate-800">{analytics?.activeBeds ?? '—'} / {analytics?.totalBeds ?? '—'}</p>
                </div>
              </Card>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <Card className="col-span-1 lg:col-span-2 flex flex-col">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2 m-0">
                    <Activity size={20} className="text-primary" />
                    Recent Activity
                  </h3>
                  <button className="text-sm text-primary font-medium hover:underline">View all</button>
                </div>
                <div className="flex-1 flex flex-col gap-4">
                  {[1, 2, 3].map((i) => (
                    <div key={i} className="flex items-start gap-4 p-3 rounded-xl hover:bg-slate-50 transition-colors">
                      <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center shrink-0 text-slate-400">
                        <TrendingUp size={18} />
                      </div>
                      <div className="flex-1">
                        <p className="text-sm font-medium text-slate-800">New patient admitted to Ward A</p>
                        <p className="text-xs text-slate-500 mt-0.5">2 hours ago</p>
                      </div>
                    </div>
                  ))}
                  {tasks.slice(0,2).map(t => (
                    <div key={t.id} className="flex items-start gap-4 p-3 rounded-xl hover:bg-slate-50 transition-colors">
                      <div className="w-10 h-10 rounded-full bg-primary-light text-primary flex items-center justify-center shrink-0">
                        <Calendar size={18} />
                      </div>
                      <div className="flex-1">
                        <p className="text-sm font-medium text-slate-800">Task completed: {t.title}</p>
                        <p className="text-xs text-slate-500 mt-0.5">Today</p>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>

              <Card className="col-span-1 bg-gradient-to-br from-primary to-primary-dark text-white border-none shadow-lg">
                <h3 className="text-lg font-bold text-white mb-6 m-0">Quick Actions</h3>
                <div className="flex flex-col gap-3">
                  <button className="flex items-center gap-3 w-full p-4 rounded-xl bg-white/10 hover:bg-white/20 transition-colors text-left group">
                    <UserPlus size={20} className="text-white/80 group-hover:text-white" />
                    <span className="font-medium text-white">Add New Patient</span>
                  </button>
                  <button className="flex items-center gap-3 w-full p-4 rounded-xl bg-white/10 hover:bg-white/20 transition-colors text-left group">
                    <CalendarPlus size={20} className="text-white/80 group-hover:text-white" />
                    <span className="font-medium text-white">Book Appointment</span>
                  </button>
                </div>
              </Card>
            </div>
          </div>
        )}

        {activeTab === 'TASKS' && (
          <div className="space-y-6">
            {canAssign && (
              <div className="mb-8">
                <NewTaskForm onCreated={handleCreate} />
              </div>
            )}
            
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-surface p-4 rounded-xl border border-slate-200 shadow-sm">
              <div className="flex flex-wrap gap-2">
                {FILTERS.map((f) => (
                  <button
                    key={f}
                    className={`px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${
                      filter === f
                        ? 'bg-slate-800 text-white shadow-sm'
                        : 'bg-transparent text-slate-600 hover:bg-slate-100'
                    }`}
                    onClick={() => setFilter(f)}
                  >
                    {FILTER_LABEL[f]}
                  </button>
                ))}
              </div>
              <div className="relative w-full sm:w-auto sm:min-w-[300px]">
                <input
                  type="search"
                  className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary focus:bg-surface transition-all"
                  placeholder="Search tasks…"
                  value={query}
                  onChange={(e) => handleQueryChange(e.target.value)}
                />
              </div>
            </div>

            {loading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {[1,2,3,4,5,6].map(i => (
                  <div key={i} className="h-48 bg-slate-100 animate-pulse rounded-xl" />
                ))}
              </div>
            ) : filtered.length === 0 ? (
              <div className="text-center py-20 px-4">
                <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <ClipboardList size={32} className="text-slate-400" />
                </div>
                <h3 className="text-lg font-bold text-slate-800 mb-2">No tasks found</h3>
                <p className="text-slate-500 max-w-md mx-auto">
                  {canAssign 
                    ? "You haven't assigned any tasks matching these filters yet. Create one above to get started." 
                    : "You don't have any tasks matching these filters."}
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                {filtered.map((task) => (
                  <TaskCard key={task.id} task={task} canUpdate={true} onUpdate={handleUpdate} onOpenTimeline={setActiveTask} />
                ))}
              </div>
            )}

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 mt-8 pb-4">
                <button
                  className="px-4 py-2 text-sm font-medium border border-slate-200 rounded-lg disabled:opacity-50 hover:bg-slate-50 transition-colors disabled:hover:bg-transparent"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </button>
                <div className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-50 rounded-lg">
                  Page {page + 1} of {totalPages}
                </div>
                <button
                  className="px-4 py-2 text-sm font-medium border border-slate-200 rounded-lg disabled:opacity-50 hover:bg-slate-50 transition-colors disabled:hover:bg-transparent"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </button>
              </div>
            )}
          </div>
        )}
      </div>
      <TimelineDrawer task={activeTask} onClose={() => setActiveTask(null)} />
    </SidebarLayout>
  )
}