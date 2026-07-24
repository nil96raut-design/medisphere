import React from 'react'
import SidebarLayout from '../components/SidebarLayout'
import WardVisualizer from '../components/WardVisualizer'

export default function IPDDashboard() {
  return (
    <SidebarLayout activeTab="IPD" onTabChange={() => {}}>
      <div className="p-6">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-display font-bold text-slate-800 tracking-tight">IPD & Ward Management</h1>
            <p className="text-slate-500 mt-1">Monitor bed occupancy, admit patients, and record nursing logs.</p>
          </div>
        </div>
        <WardVisualizer />
      </div>
    </SidebarLayout>
  )
}