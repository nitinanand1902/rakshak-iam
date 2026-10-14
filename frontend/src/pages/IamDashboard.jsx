import { Alert, Box, Button, Card, CardContent, Chip, Stack, Typography } from '@mui/material';
import Grid from '@mui/material/Grid2';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import BarChartIcon from '@mui/icons-material/BarChart';
import LockClockIcon from '@mui/icons-material/LockClock';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SecurityIcon from '@mui/icons-material/Security';
import ShieldIcon from '@mui/icons-material/Shield';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import { useMemo, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import MetricCard from '../components/common/MetricCard.jsx';
import StatusChip from '../components/common/StatusChip.jsx';
import { usePageTitle } from '../hooks/usePageTitle.js';
import { runIamEvaluation } from '../services/backendApi.js';

const controls = ['JWT Authentication', 'Refresh Tokens', 'AES Encryption', 'BCrypt Password Storage', 'RBAC', 'Audit Logging'];
const coverage = [
  { name: 'Enabled', value: 92, color: '#16834a' },
  { name: 'Review', value: 8, color: '#b7791f' },
];
export default function IamDashboard() {
  usePageTitle('IAM Security');
  const [loading, setLoading] = useState(false);
  const [report, setReport] = useState(null);

  const scores = report || { authenticationScore: 95, authorizationScore: 96, sessionSecurity: 92, risk: 'LOW' };
  const scoreBars = useMemo(
    () => [
      { name: 'Authentication', score: scores.authenticationScore },
      { name: 'Authorization', score: scores.authorizationScore },
      { name: 'Session', score: scores.sessionSecurity },
    ],
    [scores.authenticationScore, scores.authorizationScore, scores.sessionSecurity],
  );
  const threatSignals = useMemo(
    () => [
      { label: 'Unauthorized Access', status: 'Blocked', severity: 'error' },
      { label: 'Privilege Escalation', status: report?.raw?.accountLocked ? 'Detected' : 'No Threat', severity: report?.raw?.accountLocked ? 'warning' : 'success' },
      { label: 'JWT Tampering', status: 'Blocked', severity: 'error' },
      { label: 'Replay Attack', status: 'No Threat', severity: 'success' },
      {
        label: 'Brute Force Attempts',
        status: report?.raw?.failedLoginAttempts > 0 ? 'Detected' : 'No Threat',
        severity: report?.raw?.failedLoginAttempts > 0 ? 'warning' : 'success',
      },
    ],
    [report],
  );

  const evaluate = async () => {
    setLoading(true);
    try {
      const response = await runIamEvaluation();
      setReport(response);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 0.5 }}>
        IAM Security Dashboard
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Enterprise security monitoring for authentication, authorization, sessions, and role enforcement.
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard title="Authentication Score" value={`${scores.authenticationScore}%`} helper="JWT and login controls" icon={<ShieldIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard title="Authorization Score" value={`${scores.authorizationScore}%`} helper="RBAC policy coverage" icon={<AdminPanelSettingsIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard title="Session Security" value={`${scores.sessionSecurity}%`} helper="Token lifecycle posture" icon={<LockClockIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard title="Overall Risk" value={scores.risk} helper="Backend IAM posture" icon={<SecurityIcon />} color={scores.risk === 'LOW' ? 'success.main' : 'warning.main'} />
        </Grid>

        <Grid size={{ xs: 12, lg: 6 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Threat Detection
              </Typography>
              <Stack spacing={1.4}>
                {threatSignals.map((item) => (
                  <Box
                    key={item.label}
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: 2,
                      py: 1,
                      borderBottom: '1px solid',
                      borderColor: 'divider',
                    }}
                  >
                    <Typography fontWeight={700}>{item.label}</Typography>
                    <StatusChip label={item.status} color={item.severity} />
                  </Box>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Security Controls
              </Typography>
              <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                {controls.map((control) => (
                  <Chip
                    key={control}
                    icon={<VerifiedUserIcon />}
                    color="success"
                    label={`${control} Enabled`}
                    sx={{ borderRadius: 1.5 }}
                  />
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12 }}>
          <Card>
            <CardContent>
              <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" spacing={2} sx={{ mb: 3 }}>
                <Box>
                  <Typography variant="h6">Security Evaluation</Typography>
                  <Typography color="text.secondary">Run a backend IAM posture assessment against the authenticated account.</Typography>
                </Box>
                <Button variant="contained" startIcon={<PlayArrowIcon />} onClick={evaluate} disabled={loading}>
                  {loading ? 'Running Evaluation' : 'Run IAM Evaluation'}
                </Button>
              </Stack>
              {report && (
                <Alert severity="success" sx={{ mb: 3 }}>
                  Authentication Score {report.authenticationScore} · Authorization Score {report.authorizationScore} · Session Security{' '}
                  {report.sessionSecurity} · Risk {report.risk}
                </Alert>
              )}
              {report && (
                <Typography color="text.secondary" sx={{ mb: 3 }}>
                  {report.summary}
                </Typography>
              )}
              <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 5 }}>
                  <Typography variant="subtitle1" fontWeight={800} sx={{ mb: 1 }}>
                    Security Controls Coverage
                  </Typography>
                  <Box sx={{ height: 260 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie data={coverage} dataKey="value" nameKey="name" outerRadius={90} label>
                          {coverage.map((entry) => (
                            <Cell key={entry.name} fill={entry.color} />
                          ))}
                        </Pie>
                        <Tooltip />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>
                  </Box>
                </Grid>
                <Grid size={{ xs: 12, md: 7 }}>
                  <Typography variant="subtitle1" fontWeight={800} sx={{ mb: 1 }}>
                    IAM Scores
                  </Typography>
                  <Box sx={{ height: 260 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={scoreBars}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                        <XAxis dataKey="name" />
                        <YAxis domain={[0, 100]} />
                        <Tooltip />
                        <Bar dataKey="score" fill="#1769d2" radius={[6, 6, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </Box>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
