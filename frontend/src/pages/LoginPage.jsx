import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Container,
  Divider,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import SecurityIcon from '@mui/icons-material/Security';
import { useEffect, useState } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { usePageTitle } from '../hooks/usePageTitle.js';
import { useAuth } from '../hooks/useAuth.js';
import { getSsoProviders } from '../services/backendApi.js';

const accounts = [
  { label: 'Doctor User',     username: 'doctor1',  password: 'Doctor@1234'  },
  { label: 'Patient User',    username: 'patient1', password: 'Patient@1234' },
  { label: 'Technician User', username: 'tech1',    password: 'Tech@1234'    },
];

function defaultPathForRole(role) {
  return role === 'PATIENT' ? '/dashboard' : '/iam-dashboard';
}

/** Icon SVGs inlined for Google and Microsoft (no extra npm package needed) */
const GoogleIcon = () => (
  <svg width="18" height="18" viewBox="0 0 18 18" xmlns="http://www.w3.org/2000/svg">
    <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/>
    <path d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.258c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 009 18z" fill="#34A853"/>
    <path d="M3.964 10.707A5.41 5.41 0 013.682 9c0-.593.102-1.17.282-1.707V4.961H.957A8.996 8.996 0 000 9c0 1.452.348 2.827.957 4.039l3.007-2.332z" fill="#FBBC05"/>
    <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 00.957 4.961L3.964 7.293C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/>
  </svg>
);

const MicrosoftIcon = () => (
  <svg width="18" height="18" viewBox="0 0 21 21" xmlns="http://www.w3.org/2000/svg">
    <rect x="1"  y="1"  width="9" height="9" fill="#F25022"/>
    <rect x="11" y="1"  width="9" height="9" fill="#7FBA00"/>
    <rect x="1"  y="11" width="9" height="9" fill="#00A4EF"/>
    <rect x="11" y="11" width="9" height="9" fill="#FFB900"/>
  </svg>
);

export default function LoginPage() {
  usePageTitle('Login');
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [form, setForm] = useState({ username: 'patient1', password: 'Patient@1234' });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [ssoProviders, setSsoProviders] = useState([]);

  // Show error passed from SSO redirect failures
  useEffect(() => {
    const ssoError = searchParams.get('error');
    if (ssoError === 'sso_failed')   setError('SSO login failed. Please try again or use username/password.');
    if (ssoError === 'saml_failed')  setError('SAML login failed. Please try again or contact your IT administrator.');
  }, [searchParams]);

  // Load SSO providers from backend
  useEffect(() => {
    getSsoProviders().then(setSsoProviders).catch(() => {});
  }, []);

  if (isAuthenticated) {
    return <Navigate to={defaultPathForRole(user?.role)} replace />;
  }

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const loggedInUser = await login(form);
      navigate(defaultPathForRole(loggedInUser.role), { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSsoLogin = (provider) => {
    // Navigate directly to the backend SSO initiation URL.
    // The Vite proxy will forward this to Spring Security on :8080.
    window.location.href = provider.loginUrl;
  };

  const providerIcon = (id) => {
    if (id === 'google')    return <GoogleIcon />;
    if (id === 'microsoft') return <MicrosoftIcon />;
    return <SecurityIcon fontSize="small" />;
  };

  const providerColor = (id) => {
    if (id === 'google')    return { border: '#dadce0', bg: '#fff', hover: '#f8f9fa', text: '#3c4043' };
    if (id === 'microsoft') return { border: '#8c8c8c', bg: '#fff', hover: '#f3f3f3', text: '#5e5e5e' };
    return { border: '#0078d4', bg: '#0078d4', hover: '#106ebe', text: '#fff' };
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
      <Container maxWidth="sm">
        <Card>
          <CardContent sx={{ p: { xs: 3, md: 5 } }}>
            {/* Header */}
            <Stack alignItems="center" spacing={1.5} sx={{ mb: 3 }}>
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
                Rakshak IAM Security Platform
              </Typography>
              <Typography variant="body2" color="text.secondary" textAlign="center">
                Secure Identity &amp; Access Management Demonstration
              </Typography>
            </Stack>

            {/* SSO Buttons */}
            {ssoProviders.length > 0 && (
              <>
                <Stack spacing={1.5} sx={{ mb: 2 }}>
                  {ssoProviders.map((provider) => {
                    const colors = providerColor(provider.id);
                    return (
                      <Tooltip
                        key={provider.id}
                        title={
                          provider.id === 'google' || provider.id === 'microsoft'
                            ? 'Add your OAuth client credentials in application.properties to activate'
                            : 'Configure IdP metadata URI in application.properties to activate'
                        }
                        placement="right"
                      >
                        <Button
                          id={`sso-btn-${provider.id}`}
                          fullWidth
                          variant="outlined"
                          onClick={() => handleSsoLogin(provider)}
                          startIcon={providerIcon(provider.id)}
                          sx={{
                            justifyContent: 'center',
                            gap: 1,
                            py: 1.2,
                            borderColor: colors.border,
                            bgcolor: colors.bg,
                            color: colors.text,
                            fontWeight: 500,
                            letterSpacing: 0.2,
                            '&:hover': {
                              bgcolor: colors.hover,
                              borderColor: colors.border,
                            },
                          }}
                        >
                          {provider.displayName}
                        </Button>
                      </Tooltip>
                    );
                  })}
                </Stack>

                <Divider sx={{ my: 2 }}>
                  <Typography variant="caption" color="text.secondary">
                    or sign in with username
                  </Typography>
                </Divider>
              </>
            )}

            {/* Username / Password Form */}
            <Box component="form" onSubmit={submit}>
              <Stack spacing={2}>
                {error && <Alert severity="error">{error}</Alert>}
                <TextField
                  id="login-username"
                  label="Username"
                  value={form.username}
                  onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))}
                  autoComplete="username"
                  fullWidth
                  required
                />
                <TextField
                  id="login-password"
                  label="Password"
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                  autoComplete="current-password"
                  fullWidth
                  required
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton onClick={() => setShowPassword((value) => !value)} edge="end">
                          {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                />
                <Button id="login-submit" type="submit" variant="contained" size="large" disabled={loading}>
                  {loading ? <CircularProgress size={24} color="inherit" /> : 'Login'}
                </Button>
              </Stack>
            </Box>

            <Divider sx={{ my: 3 }} />
            <Typography variant="subtitle2" gutterBottom>
              Backend Test Accounts
            </Typography>
            <Stack spacing={1}>
              {accounts.map((account) => (
                <Button
                  key={account.username}
                  variant="outlined"
                  color="inherit"
                  onClick={() => setForm({ username: account.username, password: account.password })}
                  sx={{ justifyContent: 'space-between' }}
                >
                  <span>{account.label}</span>
                  <Typography component="span" variant="caption" color="text.secondary">
                    {account.username}
                  </Typography>
                </Button>
              ))}
            </Stack>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}
