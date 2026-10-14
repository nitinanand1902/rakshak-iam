import { Chip } from '@mui/material';

const colorByStatus = {
  SUCCESS: 'success',
  ENABLED: 'success',
  BLOCKED: 'error',
  FAILED: 'error',
  DETECTED: 'warning',
  LOW: 'success',
};

export default function StatusChip({ label, color, variant = 'filled' }) {
  const normalized = String(label).toUpperCase();
  return (
    <Chip
      size="small"
      color={color || colorByStatus[normalized] || 'default'}
      label={label}
      variant={variant}
      sx={{ borderRadius: 1.5 }}
    />
  );
}
