import { Box, Card, CardContent, TextField, Typography } from '@mui/material';
import Grid from '@mui/material/Grid2';
import { DataGrid } from '@mui/x-data-grid';
import BlockIcon from '@mui/icons-material/Block';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import EventNoteIcon from '@mui/icons-material/EventNote';
import SecurityIcon from '@mui/icons-material/Security';
import { useEffect, useMemo, useState } from 'react';
import MetricCard from '../components/common/MetricCard.jsx';
import StatusChip from '../components/common/StatusChip.jsx';
import { usePageTitle } from '../hooks/usePageTitle.js';
import { fetchAuditLogs } from '../services/backendApi.js';

const columns = [
  { field: 'timestamp', headerName: 'Timestamp', minWidth: 180, flex: 1 },
  { field: 'user', headerName: 'User', minWidth: 120, flex: 0.7 },
  { field: 'role', headerName: 'Role', minWidth: 130, flex: 0.7 },
  { field: 'action', headerName: 'Action', minWidth: 190, flex: 1 },
  { field: 'endpoint', headerName: 'Endpoint', minWidth: 190, flex: 1 },
  {
    field: 'status',
    headerName: 'Status',
    minWidth: 130,
    flex: 0.7,
    renderCell: (params) => <StatusChip label={params.value} />,
  },
  { field: 'ipAddress', headerName: 'IP Address', minWidth: 140, flex: 0.8 },
  { field: 'eventType', headerName: 'Event Type', minWidth: 180, flex: 1 },
];

export default function AuditLogs() {
  usePageTitle('Audit Logs');
  const [query, setQuery] = useState('');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetchAuditLogs({ page: 0, pageSize: 50 })
      .then((response) => setRows(response.rows))
      .finally(() => setLoading(false));
  }, []);

  const filteredRows = useMemo(() => {
    const lower = query.toLowerCase();
    return rows.filter((row) => Object.values(row).join(' ').toLowerCase().includes(lower));
  }, [query, rows]);

  const securityEvents = rows.filter((row) => ['ACCESS_DENIED', 'SUSPICIOUS_ACTIVITY', 'LOGIN_FAILED', 'ACCOUNT_LOCKED'].includes(row.eventType)).length;
  const failedLogins = rows.filter((row) => row.eventType === 'LOGIN_FAILED').length;
  const blockedAttacks = rows.filter((row) => row.status === 'BLOCKED').length;

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 0.5 }}>
        Audit Logs
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Searchable enterprise audit trail for identity, access, and security events.
      </Typography>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard title="Total Events" value={String(rows.length)} helper="Live backend data" icon={<EventNoteIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard title="Security Events" value={String(securityEvents)} helper="Requires review" icon={<SecurityIcon />} color="warning.main" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard title="Failed Logins" value={String(failedLogins)} helper="Credential risk" icon={<ErrorOutlineIcon />} color="error.main" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <MetricCard title="Blocked Attacks" value={String(blockedAttacks)} helper="Controls enforced" icon={<BlockIcon />} color="success.main" />
        </Grid>
      </Grid>

      <Card>
        <CardContent>
          <TextField
            label="Search audit logs"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            fullWidth
            sx={{ mb: 2 }}
          />
          <Box sx={{ height: 560, width: '100%' }}>
            <DataGrid
              rows={filteredRows}
              columns={columns}
              loading={loading}
              initialState={{
                pagination: { paginationModel: { pageSize: 8 } },
                sorting: { sortModel: [{ field: 'timestamp', sort: 'desc' }] },
              }}
              pageSizeOptions={[5, 8, 10]}
              disableRowSelectionOnClick
            />
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
