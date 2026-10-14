package com.healthcare.config;

import com.healthcare.entity.*;
import com.healthcare.repository.*;
import com.healthcare.util.EncryptionService;
import com.healthcare.util.MaskingStrategy;
import com.healthcare.util.MaskingStrategyRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the database with demo users for all three roles.
 * Runs once at startup; skips seeding if data already exists.
 * <p>
 * Demo accounts:
 * <pre>
 *   patient1   / Patient@1234  (PATIENT)
 *   doctor1    / Doctor@1234   (DOCTOR)
 *   tech1      / Tech@1234     (TECHNICIAN)
 * </pre>
 */
@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TechnicianRepository technicianRepository;
    private final IdentityDocumentRepository identityDocumentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final MaskingStrategyRegistry maskingStrategyRegistry;

    public DataInitializer(UserRepository userRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository,
                           TechnicianRepository technicianRepository,
                           IdentityDocumentRepository identityDocumentRepository,
                           PasswordEncoder passwordEncoder,
                           EncryptionService encryptionService,
                           MaskingStrategyRegistry maskingStrategyRegistry) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.technicianRepository = technicianRepository;
        this.identityDocumentRepository = identityDocumentRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.maskingStrategyRegistry = maskingStrategyRegistry;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername("patient1")) {
            log.info("DataInitializer: data already exists, skipping seed.");
            return;
        }

        log.info("DataInitializer: seeding demo data...");

        // ── Patient ──────────────────────────────────────────────────────────
        User patientUser = createUser("patient1", "patient1@healthcare.com", "Patient@1234", Role.PATIENT);
        Patient patient = Patient.builder()
                .user(patientUser)
                .fullName("Arjun Sharma")
                .bloodGroup("O+")
                .countryCode("IN")
                .build();
        patient = patientRepository.save(patient);

        // Add Aadhaar document
        addIdentityDocument(patient, DocumentType.AADHAAR, "123412341234", "IN");

        // ── Doctor ───────────────────────────────────────────────────────────
        User doctorUser = createUser("doctor1", "doctor1@healthcare.com", "Doctor@1234", Role.DOCTOR);
        Doctor doctor = Doctor.builder()
                .user(doctorUser)
                .fullName("Dr. Priya Mehta")
                .specialization("Cardiology")
                .licenseNumber("MCI-2024-001")
                .build();
        doctorRepository.save(doctor);

        // ── Technician ───────────────────────────────────────────────────────
        User techUser = createUser("tech1", "tech1@healthcare.com", "Tech@1234", Role.TECHNICIAN);
        Technician technician = Technician.builder()
                .user(techUser)
                .fullName("Rohit Verma")
                .department("Radiology")
                .build();
        technicianRepository.save(technician);

        // ── US Patient (SSN) ─────────────────────────────────────────────────
        User usPatientUser = createUser("john_us", "john@healthcare.com", "Patient@1234", Role.PATIENT);
        Patient usPatient = Patient.builder()
                .user(usPatientUser)
                .fullName("John Smith")
                .bloodGroup("A+")
                .countryCode("US")
                .build();
        usPatient = patientRepository.save(usPatient);
        addIdentityDocument(usPatient, DocumentType.SSN, "123456789", "US");

        log.info("DataInitializer: seeding complete. Demo users: patient1, doctor1, tech1, john_us");
    }

    // -------------------------------------------------------------------------
    private User createUser(String username, String email, String rawPassword, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    private void addIdentityDocument(Patient patient, DocumentType docType, String rawValue, String countryCode) {
        try {
            MaskingStrategy strategy = maskingStrategyRegistry.resolve(docType);
            String masked = strategy.mask(rawValue);
            String encrypted = encryptionService.encrypt(rawValue);

            IdentityDocument doc = IdentityDocument.builder()
                    .patient(patient)
                    .documentType(docType)
                    .encryptedDocumentValue(encrypted)
                    .maskedValue(masked)
                    .countryCode(countryCode)
                    .verificationStatus(VerificationStatus.PENDING)
                    .build();
            identityDocumentRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to add identity document for patient {}: {}", patient.getId(), e.getMessage());
        }
    }
}
