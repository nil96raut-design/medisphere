# Implementation Plan — MediSphere Demo-Ready System

This plan outlines the changes to prepare the Hospital Management System (HMS / MediSphere) for a professional, client-impressive demo.

---

## Proposed Changes

### 1. Backend: Data Seeding (`DataSeeder.java`)

We will update [DataSeeder.java](file:///d:/Project/healthtrack/backend/src/main/java/com/healthtrack/config/DataSeeder.java) to automatically seed a complete, rich, and realistic set of data for the demo:
* **1 Hospital:** "MediSphere Premium Hospital" (with active subscription status).
* **3 Core Users:**
  - Hospital Admin: `admin@medisphere.com` (Priya Sharma)
  - Lead Doctor: `doctor@medisphere.com` (Dr. Asha Mehta)
  - Front Desk / Receptionist: `receptionist@medisphere.com` (Rohan Patil)
  - Default password for all: `password123`
* **10 Patients:** Seeded with realistic names, DOBs, phone numbers, genders, and mock insurance details (e.g., Alice Vance, David Miller, Sarah Jenkins, Michael Chen, Emily Davis, Marcus Thompson, Robert Taylor, Linda Garcia, William Martinez, Jessica Robinson).
* **5 Appointments:**
  - A mix of scheduled/completed statuses, mapped to different patients and our lead doctor, scheduled for today and upcoming slots.
* **2 Admissions:**
  - Create General Ward and ICU bed records.
  - Admit 2 patients to these beds (one active, one discharged) to demonstrate both bed utilization states.
* **3 Lab Reports (LabTestOrders):**
  - Create lab orders for different patients (e.g., Complete Blood Count, Lipid Panel, Thyroid Panel) in statuses: `ORDERED`, `SAMPLE_COLLECTED`, and `RESULT_READY`.
* **1 Completed Bill:**
  - Create a historical completed/paid bill (settled via CASH or CARD) with associated Line Items to show how billing look-ups work.
* **Pharmacy Stock:**
  - Seed baseline medicine stock (e.g., Paracetamol, Amoxicillin, Ibuprofen, Metformin) so that the pharmacist dashboard and dispensing works immediately without starting from zero.

### 2. Frontend: UI & UX Audit & Polish

We will inspect the core components and pages of the Vite frontend to ensure:
* **Loading Spinners:** Present on all data-fetching dashboards.
* **Toast Notifications:** Properly triggered for all primary actions (e.g. registration success, appointment booked, lab reports generated, dispensation completed, billing settlement).
* **Empty States:** Graceful fallbacks for empty searches, empty queues, or empty lists.
* **Disabled Button States:** Disabled state when operations are in-flight (to prevent double-clicks) or when inputs are invalid.
* **Smooth Demo Flow:** Check and ensure seamless progression from Patient Registration → Appointment Booking → Triage logging → Doctor Consultation/Prescription/Service requests → Lab testing → Ward Admission & Nursing log → Pharmacy Dispensing → Billing Settlement.

---

## Verification Plan

### Automated Verification
- Run `node test-runner.js` to ensure the security, concurrency, multi-tenancy, and lifecycle flows are still 100% correct with the updated data seed setup.

### Manual Verification
- Log in to the Vite frontend as each role (Receptionist, Doctor, Nurse, Lab Tech, Pharmacist, Admin) and verify that all dashboard screens display rich, populated data and operate cleanly.
