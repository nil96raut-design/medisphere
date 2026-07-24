package com.healthtrack.config;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final BedRepository bedRepository;
    private final AppointmentRepository appointmentRepository;
    private final AdmissionRepository admissionRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final MedicineStockRepository medicineStockRepository;
    private final DispensationRecordRepository dispensationRecordRepository;
    private final BillRepository billRepository;
    private final TaskRepository taskRepository;
    private final ProgressNoteRepository progressNoteRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // ============================================================
        // 1. HOSPITAL
        // ============================================================
        Hospital hospital = hospitalRepository.save(Hospital.builder()
                .name("MediSphere Premium Hospital")
                .licenseNumber("MS-HOSP-001")
                .contactEmail("admin@medisphere.com")
                .subscriptionTier(SubscriptionTier.FREE_TRIAL)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());

        // ============================================================
        // 2. USERS (all 7 roles)
        // ============================================================
        User admin = userRepository.save(User.builder()
                .fullName("Priya Sharma")
                .email("admin@medisphere.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.ADMIN)
                .hospital(hospital)
                .build());

        User doctor = userRepository.save(User.builder()
                .fullName("Dr. Asha Mehta")
                .email("doctor@medisphere.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.DOCTOR)
                .hospital(hospital)
                .build());

        User receptionist = userRepository.save(User.builder()
                .fullName("Rohan Patil")
                .email("receptionist@medisphere.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.RECEPTIONIST)
                .hospital(hospital)
                .build());

        User nurse = userRepository.save(User.builder()
                .fullName("Ananya Reddy")
                .email("nurse@medisphere.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.NURSE)
                .hospital(hospital)
                .build());

        User pharmacist = userRepository.save(User.builder()
                .fullName("Vikram Joshi")
                .email("pharmacist@medisphere.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.PHARMACIST)
                .hospital(hospital)
                .build());

        User labTech = userRepository.save(User.builder()
                .fullName("Sneha Kapoor")
                .email("labtech@medisphere.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.LAB_TECH)
                .hospital(hospital)
                .build());

        User patientUser = userRepository.save(User.builder()
                .fullName("Meera Iyer")
                .email("patient@medisphere.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.PATIENT)
                .hospital(hospital)
                .primaryDoctor(doctor)
                .allergies("Penicillin, Peanuts")
                .build());

        // ============================================================
        // 3. DOCTOR PROFILE
        // ============================================================
        Doctor doctorProfile = doctorRepository.save(Doctor.builder()
                .hospital(hospital)
                .user(doctor)
                .specialization("Cardiology")
                .consultationFee(new BigDecimal("150.00"))
                .isAvailable(true)
                .build());

        // ============================================================
        // 4. PATIENTS (8 diverse patients)
        // ============================================================
        List<com.healthtrack.entity.Patient> patients = patientRepository.saveAll(List.of(
                com.healthtrack.entity.Patient.builder().hospital(hospital).firstName("Alice").lastName("Vance").gender("Female").dateOfBirth(LocalDate.of(1985, 3, 15)).phoneNumber("+1-555-0101").email("alice@example.com").emergencyContact("+1-555-0199").insuranceProvider("BlueCross").policyNumber("BC-12345").build(),
                com.healthtrack.entity.Patient.builder().hospital(hospital).firstName("David").lastName("Miller").gender("Male").dateOfBirth(LocalDate.of(1992, 7, 22)).phoneNumber("+1-555-0102").email("david@example.com").emergencyContact("+1-555-0198").insuranceProvider("Aetna").policyNumber("AE-67890").build(),
                com.healthtrack.entity.Patient.builder().hospital(hospital).firstName("Sarah").lastName("Jenkins").gender("Female").dateOfBirth(LocalDate.of(1978, 11, 2)).phoneNumber("+1-555-0103").email("sarah@example.com").emergencyContact("+1-555-0197").build(),
                com.healthtrack.entity.Patient.builder().hospital(hospital).firstName("Michael").lastName("Chen").gender("Male").dateOfBirth(LocalDate.of(1995, 1, 10)).phoneNumber("+1-555-0104").email("michael@example.com").build(),
                com.healthtrack.entity.Patient.builder().hospital(hospital).firstName("Emily").lastName("Davis").gender("Female").dateOfBirth(LocalDate.of(1989, 9, 5)).phoneNumber("+1-555-0105").email("emily@example.com").insuranceProvider("Cigna").policyNumber("CG-54321").build(),
                com.healthtrack.entity.Patient.builder().hospital(hospital).firstName("Marcus").lastName("Thompson").gender("Male").dateOfBirth(LocalDate.of(1965, 4, 18)).phoneNumber("+1-555-0106").emergencyContact("+1-555-0196").build(),
                com.healthtrack.entity.Patient.builder().hospital(hospital).firstName("Linda").lastName("Garcia").gender("Female").dateOfBirth(LocalDate.of(2000, 6, 30)).phoneNumber("+1-555-0107").email("linda@example.com").build(),
                com.healthtrack.entity.Patient.builder().hospital(hospital).firstName("Robert").lastName("Taylor").gender("Male").dateOfBirth(LocalDate.of(1972, 12, 25)).phoneNumber("+1-555-0108").email("robert@example.com").insuranceProvider("UnitedHealth").policyNumber("UH-98765").build()
        ));

        Patient alice = patients.get(0);
        Patient david = patients.get(1);
        Patient sarah = patients.get(2);
        Patient michael = patients.get(3);
        Patient emily = patients.get(4);
        Patient marcus = patients.get(5);
        Patient linda = patients.get(6);
        Patient robert = patients.get(7);

        // ============================================================
        // 5. BEDS (6 beds across 3 wards)
        // ============================================================
        List<Bed> beds = bedRepository.saveAll(List.of(
                Bed.builder().hospital(hospital).wardName("General Ward").bedNumber("GW-101").chargePerDay(new BigDecimal("50.00")).isOccupied(false).build(),
                Bed.builder().hospital(hospital).wardName("General Ward").bedNumber("GW-102").chargePerDay(new BigDecimal("50.00")).isOccupied(false).build(),
                Bed.builder().hospital(hospital).wardName("ICU").bedNumber("ICU-201").chargePerDay(new BigDecimal("250.00")).isOccupied(false).build(),
                Bed.builder().hospital(hospital).wardName("ICU").bedNumber("ICU-202").chargePerDay(new BigDecimal("250.00")).isOccupied(false).build(),
                Bed.builder().hospital(hospital).wardName("Maternity").bedNumber("MT-301").chargePerDay(new BigDecimal("120.00")).isOccupied(false).build(),
                Bed.builder().hospital(hospital).wardName("Maternity").bedNumber("MT-302").chargePerDay(new BigDecimal("120.00")).isOccupied(false).build()
        ));

        // ============================================================
        // 6. APPOINTMENTS (5 appointments)
        // ============================================================
        LocalDate today = LocalDate.now();
        List<Appointment> appointments = appointmentRepository.saveAll(List.of(
                Appointment.builder().hospital(hospital).patient(alice).doctor(doctorProfile).appointmentDate(today).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).status(AppointmentStatus.SCHEDULED).build(),
                Appointment.builder().hospital(hospital).patient(david).doctor(doctorProfile).appointmentDate(today).startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0)).status(AppointmentStatus.SCHEDULED).build(),
                Appointment.builder().hospital(hospital).patient(sarah).doctor(doctorProfile).appointmentDate(today).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).status(AppointmentStatus.SCHEDULED).build(),
                Appointment.builder().hospital(hospital).patient(michael).doctor(doctorProfile).appointmentDate(today).startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 0)).status(AppointmentStatus.SCHEDULED).build(),
                Appointment.builder().hospital(hospital).patient(emily).doctor(doctorProfile).appointmentDate(today.minusDays(1)).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).status(AppointmentStatus.COMPLETED).build()
        ));

        // ============================================================
        // 7. ADMISSIONS (2 admissions)
        // ============================================================
        Bed icuBed = beds.get(2);
        icuBed.setIsOccupied(true);
        bedRepository.save(icuBed);

        Bed gwBed = beds.get(0);
        gwBed.setIsOccupied(true);
        bedRepository.save(gwBed);

        List<Admission> admissions = admissionRepository.saveAll(List.of(
                Admission.builder().hospital(hospital).patient(marcus).doctor(doctor).bed(icuBed).admissionDate(today.minusDays(2)).initialDiagnosis("Acute Myocardial Infarction - STEMI").status(AdmissionStatus.ADMITTED).build(),
                Admission.builder().hospital(hospital).patient(robert).doctor(doctor).bed(gwBed).admissionDate(today.minusDays(5)).dischargeDate(today.minusDays(1)).initialDiagnosis("Type 2 Diabetes - Uncontrolled").dischargeSummary("Blood glucose stabilized. Discharged with Metformin 500mg BID. Follow-up in 2 weeks.").status(AdmissionStatus.DISCHARGED).build()
        ));

        // ============================================================
        // 8. LAB TEST ORDERS (3 orders in different statuses)
        // ============================================================
        List<LabTestOrder> labOrders = labTestOrderRepository.saveAll(List.of(
                LabTestOrder.builder().hospital(hospital).patient(alice).testName("Complete Blood Count (CBC)").requestedBy(doctor).status(LabOrderStatus.ORDERED).price(new BigDecimal("75.00")).build(),
                LabTestOrder.builder().hospital(hospital).patient(david).testName("Lipid Panel").requestedBy(doctor).status(LabOrderStatus.SAMPLE_COLLECTED).technicianNotes("Sample drawn, awaiting analysis").price(new BigDecimal("120.00")).build(),
                LabTestOrder.builder().hospital(hospital).patient(emily).testName("Thyroid Panel (TSH, T3, T4)").requestedBy(doctor).status(LabOrderStatus.RESULT_READY).resultValues("TSH: 2.5 mIU/L (0.4-4.0)\nT3: 1.2 nmol/L (1.0-2.8)\nT4: 85 nmol/L (60-140)\nAll values within normal range.").completedAt(java.time.LocalDateTime.now().minusHours(3)).price(new BigDecimal("150.00")).build()
        ));

        // ============================================================
        // 9. PHARMACY STOCK (8 medicines)
        // ============================================================
        medicineStockRepository.saveAll(List.of(
                MedicineStock.builder().hospital(hospital).medicineName("Paracetamol 500mg").batchNumber("PCM-B001").expiryDate(LocalDate.now().plusMonths(12)).availableQuantity(500).reorderLevel(50).unitPrice(new BigDecimal("2.50")).build(),
                MedicineStock.builder().hospital(hospital).medicineName("Amoxicillin 250mg").batchNumber("AMX-B002").expiryDate(LocalDate.now().plusMonths(8)).availableQuantity(200).reorderLevel(30).unitPrice(new BigDecimal("5.00")).build(),
                MedicineStock.builder().hospital(hospital).medicineName("Ibuprofen 400mg").batchNumber("IBU-B003").expiryDate(LocalDate.now().plusMonths(6)).availableQuantity(350).reorderLevel(40).unitPrice(new BigDecimal("3.00")).build(),
                MedicineStock.builder().hospital(hospital).medicineName("Metformin 500mg").batchNumber("MET-B004").expiryDate(LocalDate.now().plusMonths(10)).availableQuantity(8).reorderLevel(20).unitPrice(new BigDecimal("4.50")).build(),
                MedicineStock.builder().hospital(hospital).medicineName("Atorvastatin 10mg").batchNumber("ATR-B005").expiryDate(LocalDate.now().plusMonths(14)).availableQuantity(150).reorderLevel(25).unitPrice(new BigDecimal("7.00")).build(),
                MedicineStock.builder().hospital(hospital).medicineName("Omeprazole 20mg").batchNumber("OME-B006").expiryDate(LocalDate.now().plusMonths(5)).availableQuantity(3).reorderLevel(15).unitPrice(new BigDecimal("6.00")).build(),
                MedicineStock.builder().hospital(hospital).medicineName("Aspirin 75mg").batchNumber("ASP-B007").expiryDate(LocalDate.now().plusMonths(20)).availableQuantity(100).reorderLevel(20).unitPrice(new BigDecimal("1.50")).build(),
                MedicineStock.builder().hospital(hospital).medicineName("Cetirizine 10mg").batchNumber("CET-B008").expiryDate(LocalDate.now().minusMonths(1)).availableQuantity(60).reorderLevel(15).unitPrice(new BigDecimal("2.00")).build()
        ));

        // ============================================================
        // 10. BILL (1 completed bill)
        // ============================================================
        Bill completedBill = billRepository.save(Bill.builder()
                .hospital(hospital)
                .patient(emily)
                .idempotencyKey("BILL-001-EMILY")
                .totalAmount(new BigDecimal("380.00"))
                .discountAmount(new BigDecimal("30.00"))
                .insuranceCoveredAmount(new BigDecimal("150.00"))
                .netPayable(new BigDecimal("200.00"))
                .paymentStatus(PaymentStatus.PAID)
                .paymentMode(PaymentMode.CARD)
                .build());

        // ============================================================
        // 11. TASKS (for the patient dashboard)
        // ============================================================
        Task t1 = taskRepository.save(Task.builder()
                .title("Take morning medication")
                .description("500mg Metformin with breakfast - important for blood sugar control.")
                .assignee(patientUser)
                .assignedBy(doctor)
                .hospital(hospital)
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .progressPercent(60)
                .dueDate(today.plusDays(1))
                .build());

        Task t2 = taskRepository.save(Task.builder()
                .title("Complete pre-surgery bloodwork")
                .description("Visit the lab for CBC and metabolic panel before next appointment.")
                .assignee(patientUser)
                .assignedBy(receptionist)
                .hospital(hospital)
                .status(TaskStatus.NOT_STARTED)
                .priority(TaskPriority.URGENT)
                .progressPercent(0)
                .dueDate(today.plusDays(3))
                .build());

        Task t3 = taskRepository.save(Task.builder()
                .title("Schedule follow-up with cardiologist")
                .description("Call reception to book follow-up appointment within 2 weeks of discharge.")
                .assignee(patientUser)
                .assignedBy(doctor)
                .hospital(hospital)
                .status(TaskStatus.COMPLETED)
                .priority(TaskPriority.MEDIUM)
                .progressPercent(100)
                .dueDate(today.minusDays(1))
                .build());

        progressNoteRepository.save(ProgressNote.builder()
                .task(t1)
                .author(patientUser)
                .hospital(hospital)
                .note("Took today's dose with breakfast at 8 AM.")
                .progressPercent(60)
                .status(TaskStatus.IN_PROGRESS)
                .build());

        progressNoteRepository.save(ProgressNote.builder()
                .task(t2)
                .author(doctor)
                .hospital(hospital)
                .note("Lab order has been created. Patient needs to visit the collection center.")
                .progressPercent(0)
                .status(TaskStatus.NOT_STARTED)
                .build());

        progressNoteRepository.save(ProgressNote.builder()
                .task(t3)
                .author(patientUser)
                .hospital(hospital)
                .note("Completed - appointment booked for next week.")
                .progressPercent(100)
                .status(TaskStatus.COMPLETED)
                .build());
    }
}
