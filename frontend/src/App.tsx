import { Navigate, Route, Routes } from 'react-router-dom';
import AppShell from './layout/AppShell';
import DashboardListPage from './pages/dashboard/DashboardListPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import DashboardDefaultRedirect from './pages/dashboard/DashboardDefaultRedirect';
import { EditModeProvider } from './pages/dashboard/EditModeContext';
import { NotifyProvider } from './notify/NotifyProvider';
import SettingsPage from './pages/SettingsPage';
import RemoveBackgroundPage from './pages/tools/RemoveBackgroundPage';
import OgImagePage from './pages/tools/OgImagePage';
import ResizePage from './pages/tools/ResizePage';
import SvgToPngPage from './pages/tools/SvgToPngPage';
import PasswordPage from './pages/tools/PasswordPage';
import KanbanPage from './pages/kanban/KanbanPage';
import TimeSeriesListPage from './pages/timeseries/TimeSeriesListPage';
import TimeSeriesDetailPage from './pages/timeseries/TimeSeriesDetailPage';
import IngestTokenSettingsPage from './pages/settings/IngestTokenSettingsPage';
import { ProtectedRoute } from './auth/ProtectedRoute';

export default function App() {
  return (
    <Routes>
      <Route
        element={
          <ProtectedRoute>
            {/* EditModeProvider wrappt AppShell, damit Sidebar und DashboardPage
                denselben State sehen (Sidebar wechselt auf Widget-Palette im Edit-Modus). */}
            <EditModeProvider>
              <NotifyProvider>
                <AppShell />
              </NotifyProvider>
            </EditModeProvider>
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboards/default" replace />} />
        <Route path="/dashboards" element={<DashboardListPage />} />
        <Route path="/dashboards/default" element={<DashboardDefaultRedirect />} />
        <Route path="/dashboards/:id" element={<DashboardPage />} />
        {/* Backward-Compat: alter /dashboard-Bookmark landet beim Default. */}
        <Route path="/dashboard" element={<Navigate to="/dashboards/default" replace />} />
        <Route path="/tools/remove-background" element={<RemoveBackgroundPage />} />
        <Route path="/tools/og-image" element={<OgImagePage />} />
        <Route path="/tools/resize" element={<ResizePage />} />
        <Route path="/tools/svg-to-png" element={<SvgToPngPage />} />
        <Route path="/tools/password" element={<PasswordPage />} />
        <Route path="/kanban" element={<KanbanPage />} />
        <Route path="/timeseries" element={<TimeSeriesListPage />} />
        <Route path="/timeseries/:id" element={<TimeSeriesDetailPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/settings/tokens" element={<IngestTokenSettingsPage />} />
      </Route>
    </Routes>
  );
}
