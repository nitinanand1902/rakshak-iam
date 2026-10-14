import { Box, Toolbar, useMediaQuery } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { Outlet } from 'react-router-dom';
import { useState } from 'react';
import Navbar from './Navbar.jsx';
import Sidebar, { drawerWidth } from './Sidebar.jsx';

export default function DashboardLayout() {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('lg'));
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <Navbar onMenuClick={() => setMobileOpen(true)} />
      <Sidebar
        mobileOpen={mobileOpen}
        onClose={() => setMobileOpen(false)}
        variant={isDesktop ? 'permanent' : 'temporary'}
      />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { lg: `calc(100% - ${drawerWidth}px)` },
          px: { xs: 2, md: 3 },
          pb: 4,
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
