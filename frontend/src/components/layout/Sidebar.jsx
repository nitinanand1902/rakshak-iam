import {
  Box,
  Divider,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import LogoutIcon from '@mui/icons-material/Logout';
import SecurityOutlinedIcon from '@mui/icons-material/SecurityOutlined';
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';

export const drawerWidth = 280;

const navItems = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlinedIcon /> },
  { label: 'IAM Security', path: '/iam-dashboard', icon: <SecurityOutlinedIcon />, roles: ['DOCTOR', 'TECHNICIAN'] },
  { label: 'Audit Logs', path: '/audit-logs', icon: <AssessmentOutlinedIcon />, roles: ['DOCTOR'] },
];

export default function Sidebar({ mobileOpen, onClose, variant }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const content = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ gap: 1.5 }}>
        <ShieldOutlinedIcon color="primary" />
        <Box>
          <Typography variant="subtitle1" fontWeight={800}>
            Rakshak IAM
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Security Console
          </Typography>
        </Box>
      </Toolbar>
      <Divider />
      <List sx={{ px: 1.5, py: 2, flexGrow: 1 }}>
        {navItems
          .filter((item) => !item.roles || item.roles.includes(user?.role))
          .map((item) => (
            <ListItemButton
              key={item.path}
              component={NavLink}
              to={item.path}
              onClick={onClose}
              sx={{
                borderRadius: 2,
                mb: 0.5,
                '&.active': {
                  bgcolor: 'primary.light',
                  color: 'primary.dark',
                  '& .MuiListItemIcon-root': { color: 'primary.dark' },
                },
              }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} primaryTypographyProps={{ fontWeight: 700 }} />
            </ListItemButton>
          ))}
      </List>
      <Divider />
      <List sx={{ px: 1.5, py: 2 }}>
        <ListItemButton
          onClick={() => {
            logout();
            navigate('/login', { replace: true });
          }}
          sx={{ borderRadius: 2 }}
        >
          <ListItemIcon>
            <LogoutIcon />
          </ListItemIcon>
          <ListItemText primary="Logout" primaryTypographyProps={{ fontWeight: 700 }} />
        </ListItemButton>
      </List>
    </Box>
  );

  return (
    <Drawer
      variant={variant}
      open={variant === 'permanent' ? true : mobileOpen}
      onClose={onClose}
      ModalProps={{ keepMounted: true }}
      sx={{
        width: { lg: drawerWidth },
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          borderRight: '1px solid',
          borderColor: 'divider',
        },
      }}
    >
      {content}
    </Drawer>
  );
}
