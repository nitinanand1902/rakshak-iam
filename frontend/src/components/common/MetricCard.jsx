import { Box, Card, CardContent, Typography } from '@mui/material';

export default function MetricCard({ title, value, helper, icon, color = 'primary.main' }) {
  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
          <Box>
            <Typography variant="body2" color="text.secondary" fontWeight={700}>
              {title}
            </Typography>
            <Typography variant="h4" sx={{ mt: 1, color }}>
              {value}
            </Typography>
            {helper && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {helper}
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2,
              display: 'grid',
              placeItems: 'center',
              bgcolor: 'primary.light',
              color,
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}
