import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { CssBaseline, ThemeProvider } from '@mui/material';
// react-grid-layout braucht zwei Stylesheets damit Drag/Resize-Handles und der
// Grid-Container korrekt aussehen. Globale Imports — die kleinen CSS-Files sind <5 KB.
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import App from './App';
import { theme } from './theme';
import { AuthProvider } from './auth/AuthProvider';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>,
);
