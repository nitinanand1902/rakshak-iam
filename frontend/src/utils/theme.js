import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1769d2',
      dark: '#0f4f9e',
      light: '#e8f1ff',
    },
    success: {
      main: '#16834a',
      light: '#e9f7ef',
    },
    error: {
      main: '#d92d20',
      light: '#fdecec',
    },
    warning: {
      main: '#b7791f',
      light: '#fff6df',
    },
    background: {
      default: '#f6f8fb',
      paper: '#ffffff',
    },
    text: {
      primary: '#172033',
      secondary: '#5c667a',
    },
    divider: '#e5eaf2',
  },
  typography: {
    fontFamily: 'Inter, Arial, sans-serif',
    h4: { fontWeight: 700 },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 700 },
    button: { textTransform: 'none', fontWeight: 700 },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid #e5eaf2',
          boxShadow: '0 10px 30px rgba(23, 32, 51, 0.06)',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 700,
        },
      },
    },
  },
});

export default theme;
