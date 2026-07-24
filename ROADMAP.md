# HealthTrack Roadmap

This roadmap maps the current state of the HealthTrack codebase to a comprehensive hospital-workflow vision. 

## 1. Patient registration/intake
* **What exists today:** Basic user registration exists via `AuthController.java` and `User.java`. The `User` entity captures `fullName`, `email`, `role`, and `allergies` (added in `V2__add_allergies.sql`), but lacks extensive demographic or intake data.
* **First increment:** Introduce a `PatientProfile` entity linked one-to-one with `User`, containing demographics (DOB, address, emergency contact). A new migration `V3__add_patient_profile.sql` would create the table, managed by a `PatientProfileService` with role-checks ensuring patients can update their own data and staff/admins can edit it.
* **Complexity flag:** Collecting detailed demographics and government IDs requires strict adherence to PHI handling regulations (HIPAA in the US, DISHA/ABDM in India), necessitating design review for data encryption at rest and audit logging.
* **Out of scope for now:** Ambulance/emergency dispatch modules are real HMS features but are not roadmapped in this pass.

## 2. Triage/queueing
* **What exists today:** The `Task.java` entity supports priority (`TaskPriority.java`) and status (`TaskStatus.java`), which can be used to loosely prioritize work, but there is no concept of a live patient queue or physical location.
* **First increment:** Create an `Encounter` or `Visit` entity to represent a specific hospital visit, with a `TriagePriority` and `status` (e.g., WAITING, IN_CONSULTATION). The `VisitService` would handle state transitions, restricted to `STAFF` and `DOCTOR` roles.
* **Complexity flag:** Triage workflows must account for clinical urgency protocols. Algorithms or automated sorting rules require clinical safety validation before implementation.
* **Out of scope for now:** Emergency/trauma prioritization is outside the scope of this pass.

## 3. Doctor consultation (closest to current MVP)
* **What exists today:** This maps closely to the existing `Task` and `ProgressNote` entities. Doctors can assign tasks to patients and monitor progress (`TaskService.updateProgress`), while `ProgressNote` acts as a timeline of updates. `User.java` tracks `primaryDoctorId`.
* **First increment:** Add a `ConsultationNote` entity (or expand `ProgressNote`) dedicated to structured clinical observations (SOAP format: Subjective, Objective, Assessment, Plan). Handled via `ConsultationService` accessible only to the assigned `DOCTOR`.
* **Complexity flag:** Clinical documentation must support versioning and immutable audit trails for medical-legal compliance.
* **Out of scope for now:** E-prescribing integrations are not currently planned.

## 4. Diagnostics/labs
* **What exists today:** Does not exist. The `allergies` field in `User.java` is the only clinically relevant safety data currently stored. 
* **First increment:** Introduce a `LabOrder` entity and a related `LabResult` entity. Modeled like `Task`, a `LabOrder` would be created by a `DOCTOR`, fulfilled by `STAFF` (lab technicians), with `LabOrderService` enforcing role access.
* **Complexity flag:** Laboratory results are highly sensitive PHI. Any transmission of this data implicates HIPAA/ABDM and requires dedicated security review.
* **Out of scope for now:** Blood bank management is a real HMS module but is not being roadmapped in this pass.

## 5. Pharmacy
* **What exists today:** Does not exist.
* **First increment:** Create a `Prescription` entity linked to a `User` (patient) and the prescribing `User` (doctor). A `PrescriptionService` would validate that only `DOCTOR` roles can create records, while `STAFF` (pharmacists) can update status to "DISPENSED".
* **Complexity flag:** Pharmacy modules require interaction with external drug databases for interaction checks and compliance with controlled substance regulations.
* **Out of scope for now:** Inpatient ward medication dispensing carts are not covered in this pass.

## 6. Admission/bed management
* **What exists today:** Does not exist. The system assumes outpatient interaction via `Task`.
* **First increment:** A `Bed` entity and an `Admission` entity tracking which patient is assigned to which bed. `AdmissionService` would manage check-in/check-out, restricted to `STAFF` and `ADMIN`.
* **Complexity flag:** Bed management impacts hospital capacity reporting and infectious disease isolation protocols, requiring careful operational design.
* **Out of scope for now:** OT/surgery scheduling is a real HMS module but is not being roadmapped in this pass.

## 7. Discharge & referral handoff
* **What exists today:** Tasks can be marked as `COMPLETED` (`TaskStatus.java`), but there is no formalized discharge process or external handoff.
* **First increment:** Create a `DischargeSummary` entity that aggregates tasks, progress notes, and future care plans. Managed by `DischargeService` with doctor sign-off workflows.
* **Complexity flag:** Referrals and external handoffs heavily implicate interoperability standards (HL7/FHIR) if data ever leaves the system. This requires dedicated design and legal review.
* **Out of scope for now:** Direct electronic referrals to out-of-network providers are not roadmapped.

## 8. Billing (incl. insurance/TPA claims)
* **What exists today:** Does not exist.
* **First increment:** Introduce an `Invoice` entity linked to a `User` and a specific visit/encounter. `BillingService` would calculate totals based on standardized charge codes, accessible to `ADMIN` and billing `STAFF`.
* **Complexity flag:** Billing and insurance claims implicate payment-processing standards (PCI-DSS) and TPA integration standards. This is not just another CRUD module; it requires strict financial and legal auditing.
* **Out of scope for now:** Integration with physical point-of-sale terminals is excluded.

## 9. Admin analytics dashboard
* **What exists today:** The `/api/admin/**` endpoints are restricted to `ADMIN` via `SecurityConfig.java`. `Dashboard.jsx` shows basic counts (Not Started, In Progress, etc.) based on all tasks.
* **First increment:** Create specialized aggregated queries in `TaskRepository.java` (e.g., average completion time). An `AnalyticsController` and `AnalyticsService` would serve these metrics, protected by `hasRole("ADMIN")`.
* **Complexity flag:** Analytics over PHI require data de-identification strategies to ensure compliance with privacy regulations during reporting.
* **Out of scope for now:** Advanced predictive AI modeling for patient outcomes is not in scope.
