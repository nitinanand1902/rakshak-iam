import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Container,
  Stack,
  Typography,
  Alert,
  Chip,
} from '@mui/material';
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety';
import LocalHospitalIcon from '@mui/icons-material/LocalHospital';
import PersonIcon from '@mui/icons-material/Person';
import BiotechIcon from '@mui/icons-material/Biotech';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import { usePageTitle } from '../hooks/usePageTitle.js';

const ROLES = [
  {
    id: 'PATIENT',
    label: 'Patient',
    description: 'View your own health records, identity documents, and diagnosis notes.',
    icon: <PersonIcon sx={{ fontSize: 36 }} />,
    color: '#1976d2',
    bg: 'rgba(25,118,210,0.06)',
    border: 'rgba(25,118,210,0.3)',
    permissions: ['View own profile', 'View own diagnosis', 'Manage identity documents'],
  },
  {
    id: 'DOCTOR',
    label: 'Doctor',
    description: 'Manage patients, create diagnosis notes, and access IAM/audit dashboards.',
    icon: <LocalHospitalIcon sx={{ fontSize: 36 }} />,
    color: '#2e7d32',
    bg: 'rgba(46,125,50,0.06)',
    border: 'rgba(46,125,50,0.3)',
    permissions: ['View all patients', 'Create diagnosis notes', 'Access audit logs', 'IAM evaluation'],
  },
  {
    id: 'TECHNICIAN',
    label: 'Technician',
    description: 'View patient records and diagnosis notes, run IAM evaluations.',
    icon: <BiotechIcon sx={{ fontSize: 36 }} />,
    color: '#7b1fa2',
    bg: 'rgba(123,31,162,0.06)',
    border: 'rgba(123,31,162,0.3)',
    permissions: ['View all patients', 'View diagnosis notes', 'IAM evaluation'],
  },
];

/**
 * SsoOnboarding — Role selection page for new SSO users.
 *
 * Shown after /sso-callback redirects here with a PROVISIONING token.
 * The user picks their role, which is sent to POST /api/v1/auth/sso/complete,
 * and then redirected to their dashboard.
 */
export default function SsoOnboarding() {
  usePageTitle('Choose Your Role');
  const { completeSsoOnboarding, provisioningToken } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const displayName = searchParams.get('displayName') || 'there';

  const [selected, setSelected]  = useState(null);
  const [loading,  setLoading]   = useState(false);
  const [error,    setError]     = useState('');

  // Guard: if no provisioning token, redirect to login
  if (!provisioningToken) {
    navigate('/login', { replace: true });
    return null;
  }

  const handleSubmit = async () => {
    if (!selected) return;
    setLoading(true);
    setError('');
    try {
      const user = await completeSsoOnboarding(selected);
      navigate(user.role === 'PATIENT' ? '/dashboard' : '/iam-dashboard', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Onboarding failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'grid',
        alignItems: 'center',
        py: 4,
        background:
          'linear-gradient(135deg, rgba(23,105,210,0.10), rgba(22,131,74,0.08)), #f6f8fb',
      }}
    >
      <Container maxWidth="md">
        <Card>
          <CardContent sx={{ p: { xs: 3, md: 5 } }}>
            {/* Header */}
            <Stack alignItems="center" spacing={1.5} sx={{ mb: 4 }}>
              <Box
                sx={{
                  width: 64,
                  height: 64,
                  borderRadius: 3,
                  display: 'grid',
                  placeItems: 'center',
                  bgcolor: 'primary.light',
                  color: 'primary.main',
                }}
              >
                <HealthAndSafetyIcon fontSize="large" />
              </Box>
              <Typography variant="h5" textAlign="center">
                Welcome, {decodeURIComponent(displayName)}!
              </Typography>
              <Typography variant="body2" color="text.secondary" textAlign="center" maxWidth={480}>
                Your SSO identity has been verified. Please select the role that matches your
                position in the healthcare system to complete your account setup.
              </Typography>
            </Stack>

            {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

            {/* Role Cards */}
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={2}
              sx={{ mb: 4 }}
            >
              {ROLES.map((role) => {
                const isSelected = selected === role.id;
                return (
                  <Box
                    key={role.id}
                    id={`role-card-${role.id.toLowerCase()}`}
                    onClick={() => setSelected(role.id)}
                    sx={{
                      flex: 1,
                      border: `2px solid ${isSelected ? role.color : role.border}`,
                      borderRadius: 3,
                      p: 2.5,
                      cursor: 'pointer',
                      bgcolor: isSelected ? role.bg : 'background.paper',
                      transition: 'all 0.2s ease',
                      position: 'relative',
                      '&:hover': {
                        bgcolor: role.bg,
                        borderColor: role.color,
                        transform: 'translateY(-2px)',
                        boxShadow: `0 4px 20px ${role.bg}`,
                      },
                    }}
                  >
                    {isSelected && (
                      <CheckCircleOutlineIcon
                        sx={{
                          position: 'absolute',
                          top: 10,
                          right: 10,
                          color: role.color,
                          fontSize: 22,
                        }}
                      />
                    )}
                    <Stack spacing={1.5}>
                      <Box sx={{ color: role.color }}>{role.icon}</Box>
                      <Typography variant="h6" fontWeight={600}>
                        {role.label}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ minHeight: 48 }}>
                        {role.description}
                      </Typography>
                      <Stack direction="row" flexWrap="wrap" gap={0.5}>
                        {role.permissions.map((p) => (
                          <Chip
                            key={p}
                            label={p}
                            size="small"
                            sx={{
                              fontSize: '0.68rem',
                              bgcolor: role.bg,
                              color: role.color,
                              border: `1px solid ${role.border}`,
                            }}
                          />
                        ))}
                      </Stack>
                    </Stack>
                  </Box>
                );
              })}
            </Stack>

            {/* Submit */}
            <Stack alignItems="center" spacing={1}>
              <Button
                id="sso-onboarding-submit"
                variant="contained"
                size="large"
                disabled={!selected || loading}
                onClick={handleSubmit}
                sx={{ minWidth: 240, py: 1.5 }}
              >
                {loading ? (
                  <CircularProgress size={24} color="inherit" />
                ) : (
                  `Continue as ${selected ? ROLES.find((r) => r.id === selected)?.label : '…'}`
                )}
              </Button>
              <Typography variant="caption" color="text.disabled">
                This role determines your access level and cannot be changed without contacting an administrator.
              </Typography>
            </Stack>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}
