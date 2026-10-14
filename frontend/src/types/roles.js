export const ROLES = {
  PATIENT: 'PATIENT',
  DOCTOR: 'DOCTOR',
  TECHNICIAN: 'TECHNICIAN',
};

export const ROLE_PERMISSIONS = {
  PATIENT: ['View Own Records', 'Review Identity Status', 'View Audit Events'],
  DOCTOR: ['View Patients', 'Add Diagnosis', 'Add Notes', 'Review IAM Scores'],
  TECHNICIAN: ['Upload Reports', 'Validate Report Metadata', 'View Assigned Work'],
};
