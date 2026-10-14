-- Rakshak IAM Security Platform demo data.
-- H2-compatible SQL for creating synthetic demo users and audit activity.
-- No real patient/user information is included.
--
-- Demo credentials:
--   patient1 / Patient@1234
--   doctor1  / Doctor@1234
--   tech1    / Tech@1234
--
-- Run after Hibernate has created the schema, or paste into the H2 console.

MERGE INTO users (
  id, username, email, password, role, enabled, account_locked,
  account_locked_until, failed_login_attempts, created_at, updated_at
) KEY(username) VALUES
  (1, 'patient1', 'patient1@demo.local', '$2y$12$hmhKql0Vw8EA1AUI9WcO0OOdu0UHQ9Bd7TsXTuS/FtCEBQEOHeYya', 'PATIENT', TRUE, FALSE, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'doctor1', 'doctor1@demo.local', '$2y$12$x.hm8U3mIAFRNmudDfDijOJByI2KqTgOvfsZXisnB1EWoH5QjuxfO', 'DOCTOR', TRUE, FALSE, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'tech1', 'tech1@demo.local', '$2y$12$oHm4qSWH4rfL7wCALAdSLOm0vQDjvv5gKJ3nEHa050Q0f0LMsZ8W.', 'TECHNICIAN', TRUE, FALSE, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO patients (
  id, user_id, full_name, date_of_birth, blood_group, country_code, created_at
) KEY(user_id) VALUES
  (1, 1, 'Demo Patient', DATE '1990-01-01', 'O+', 'IN', CURRENT_TIMESTAMP);

MERGE INTO doctors (
  id, user_id, full_name, specialization, license_number, created_at
) KEY(user_id) VALUES
  (1, 2, 'Demo Doctor', 'Cardiology', 'DEMO-MCI-001', CURRENT_TIMESTAMP);

MERGE INTO technicians (
  id, user_id, full_name, department, created_at
) KEY(user_id) VALUES
  (1, 3, 'Demo Technician', 'Radiology', CURRENT_TIMESTAMP);

MERGE INTO identity_documents (
  id, patient_id, document_type, encrypted_document_value, masked_value,
  country_code, verification_status, created_at, verified_at
) KEY(id) VALUES
  (1, 1, 'AADHAAR', 'DEMO_AES256_GCM_CIPHERTEXT_ONLY_NOT_RAW_IDENTITY_VALUE', 'XXXXXXXX1234', 'IN', 'VERIFIED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO audit_logs (
  id, event_type, username, resource_type, resource_id, description,
  ip_address, success, timestamp
) KEY(id) VALUES
  (1, 'LOGIN_SUCCESS', 'doctor1', 'User', '2', 'Login successful', '10.20.1.42', TRUE, DATEADD('MINUTE', -55, CURRENT_TIMESTAMP)),
  (2, 'LOGIN_SUCCESS', 'patient1', 'User', '1', 'Login successful', '10.20.4.18', TRUE, DATEADD('MINUTE', -48, CURRENT_TIMESTAMP)),
  (3, 'DATA_ACCESS', 'patient1', 'Patient', '1', 'Own profile viewed', '10.20.4.18', TRUE, DATEADD('MINUTE', -45, CURRENT_TIMESTAMP)),
  (4, 'ACCESS_DENIED', 'tech1', 'AuditLog', 'all', 'Attempted doctor-only audit access', '10.20.8.12', FALSE, DATEADD('MINUTE', -39, CURRENT_TIMESTAMP)),
  (5, 'DATA_ACCESS', 'doctor1', 'IAM', 'patient1', 'IAM posture evaluated', '10.20.1.42', TRUE, DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)),
  (6, 'SUSPICIOUS_ACTIVITY', 'unknown', 'Token', NULL, 'Malformed JWT rejected', '198.51.100.7', FALSE, DATEADD('MINUTE', -18, CURRENT_TIMESTAMP));
