import { Navigate, Route, Routes } from 'react-router-dom';
import AppShell from './layout/AppShell';
import DashboardPage from './pages/DashboardPage';
import SettingsPage from './pages/SettingsPage';
import RemoveBackgroundPage from './pages/tools/RemoveBackgroundPage';
import OgImagePage from './pages/tools/OgImagePage';

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/tools/remove-background" element={<RemoveBackgroundPage />} />
        <Route path="/tools/og-image" element={<OgImagePage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  );
}
