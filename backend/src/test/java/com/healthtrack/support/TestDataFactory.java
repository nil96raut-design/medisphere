package com.healthtrack.support;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Reusable builder that creates consistent, fully-populated entity graphs
 * for tests.  Every entity that needs a hospital_id gets one.
 *
 * Usage (inside a test class that has @Autowired repositories):
 * <pre>{@code
 *   private TestDataFactory data;
 *
 *   @BeforeEach
 *   void setUp() {
 *     data = new TestDataFactory(hospitalRepository, userRepository, ...);
 *     hospital = data.createHospital();
 *     doctorUser = data.createUser(hospital, Role.DOCTOR);
 *     patient = data.createPatient(hospital);
 *   }
 * }</pre>
 */
public class TestDataFactory {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final BedRepository bedRepository;
    private final AppointmentRepository appointmentRepository;
    private final AdmissionRepository admissionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicineStockRepository medicineStockRepository;
    private final TaskRepository taskRepository;

    public TestDataFactory(
            HospitalRepository hospitalRepository,
            UserRepository userRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            BedRepository bedRepository,
            AppointmentRepository appointmentRepository,
            AdmissionRepository admissionRepository,
            MedicalRecordRepository medicalRecordRepository,
            MedicineStockRepository medicineStockRepository,
            TaskRepository taskRepository) {
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.bedRepository = bedRepository;
        this.appointmentRepository = appointmentRepository;
        this.admissionRepository = admissionRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.medicineStockRepository = medicineStockRepository;
        this.taskRepository = taskRepository;
    }

    // ---- Hospital ----

    public Hospital createHospital() {
        return hospitalRepository.save(Hospital.builder()
                .name("Test Hospital " + nano())
                .licenseNumber("LIC-" + nano())
                .contactEmail("hospital-" + nano() + "@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    // ---- User ----

    public User createUser(Hospital hospital, Role role) {
        return userRepository.save(User.builder()
                .fullName(role.name() + "-" + nano())
                .email(role.name().toLowerCase() + "-" + nano() + "@test.com")
                .passwordHash("x")
                .role(role)
                .hospital(hospital)
                .build());
    }

    public UserPrincipal createPrincipal(Hospital hospital, Role role) {
        return new UserPrincipal(createUser(hospital, role));
    }

    public UserPrincipal createPrincipal(User user) {
        return new UserPrincipal(user);
    }

    // ---- Patient ----

    public Patient createPatient(Hospital hospital) {
        return patientRepository.save(Patient.builder()
                .hospital(hospital)
                .firstName("First-" + nano())
                .lastName("Last-" + nano())
                .phoneNumber("555-" + nano())
                .build());
    }

    // ---- Doctor ----

    public record DoctorAndUser(User user, Doctor doctor) {}

    public DoctorAndUser createDoctor(Hospital hospital, User user) {
        return new DoctorAndUser(user, doctorRepository.save(Doctor.builder()
                .hospital(hospital)
                .user(user)
                .specialization("General")
                .consultationFee(new BigDecimal("200"))
                .isAvailable(true)
                .build()));
    }

    // ---- Bed ----

    public Bed createBed(Hospital hospital) {
        return bedRepository.save(Bed.builder()
                .hospital(hospital)
                .wardName("Ward-" + nano())
                .bedNumber("B-" + nano())
                .chargePerDay(new BigDecimal("500"))
                .isOccupied(false)
                .build());
    }

    // ---- Appointment ----

    public Appointment createCompletedAppointment(Hospital hospital, Patient patient, Doctor doctor) {
        return appointmentRepository.save(Appointment.builder()
                .hospital(hospital)
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .status(AppointmentStatus.COMPLETED)
                .build());
    }

    // ---- Admission ----

    public Admission createAdmission(Hospital hospital, Patient patient, User doctorUser, Bed bed) {
        return admissionRepository.save(Admission.builder()
                .hospital(hospital)
                .patient(patient)
                .doctor(doctorUser)
                .bed(bed)
                .admissionDate(LocalDate.now().minusDays(2))
                .status(AdmissionStatus.ADMITTED)
                .initialDiagnosis("Test diagnosis")
                .build());
    }

    // ---- Medicine Stock ----

    public MedicineStock createMedicineStock(Hospital hospital) {
        return medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital)
                .medicineName("Med-" + nano())
                .batchNumber("BATCH-" + nano())
                .expiryDate(LocalDate.now().plusYears(1))
                .availableQuantity(100)
                .unitPrice(new BigDecimal("10.00"))
                .build());
    }

    // ---- Task ----

    public Task createTask(Hospital hospital, User assignee, User assignedBy) {
        return taskRepository.save(Task.builder()
                .hospital(hospital)
                .title("Task-" + nano())
                .description("Auto-generated task")
                .assignee(assignee)
                .assignedBy(assignedBy)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(7))
                .status(TaskStatus.NOT_STARTED)
                .progressPercent(0)
                .build());
    }

    private static long nano() {
        return System.nanoTime();
    }
}
