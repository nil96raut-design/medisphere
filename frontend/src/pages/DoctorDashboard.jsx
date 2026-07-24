import React from 'react'
import SidebarLayout from '../components/SidebarLayout'
import DoctorWorkbench from '../components/DoctorWorkbench'

export default function DoctorDashboard() {
  return (
    <SidebarLayout activeTab="DOCTOR" onTabChange={() => {}}>
      <div className="p-6">
        <DoctorWorkbench />
      </div>
    </SidebarLayout>
  )
}