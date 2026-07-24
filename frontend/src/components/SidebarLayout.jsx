import React, { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  LayoutDashboard, Users, CreditCard, FlaskConical, 
  Pill, Bed, Settings, Stethoscope, Monitor, 
  ListOrdered, ClipboardList, LogOut, ChevronLeft, ChevronRight,
  Activity
} from 'lucide-react'

const ROLE_LABEL = { 
  PATIENT: 'Patient', 
  DOCTOR: 'Doctor', 
  RECEPTIONIST: 'Receptionist', 
  ADMIN: 'Hospital Admin', 
  NURSE: 'Nurse', 
  PHARMACIST: 'Pharmacist', 
  LAB_TECH: 'Lab Technician' 
}

// Group definitions
const MENU_GROUPS = {
  ADMIN: [
    { label: 'Overview', items: [
      { key: 'DASHBOARD', label: 'Dashboard', icon: LayoutDashboard }
    ]},
    { label: 'Operations', items: [
      { key: 'PATIENTS', label: 'Patients', icon: Users },
      { key: 'TASKS', label: 'Tasks', icon: ClipboardList },
      { key: 'USERS', label: 'User Management', icon: Settings },
    ]},
    { label: 'Clinical', items: [
      { key: 'IPD', label: 'IPD / Ward', icon: Bed },
      { key: 'LAB', label: 'Lab', icon: FlaskConical },
      { key: 'PHARMACY', label: 'Pharmacy', icon: Pill },
    ]},
    { label: 'Finance', items: [
      { key: 'BILLING', label: 'Billing', icon: CreditCard },
    ]}
  ],
  DOCTOR: [
    { label: 'Clinical', items: [
      { key: 'DOCTOR', label: 'Workbench', icon: Stethoscope },
      { key: 'PATIENTS', label: 'Patients', icon: Users },
      { key: 'IPD', label: 'IPD / Ward', icon: Bed },
    ]},
    { label: 'Personal', items: [
      { key: 'TASKS', label: 'My Tasks', icon: ClipboardList },
    ]}
  ],
  RECEPTIONIST: [
    { label: 'Front Desk', items: [
      { key: 'FRONT_DESK', label: 'Overview', icon: Monitor },
      { key: 'QUEUE', label: 'Queue', icon: ListOrdered },
      { key: 'PATIENTS', label: 'Patients', icon: Users },
    ]},
    { label: 'Operations', items: [
      { key: 'TASKS', label: 'Tasks', icon: ClipboardList },
    ]}
  ],
  NURSE: [
    { label: 'Clinical', items: [
      { key: 'IPD', label: 'IPD / Ward', icon: Bed },
      { key: 'TASKS', label: 'Tasks', icon: ClipboardList },
    ]}
  ],
  PHARMACIST: [
    { label: 'Operations', items: [
      { key: 'PHARMACY', label: 'Pharmacy', icon: Pill },
      { key: 'TASKS', label: 'Tasks', icon: ClipboardList },
    ]}
  ],
  LAB_TECH: [
    { label: 'Operations', items: [
      { key: 'LAB', label: 'Lab', icon: FlaskConical },
      { key: 'TASKS', label: 'Tasks', icon: ClipboardList },
    ]}
  ],
  PATIENT: [
    { label: 'Personal', items: [
      { key: 'PATIENT', label: 'My Care Plan', icon: ClipboardList },
    ]}
  ]
}

export default function SidebarLayout({ activeTab, onTabChange, children }) {
  const { user, logout } = useAuth()
  const [collapsed, setCollapsed] = useState(false)

  const menuGroups = MENU_GROUPS[user?.role] || []

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      <motion.aside
        initial={false}
        animate={{ width: collapsed ? 80 : 260 }}
        className="flex flex-col bg-surface border-r border-slate-200 z-20 shrink-0"
      >
        <div className="h-16 flex items-center justify-between px-6 border-b border-slate-200 shrink-0">
          <AnimatePresence mode="popLayout">
            {!collapsed ? (
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                className="flex items-center gap-2 overflow-hidden whitespace-nowrap"
              >
                <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                  <Activity className="text-primary" size={20} />
                </div>
                <span className="font-display font-bold text-slate-800 text-xl tracking-tight">MediSphere</span>
              </motion.div>
            ) : (
              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                className="w-full flex justify-center"
              >
                <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                  <Activity className="text-primary" size={20} />
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <button 
          onClick={() => setCollapsed(!collapsed)}
          className="absolute left-[calc(100%-12px)] top-20 w-6 h-6 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-400 hover:text-primary hover:border-primary shadow-sm z-30 transition-colors hidden md:flex"
        >
          {collapsed ? <ChevronRight size={14} /> : <ChevronLeft size={14} />}
        </button>

        <div className="flex-1 overflow-y-auto py-6 no-scrollbar flex flex-col gap-6">
          {menuGroups.map((group, groupIdx) => (
            <div key={groupIdx} className="px-3">
              {!collapsed && (
                <div className="px-3 mb-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                  {group.label}
                </div>
              )}
              <div className="flex flex-col gap-1">
                {group.items.map(item => {
                  const Icon = item.icon
                  const isActive = activeTab === item.key
                  
                  return (
                    <button
                      key={item.key}
                      onClick={() => onTabChange(item.key)}
                      title={collapsed ? item.label : ''}
                      className={`flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group relative ${
                        isActive 
                          ? 'bg-primary/10 text-primary font-medium' 
                          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                      }`}
                    >
                      {isActive && (
                        <motion.div 
                          layoutId="activeIndicator"
                          className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r-full"
                        />
                      )}
                      <div className="flex items-center justify-center w-6 h-6">
                        <Icon size={20} className={isActive ? 'text-primary' : 'text-slate-400 group-hover:text-slate-600'} />
                      </div>
                      <AnimatePresence mode="popLayout">
                        {!collapsed && (
                          <motion.span
                            initial={{ opacity: 0, x: -10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -10 }}
                            className="whitespace-nowrap"
                          >
                            {item.label}
                          </motion.span>
                        )}
                      </AnimatePresence>
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
        </div>

        <div className="p-4 border-t border-slate-200 bg-slate-50/50 shrink-0">
          <div className={`flex items-center gap-3 ${collapsed ? 'justify-center' : ''}`}>
            <div className="w-10 h-10 rounded-full bg-primary text-white flex items-center justify-center font-bold text-sm shrink-0">
              {user?.fullName?.charAt(0).toUpperCase() || 'U'}
            </div>
            {!collapsed && (
              <div className="flex-1 min-w-0 flex flex-col">
                <span className="text-sm font-semibold text-slate-800 truncate">{user?.fullName}</span>
                <span className="text-xs text-slate-500 truncate">{ROLE_LABEL[user?.role] || user?.role}</span>
                {user?.hospitalId && (
                  <span className="text-[10px] font-medium text-primary uppercase mt-0.5">Hospital #{user.hospitalId}</span>
                )}
              </div>
            )}
          </div>
          
          <button 
            onClick={logout}
            title={collapsed ? "Sign out" : ""}
            className={`mt-4 w-full flex items-center gap-2 text-danger hover:bg-danger-light px-3 py-2 rounded-xl transition-colors ${
              collapsed ? 'justify-center' : ''
            }`}
          >
            <LogOut size={18} />
            {!collapsed && <span className="text-sm font-medium">Sign out</span>}
          </button>
        </div>
      </motion.aside>

      <main className="flex-1 overflow-y-auto bg-background p-4 md:p-8">
        <div className="max-w-[1400px] mx-auto">
          {children}
        </div>
      </main>
    </div>
  )
}
