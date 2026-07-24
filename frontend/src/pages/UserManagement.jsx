import React, { useState, useEffect, useCallback } from 'react'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import SidebarLayout from '../components/SidebarLayout'
import { Card } from '../components/Card'
import { Modal } from '../components/Modal'
import { useToast } from '../context/ToastContext'
import { UserPlus, Users, Search, Mail, Shield, Edit3, Trash2, Stethoscope } from 'lucide-react'

const ROLE_OPTIONS = [
  { value: 'DOCTOR', label: 'Doctor' },
  { value: 'NURSE', label: 'Nurse' },
  { value: 'RECEPTIONIST', label: 'Care Team (Staff)' },
  { value: 'PHARMACIST', label: 'Pharmacist' },
  { value: 'LAB_TECH', label: 'Lab Technician' },
]

const ROLE_COLOR = {
  ADMIN: 'bg-violet-50 text-violet-600',
  DOCTOR: 'bg-teal-50 text-teal-600',
  NURSE: 'bg-pink-50 text-pink-600',
  RECEPTIONIST: 'bg-blue-50 text-blue-600',
  PHARMACIST: 'bg-amber-50 text-amber-600',
  LAB_TECH: 'bg-emerald-50 text-emerald-600',
  PATIENT: 'bg-sky-50 text-sky-600',
}

export default function UserManagement() {
  const { user } = useAuth()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [filterRole, setFilterRole] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const toast = useToast()

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'DOCTOR',
    primaryDoctorId: '',
  })

  const [doctors, setDoctors] = useState([])
  const [submitting, setSubmitting] = useState(false)

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const allRoles = ['DOCTOR', 'NURSE', 'RECEPTIONIST', 'PHARMACIST', 'LAB_TECH', 'PATIENT']
      const results = await Promise.all(allRoles.map(role => api.usersByRole(role).catch(() => [])))
      setUsers(results.flat())
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => { fetchUsers() }, [fetchUsers])

  useEffect(() => {
    api.usersByRole('DOCTOR').then(setDoctors).catch(() => {})
  }, [])

  const openCreateModal = () => {
    setEditingUser(null)
    setForm({ fullName: '', email: '', password: '', role: 'DOCTOR', primaryDoctorId: '' })
    setShowModal(true)
  }

  const openEditModal = (u) => {
    setEditingUser(u)
    setForm({ fullName: u.fullName, email: u.email, password: '', role: u.role, primaryDoctorId: u.primaryDoctorId || '' })
    setShowModal(true)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      if (editingUser) {
        toast.info('User updated (UI only - backend update endpoint needed)')
      } else {
        await api.createUser({
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          role: form.role,
        })
        toast.success('User created successfully')
        fetchUsers()
      }
      setShowModal(false)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (userId) => {
    if (!window.confirm('Are you sure you want to delete this user?')) return
    toast.info('Delete endpoint not implemented yet')
  }

  const filteredUsers = users
    .filter(u => u.id !== user.id)
    .filter(u => filterRole === 'ALL' || u.role === filterRole)
    .filter(u => {
      if (!searchQuery.trim()) return true
      const q = searchQuery.toLowerCase()
      return u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    })

  if (loading) {
    return (
      <SidebarLayout activeTab="USERS" onTabChange={() => {}}>
        <div className="flex items-center justify-center h-96">
          <div className="text-center">
            <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
            <p className="text-sm text-slate-500 font-medium">Loading users…</p>
          </div>
        </div>
      </SidebarLayout>
    )
  }

  return (
    <SidebarLayout activeTab="USERS" onTabChange={() => {}}>
      <div className="space-y-6 animate-in fade-in duration-500 pb-12">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-display font-bold text-slate-800 tracking-tight">User Management</h1>
            <p className="text-slate-500 mt-1">{users.length} users across your organization.</p>
          </div>
          <button className="btn btn-primary bg-primary text-white" onClick={openCreateModal}>
            <UserPlus size={18} /> Add User
          </button>
        </div>

        {/* Filters */}
        <div className="flex flex-col md:flex-row gap-3 items-stretch md:items-center">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
            <input
              type="search"
              className="input-field pl-10 py-2.5"
              placeholder="Search by name or email..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <div className="flex flex-wrap gap-2 bg-slate-50 p-1.5 rounded-xl border border-slate-200">
            {[{ key: 'ALL', label: 'All' }, ...ROLE_OPTIONS, { value: 'PATIENT', label: 'Patient' }].map((opt) => {
              const key = opt.key || opt.value
              const isActive = filterRole === key
              return (
                <button
                  key={key}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                    isActive
                      ? 'bg-white text-primary shadow-sm border border-slate-200'
                      : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100 border border-transparent'
                  }`}
                  onClick={() => setFilterRole(key)}
                >
                  {opt.label}
                </button>
              )
            })}
          </div>
        </div>

        {/* User Cards Grid */}
        {filteredUsers.length === 0 ? (
          <div className="text-center py-16 bg-surface rounded-2xl border border-slate-200 border-dashed">
            <Users size={48} className="mx-auto text-slate-200 mb-4" />
            <h3 className="text-lg font-bold text-slate-800 mb-2">No users found</h3>
            <p className="text-slate-500">{searchQuery ? 'Try a different search term.' : 'No users match the selected filter.'}</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredUsers.map((u) => (
              <Card key={u.id} padding="p-0" className="flex flex-col overflow-hidden group hover:border-primary/30 transition-colors">
                <div className="p-5 flex-1">
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-primary text-white flex items-center justify-center font-bold text-lg shadow-sm shrink-0">
                      {u.fullName?.charAt(0) || '?'}
                    </div>
                    <div className="min-w-0 flex-1">
                      <h3 className="font-bold text-slate-800 m-0 text-base truncate">{u.fullName}</h3>
                      <p className="text-xs text-slate-500 m-0 mt-0.5 truncate flex items-center gap-1">
                        <Mail size={12} /> {u.email}
                      </p>
                    </div>
                  </div>
                  
                  <div className="mt-4 flex items-center gap-2">
                    <span className={`text-xs font-bold px-2.5 py-1 rounded-md ${ROLE_COLOR[u.role] || 'bg-slate-100 text-slate-600'}`}>
                      {u.role}
                    </span>
                    {u.primaryDoctorId && (
                      <span className="text-xs text-slate-500 flex items-center gap-1">
                        <Stethoscope size={12} /> Dr. ID: {u.primaryDoctorId}
                      </span>
                    )}
                  </div>
                </div>
                
                <div className="bg-slate-50 border-t border-slate-100 px-5 py-3 flex justify-end gap-2 opacity-70 group-hover:opacity-100 transition-opacity">
                  <button 
                    className="p-2 text-slate-400 hover:text-primary hover:bg-primary-light/30 rounded-lg transition-colors"
                    onClick={() => openEditModal(u)}
                    title="Edit"
                  >
                    <Edit3 size={16} />
                  </button>
                  <button 
                    className="p-2 text-slate-400 hover:text-danger hover:bg-danger-light rounded-lg transition-colors"
                    onClick={() => handleDelete(u.id)}
                    title="Delete"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Create / Edit Modal */}
      {showModal && (
        <Modal isOpen={true} onClose={() => setShowModal(false)} title={editingUser ? 'Edit User' : 'Create New User'}>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="form-label">Full Name *</label>
                <input className="input-field" value={form.fullName} onChange={e => setForm({...form, fullName: e.target.value})} required />
              </div>
              <div>
                <label className="form-label">Email *</label>
                <input className="input-field" type="email" value={form.email} onChange={e => setForm({...form, email: e.target.value})} required />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="form-label">{editingUser ? 'New Password' : 'Password *'}</label>
                <input className="input-field" type="password" value={form.password} onChange={e => setForm({...form, password: e.target.value})} required={!editingUser} placeholder={editingUser ? 'Leave blank to keep' : ''} />
              </div>
              <div>
                <label className="form-label">Role *</label>
                <select className="input-field" value={form.role} onChange={e => setForm({...form, role: e.target.value})}>
                  {ROLE_OPTIONS.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                </select>
              </div>
            </div>
            <div>
              <label className="form-label">Primary Doctor (for patients)</label>
              <select className="input-field" value={form.primaryDoctorId} onChange={e => setForm({...form, primaryDoctorId: e.target.value})}>
                <option value="">None</option>
                {doctors.map(d => <option key={d.id} value={d.id}>{d.fullName}</option>)}
              </select>
            </div>
            <div className="pt-4 flex justify-end gap-3 border-t border-slate-100 mt-6">
              <button type="button" className="btn btn-ghost" onClick={() => setShowModal(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving...' : (editingUser ? 'Update User' : 'Create User')}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </SidebarLayout>
  )
}