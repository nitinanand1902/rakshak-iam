import { Alert, Box, Button, Card, CardContent, Chip, Skeleton, Stack, TextField, Typography } from '@mui/material';
import Grid from '@mui/material/Grid2';
import FingerprintIcon from '@mui/icons-material/Fingerprint';
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety';
import KeyIcon from '@mui/icons-material/Key';
import { useEffect, useState } from 'react';
import StatusChip from '../components/common/StatusChip.jsx';
import { usePageTitle } from '../hooks/usePageTitle.js';
import { useAuth } from '../hooks/useAuth.js';
import {
  createDiagnosisNote,
  fetchDiagnosisNotes,
  fetchPatientDashboard,
  fetchPatients,
} from '../services/backendApi.js';
import { ROLE_PERMISSIONS } from '../types/roles.js';

function InfoRow({ label, value }) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
      <Typography color="text.secondary">{label}</Typography>
      <Typography fontWeight={700} textAlign="right">
        {value}
      </Typography>
    </Box>
  );
}

function SectionCard({ title, icon, children }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          {icon}
          <Typography variant="h6">{title}</Typography>
        </Stack>
        {children}
      </CardContent>
    </Card>
  );
}

export default function PatientDashboard() {
  usePageTitle('Dashboard');
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [patients, setPatients] = useState([]);
  const [selectedPatientId, setSelectedPatientId] = useState('');
  const [notes, setNotes] = useState([]);
  const [clinicianLoading, setClinicianLoading] = useState(false);
  const [savingNote, setSavingNote] = useState(false);
  const [message, setMessage] = useState('');
  const [noteForm, setNoteForm] = useState({
    diagnosisText: '',
    icdCode: '',
    treatmentPlan: '',
  });

  useEffect(() => {
    if (user.role === 'PATIENT') {
      fetchPatientDashboard(user).then(setData);
    }
  }, [user]);

  useEffect(() => {
    if (user.role === 'PATIENT') return;

    setClinicianLoading(true);
    fetchPatients()
      .then((patientRows) => {
        setPatients(patientRows);
        setSelectedPatientId((current) => current || patientRows[0]?.id || '');
      })
      .finally(() => setClinicianLoading(false));
  }, [user.role]);

  useEffect(() => {
    if (!selectedPatientId) return;

    fetchDiagnosisNotes(selectedPatientId).then(setNotes);
  }, [selectedPatientId]);

  const selectedPatient = patients.find((patient) => patient.id === Number(selectedPatientId));

  const submitDiagnosis = async (event) => {
    event.preventDefault();
    setSavingNote(true);
    setMessage('');
    try {
      await createDiagnosisNote({
        patientId: Number(selectedPatientId),
        diagnosisText: noteForm.diagnosisText,
        icdCode: noteForm.icdCode,
        treatmentPlan: noteForm.treatmentPlan,
      });
      setNoteForm({ diagnosisText: '', icdCode: '', treatmentPlan: '' });
      setNotes(await fetchDiagnosisNotes(selectedPatientId));
      setMessage('Diagnosis note saved through the backend.');
    } catch (err) {
      setMessage(err.response?.data?.message || err.message);
    } finally {
      setSavingNote(false);
    }
  };

  if (user.role !== 'PATIENT') {
    return (
      <Box>
        <Typography variant="h4" sx={{ mb: 0.5 }}>
          Clinical Workspace
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          Patient records, diagnosis notes, and role-scoped clinical access for the current authenticated session.
        </Typography>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 5 }}>
            <SectionCard title="Patient Records" icon={<HealthAndSafetyIcon color="primary" />}>
              {clinicianLoading ? (
                <Skeleton variant="rounded" height={180} />
              ) : (
                <Stack spacing={1.2}>
                  {patients.map((patient) => (
                    <Button
                      key={patient.id}
                      variant={patient.id === Number(selectedPatientId) ? 'contained' : 'outlined'}
                      color="inherit"
                      onClick={() => setSelectedPatientId(patient.id)}
                      sx={{ justifyContent: 'space-between' }}
                    >
                      <span>{patient.fullName}</span>
                      <Typography component="span" variant="caption">
                        {patient.countryCode} · {patient.bloodGroup}
                      </Typography>
                    </Button>
                  ))}
                </Stack>
              )}
            </SectionCard>
          </Grid>

          <Grid size={{ xs: 12, md: 7 }}>
            <SectionCard title="Diagnosis Notes" icon={<FingerprintIcon color="primary" />}>
              {selectedPatient && (
                <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" sx={{ mb: 2 }}>
                  <Chip label={selectedPatient.fullName} color="primary" sx={{ borderRadius: 1.5, fontWeight: 700 }} />
                  <Chip label={`Blood Group ${selectedPatient.bloodGroup}`} variant="outlined" sx={{ borderRadius: 1.5, fontWeight: 700 }} />
                </Stack>
              )}

              <Stack spacing={1.5}>
                {notes.length === 0 && <Typography color="text.secondary">No diagnosis notes found for this patient.</Typography>}
                {notes.map((note) => (
                  <Box key={note.id} sx={{ borderBottom: '1px solid', borderColor: 'divider', pb: 1.5 }}>
                    <Typography fontWeight={800}>{note.diagnosisText}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {note.icdCode || 'No ICD code'} · {note.doctorName}
                    </Typography>
                    <Typography sx={{ mt: 0.5 }}>{note.treatmentPlan || 'No treatment plan recorded.'}</Typography>
                  </Box>
                ))}
              </Stack>
            </SectionCard>
          </Grid>

          {user.role === 'DOCTOR' && (
            <Grid size={{ xs: 12 }}>
              <SectionCard title="Add Diagnosis" icon={<VerifiedUserIcon color="primary" />}>
                <Box component="form" onSubmit={submitDiagnosis}>
                  <Stack spacing={2}>
                    {message && <Alert severity={message.includes('saved') ? 'success' : 'error'}>{message}</Alert>}
                    <TextField
                      label="Diagnosis"
                      value={noteForm.diagnosisText}
                      onChange={(event) => setNoteForm((current) => ({ ...current, diagnosisText: event.target.value }))}
                      required
                      fullWidth
                    />
                    <TextField
                      label="ICD Code"
                      value={noteForm.icdCode}
                      onChange={(event) => setNoteForm((current) => ({ ...current, icdCode: event.target.value }))}
                      fullWidth
                    />
                    <TextField
                      label="Doctor Notes / Treatment Plan"
                      value={noteForm.treatmentPlan}
                      onChange={(event) => setNoteForm((current) => ({ ...current, treatmentPlan: event.target.value }))}
                      required
                      fullWidth
                      multiline
                      minRows={3}
                    />
                    <Button type="submit" variant="contained" disabled={!selectedPatientId || savingNote}>
                      {savingNote ? 'Saving' : 'Save Diagnosis Note'}
                    </Button>
                  </Stack>
                </Box>
              </SectionCard>
            </Grid>
          )}

          <Grid size={{ xs: 12 }}>
            <SectionCard title="Role Information" icon={<KeyIcon color="primary" />}>
              <InfoRow label="Current Role" value={user.role} />
              <Typography color="text.secondary" sx={{ mt: 2, mb: 1 }}>
                Permissions
              </Typography>
              <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                {ROLE_PERMISSIONS[user.role].map((permission) => (
                  <Chip key={permission} label={permission} color="primary" variant="outlined" sx={{ borderRadius: 1.5 }} />
                ))}
              </Stack>
            </SectionCard>
          </Grid>
        </Grid>
      </Box>
    );
  }

  if (!data) {
    return (
      <Grid container spacing={3}>
        {[1, 2, 3, 4].map((item) => (
          <Grid key={item} size={{ xs: 12, md: 6 }}>
            <Skeleton variant="rounded" height={260} />
          </Grid>
        ))}
      </Grid>
    );
  }

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 0.5 }}>
        Patient Dashboard
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Your identity, health records, and role permissions for the current session.
      </Typography>
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <SectionCard title="Identity Information" icon={<FingerprintIcon color="primary" />}>
            <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
              <StatusChip label={data.identity.verificationStatus} color="success" />
              <Chip size="small" label="AES-256 Encrypted" color="primary" sx={{ borderRadius: 1.5, fontWeight: 700 }} />
            </Stack>
            <InfoRow label="Name" value={data.identity.name} />
            <InfoRow label="Country" value={data.identity.country} />
            <InfoRow label="Identity Type" value={data.identity.identityType} />
            <InfoRow label="Masked Identity Value" value={data.identity.maskedIdentityValue} />
            <InfoRow label="Verification Status" value={data.identity.verificationStatus} />
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <SectionCard title="Health Information" icon={<HealthAndSafetyIcon color="primary" />}>
            <InfoRow label="Blood Pressure" value={data.health.bloodPressure} />
            <InfoRow label="Heart Rate" value={data.health.heartRate} />
            <InfoRow label="Temperature" value={data.health.temperature} />
            <InfoRow label="Diagnosis" value={data.health.diagnosis} />
            <Typography color="text.secondary" sx={{ mt: 2 }}>
              Doctor Notes
            </Typography>
            <Typography fontWeight={600}>{data.health.doctorNotes}</Typography>
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12 }}>
          <SectionCard title="Role Information" icon={<KeyIcon color="primary" />}>
            <InfoRow label="Current Role" value={user.role} />
            <Typography color="text.secondary" sx={{ mt: 2, mb: 1 }}>
              Permissions
            </Typography>
            <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
              {ROLE_PERMISSIONS[user.role].map((permission) => (
                <Chip key={permission} label={permission} color="primary" variant="outlined" sx={{ borderRadius: 1.5 }} />
              ))}
            </Stack>
          </SectionCard>
        </Grid>
      </Grid>
    </Box>
  );
}
