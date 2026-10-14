import {
  AppBar,
  Avatar,
  Badge,
  Box,
  Button,
  IconButton,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuIcon from '@mui/icons-material/Menu';
import NotificationsOutlinedIcon from '@mui/icons-material/NotificationsOutlined';
import SecurityIcon from '@mui/icons-material/Security';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';
import { drawerWidth } from './Sidebar.jsx';

export default function Navbar({ onMenuClick }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <AppBar
      position="fixed"
      color="inherit"
      elevation={0}
      sx={{
        borderBottom: '1px solid',
        borderColor: 'divider',
        width: { lg: `calc(100% - ${drawerWidth}px)` },
        ml: { lg: `${drawerWidth}px` },
      }}
    >
      <Toolbar sx={{ gap: 2 }}>
        <IconButton edge="start" onClick={onMenuClick} sx={{ display: { lg: 'none' } }}>
          <MenuIcon />
        </IconButton>
        <SecurityIcon color="primary" />
        <Box sx={{ minWidth: 0, flexGrow: 1 }}>
          <Typography variant="h6" noWrap>
            Rakshak IAM Security Platform
          </Typography>
          <Typography variant="caption" color="text.secondary" noWrap sx={{ display: { xs: 'none', sm: 'block' } }}>
            Secure Identity & Access Management Demonstration
          </Typography>
        </Box>
        <IconButton aria-label="notifications">
          <Badge color="error" badgeContent={3}>
            <NotificationsOutlinedIcon />
          </Badge>
        </IconButton>
        <Button
          color="inherit"
          onClick={(event) => setAnchorEl(event.currentTarget)}
          startIcon={<Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.main', fontSize: 13 }}>{(user?.name || user?.username)?.[0]}</Avatar>}
          sx={{ display: { xs: 'none', md: 'inline-flex' } }}
        >
          {user?.name || user?.username} · {user?.role}
        </Button>
        <IconButton onClick={handleLogout} color="primary" aria-label="logout">
          <LogoutIcon />
        </IconButton>
        <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
          <MenuItem disabled>{user?.username}</MenuItem>
          <MenuItem disabled>{user?.role}</MenuItem>
          <MenuItem onClick={handleLogout}>Logout</MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
}
