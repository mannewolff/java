import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    // Petrol/Teal — Brand-Grundfarbe aus dem Mock-Image.
    // Hauptverwendung: AppBar (Header), Drawer-Active-State, primaerer Button.
    primary: {
      main: '#3d8a98',
      light: '#6cb4c3',
      dark: '#256270',
      contrastText: '#ffffff',
    },
    background: { default: '#f7f8fa' },
  },
  shape: { borderRadius: 8 },
});
