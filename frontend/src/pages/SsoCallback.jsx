import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, CircularProgress, Typography } from '@mui/material';
import { useAuth } from '../hooks/useAuth.js';
import { usePageTitle } from '../hooks/usePageTitle.js';

/**
 * SsoCallback — Landing page for the SSO redirect flow.
 *
 * The backend redirects here after a successful OIDC or SAML login
 * with one of two URL shapes:
 *
 * New user (needs role selection):
 *   /sso-callback?state=provisioning&token=<provisioning-jwt>&displayName=John+Doe
 *
 * Returning user (fully provisioned):
 *   /sso-callback?state=authenticated&accessToken=<jwt>&refreshToken=<jwt>&username=john&role=DOCTOR
 *
 * This page reads the query params, updates auth state, then redirects appropriately.
 */
export default function SsoCallback() {
  usePageTitle('SSO Login');
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithSsoTokens, startSsoProvisioning } = useAuth();

  useEffect(() => {
    const state = searchParams.get('state');

    if (state === 'authenticated') {
      // Returning provisioned user — store tokens and go to dashboard
      const accessToken  = searchParams.get('accessToken');
      const refreshToken = searchParams.get('refreshToken');
      const username     = searchParams.get('username');
      const role         = searchParams.get('role');

      if (accessToken && username && role) {
        const user = loginWithSsoTokens({ accessToken, refreshToken, username, role });
        navigate(role === 'PATIENT' ? '/dashboard' : '/iam-dashboard', { replace: true });
      } else {
        navigate('/login?error=sso_failed', { replace: true });
      }

    } else if (state === 'provisioning') {
      // New user — store provisioning token and go to onboarding
      const token       = searchParams.get('token');
      const displayName = searchParams.get('displayName') || '';

      if (token) {
        startSsoProvisioning(token);
        navigate(`/sso-onboarding?displayName=${encodeURIComponent(displayName)}`, { replace: true });
      } else {
        navigate('/login?error=sso_failed', { replace: true });
      }

    } else {
      // Unknown state — fall back to login
      navigate('/login?error=sso_failed', { replace: true });
    }
  }, [searchParams, loginWithSsoTokens, startSsoProvisioning, navigate]);

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
        background: 'linear-gradient(135deg, rgba(23,105,210,0.10), rgba(22,131,74,0.08)), #f6f8fb',
      }}
    >
      <CircularProgress size={48} />
      <Typography variant="h6" color="text.secondary">
        Completing SSO login…
      </Typography>
      <Typography variant="body2" color="text.disabled">
        You will be redirected automatically.
      </Typography>
    </Box>
  );
}
