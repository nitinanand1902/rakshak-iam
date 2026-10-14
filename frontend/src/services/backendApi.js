import api from './api.js';
import { ROLE_PERMISSIONS } from '../types/roles.js';

const securityControls = [
  'JWT Authentication Enabled',
  'AES Encryption Enabled',
  'BCrypt Enabled',
  'Audit Logging Enabled',
  'Role Based Access Control Enabled',
  'OAuth2/OIDC SSO Enabled',
  'SAML 2.0 SSO Enabled',
];

function scoreFromRisk(risk) {
  if (risk === 'LOW') return { authenticationScore: 95, authorizationScore: 96, sessionSecurity: 92 };
  if (risk === 'MEDIUM') return { authenticationScore: 84, authorizationScore: 88, sessionSecurity: 80 };
  if (risk === 'HIGH') return { authenticationScore: 70, authorizationScore: 74, sessionSecurity: 68 };
  return { authenticationScore: 55, authorizationScore: 60, sessionSecurity: 52 };
}

export async function loginRequest(credentials) {
  const { data } = await api.post('/auth/login', credentials);
  return {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    user: {
      username: data.username,
      name: data.username,
      role: data.role,
      permissions: ROLE_PERMISSIONS[data.role] || [],
    },
  };
}

export async function logoutRequest() {
  try {
    await api.post('/auth/logout');
  } catch {
    // Local logout should still complete if the access token has expired.
  }
}

export async function fetchPatientDashboard(user) {
  if (user.role !== 'PATIENT') {
    return {
      identity: {
        name: user.name || user.username,
        country: 'Rakshak IAM',
        identityType: `${user.role} ACCOUNT`,
        maskedIdentityValue: user.username,
        verificationStatus: 'Active',
      },
      health: {
        bloodPressure: 'Restricted',
        heartRate: 'Restricted',
        temperature: 'Restricted',
        diagnosis: 'Role-based data access enforced',
        doctorNotes: 'Clinical data is only loaded from patient-scoped endpoints.',
      },
      security: securityControls,
    };
  }

  const { data: patient } = await api.get('/patients/me');
  const [{ data: iam }, docsResult, notesResult] = await Promise.all([
    api.get('/iam/evaluate/me'),
    api.get(`/identity/patients/${patient.id}`).catch(() => ({ data: [] })),
    api.get(`/diagnosis/patients/${patient.id}?size=1`).catch(() => ({ data: { content: [] } })),
  ]);

  const identityDoc = docsResult.data?.[0] || iam.identityDocuments?.[0];
  const latestNote = notesResult.data?.content?.[0];

  return {
    identity: {
      name: patient.fullName,
      country: patient.countryCode,
      identityType: identityDoc?.documentType || 'NOT_PROVIDED',
      maskedIdentityValue: identityDoc?.maskedValue || 'Not available',
      verificationStatus: identityDoc?.verificationStatus || identityDoc?.status || 'Pending',
    },
    health: {
      bloodPressure: patient.bloodGroup ? `Blood Group ${patient.bloodGroup}` : 'Not available',
      heartRate: 'Available through clinical systems',
      temperature: 'Available through clinical systems',
      diagnosis: latestNote?.diagnosisText || 'No diagnosis notes found',
      doctorNotes: latestNote?.treatmentPlan || 'No doctor notes found for this patient.',
    },
    security: securityControls,
  };
}

export async function fetchPatients({ page = 0, pageSize = 20 } = {}) {
  const { data } = await api.get('/patients', {
    params: {
      page,
      size: pageSize,
      sort: 'id,asc',
    },
  });
  return data.content;
}

export async function fetchDiagnosisNotes(patientId, { page = 0, pageSize = 20 } = {}) {
  const { data } = await api.get(`/diagnosis/patients/${patientId}`, {
    params: {
      page,
      size: pageSize,
      sort: 'createdAt,desc',
    },
  });
  return data.content;
}

export async function createDiagnosisNote(note) {
  const { data } = await api.post('/diagnosis', note);
  return data;
}

export async function runIamEvaluation(username) {
  const endpoint = username ? `/iam/evaluate/${username}` : '/iam/evaluate/me';
  const { data } = await api.get(endpoint);
  const scores = scoreFromRisk(data.overallRisk);

  return {
    ...scores,
    risk: data.overallRisk,
    generatedAt: data.evaluatedAt,
    raw: data,
    summary:
      data.securityFlags?.length > 0
        ? `IAM evaluation completed for ${data.username}. Flags: ${data.securityFlags.join(', ')}.`
        : `IAM evaluation completed for ${data.username}. No active IAM flags were returned by the backend.`,
  };
}

export async function fetchAuditLogs({ page = 0, pageSize = 25 } = {}) {
  const { data } = await api.get('/audit', {
    params: {
      page,
      size: pageSize,
      sort: 'timestamp,desc',
    },
  });

  return {
    rows: data.content.map((row) => ({
      id: row.id,
      timestamp: row.timestamp,
      user: row.username,
      role: 'Protected',
      action: row.description || row.eventType,
      endpoint: `${row.resourceType || 'System'}:${row.resourceId || 'n/a'}`,
      status: row.success ? 'SUCCESS' : 'BLOCKED',
      ipAddress: row.ipAddress || 'masked',
      eventType: row.eventType,
    })),
    total: data.totalElements,
  };
}

// ============================================================
// SSO / Federated Authentication
// ============================================================

/**
 * Returns the list of configured SSO providers from the backend.
 * Used to render the social login buttons on the login page.
 */
export async function getSsoProviders() {
  try {
    const { data } = await api.get('/auth/sso/providers');
    return data.providers || [];
  } catch {
    // If backend is unreachable or SSO not wired up, return static defaults
    return [
      { id: 'google',    displayName: 'Sign in with Google',    loginUrl: '/login/oauth2/authorization/google',    type: 'OIDC' },
      { id: 'microsoft', displayName: 'Sign in with Microsoft', loginUrl: '/login/oauth2/authorization/microsoft', type: 'OIDC' },
      { id: 'saml',      displayName: 'Login with SSO',         loginUrl: '/saml2/authenticate/default-idp',       type: 'SAML' },
    ];
  }
}

/**
 * Completes the SSO onboarding step — exchanges the provisioning token for
 * a full JWT pair after the user picks their role.
 *
 * @param {string} provisioningToken  the short-lived PROVISIONING JWT
 * @param {string} role               chosen role: PATIENT | DOCTOR | TECHNICIAN
 */
export async function completeSsoProvisioning(provisioningToken, role) {
  const { data } = await api.post('/auth/sso/complete', { provisioningToken, role });
  return {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    user: {
      username: data.username,
      name: data.username,
      role: data.role,
      permissions: ROLE_PERMISSIONS[data.role] || [],
    },
  };
}
