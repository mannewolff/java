import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import AppShell from './layout/AppShell';
import { EditModeProvider } from './pages/dashboard/EditModeContext';
import { KioskModeProvider } from './pages/dashboard/KioskModeContext';
import { NotifyProvider } from './notify/NotifyProvider';
import { ProtectedRoute } from './auth/ProtectedRoute';
import PageLoader from './layout/PageLoader';

const DashboardListPage = lazy(() => import('./pages/dashboard/DashboardListPage'));
const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));
const DashboardDefaultRedirect = lazy(
  () => import('./pages/dashboard/DashboardDefaultRedirect'),
);
const SettingsPage = lazy(() => import('./pages/SettingsPage'));
const RemoveBackgroundPage = lazy(() => import('./pages/tools/RemoveBackgroundPage'));
const OgImagePage = lazy(() => import('./pages/tools/OgImagePage'));
const ResizePage = lazy(() => import('./pages/tools/ResizePage'));
const SvgToPngPage = lazy(() => import('./pages/tools/SvgToPngPage'));
const ColorPickerPage = lazy(() => import('./pages/tools/ColorPickerPage'));
const PasswordPage = lazy(() => import('./pages/tools/PasswordPage'));
const KanbanPage = lazy(() => import('./pages/kanban/KanbanPage'));
const MobilePage = lazy(() => import('./pages/mobile/MobilePage'));
const TimeSeriesListPage = lazy(() => import('./pages/timeseries/TimeSeriesListPage'));
const TimeSeriesDetailPage = lazy(
  () => import('./pages/timeseries/TimeSeriesDetailPage'),
);
const IngestTokenSettingsPage = lazy(
  () => import('./pages/settings/IngestTokenSettingsPage'),
);

export default function App() {
  return (
    <Routes>
      <Route
        element={
          <ProtectedRoute>
            {/* EditModeProvider wrappt AppShell, damit Sidebar und DashboardPage
                denselben State sehen (Sidebar wechselt auf Widget-Palette im Edit-Modus). */}
            <EditModeProvider>
              <KioskModeProvider>
                <NotifyProvider>
                  <AppShell />
                </NotifyProvider>
              </KioskModeProvider>
            </EditModeProvider>
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboards/default" replace />} />
        <Route
          path="/dashboards"
          element={
            <Suspense fallback={<PageLoader />}>
              <DashboardListPage />
            </Suspense>
          }
        />
        <Route
          path="/dashboards/default"
          element={
            <Suspense fallback={<PageLoader />}>
              <DashboardDefaultRedirect />
            </Suspense>
          }
        />
        <Route
          path="/dashboards/:id"
          element={
            <Suspense fallback={<PageLoader />}>
              <DashboardPage />
            </Suspense>
          }
        />
        {/* Backward-Compat: alter /dashboard-Bookmark landet beim Default. */}
        <Route path="/dashboard" element={<Navigate to="/dashboards/default" replace />} />
        <Route
          path="/tools/remove-background"
          element={
            <Suspense fallback={<PageLoader />}>
              <RemoveBackgroundPage />
            </Suspense>
          }
        />
        <Route
          path="/tools/og-image"
          element={
            <Suspense fallback={<PageLoader />}>
              <OgImagePage />
            </Suspense>
          }
        />
        <Route
          path="/tools/resize"
          element={
            <Suspense fallback={<PageLoader />}>
              <ResizePage />
            </Suspense>
          }
        />
        <Route
          path="/tools/svg-to-png"
          element={
            <Suspense fallback={<PageLoader />}>
              <SvgToPngPage />
            </Suspense>
          }
        />
        <Route
          path="/tools/color-picker"
          element={
            <Suspense fallback={<PageLoader />}>
              <ColorPickerPage />
            </Suspense>
          }
        />
        <Route
          path="/tools/password"
          element={
            <Suspense fallback={<PageLoader />}>
              <PasswordPage />
            </Suspense>
          }
        />
        <Route
          path="/kanban"
          element={
            <Suspense fallback={<PageLoader />}>
              <KanbanPage />
            </Suspense>
          }
        />
        <Route
          path="/mobile"
          element={
            <Suspense fallback={<PageLoader />}>
              <MobilePage />
            </Suspense>
          }
        />
        <Route
          path="/timeseries"
          element={
            <Suspense fallback={<PageLoader />}>
              <TimeSeriesListPage />
            </Suspense>
          }
        />
        <Route
          path="/timeseries/:id"
          element={
            <Suspense fallback={<PageLoader />}>
              <TimeSeriesDetailPage />
            </Suspense>
          }
        />
        <Route
          path="/settings"
          element={
            <Suspense fallback={<PageLoader />}>
              <SettingsPage />
            </Suspense>
          }
        />
        <Route
          path="/settings/tokens"
          element={
            <Suspense fallback={<PageLoader />}>
              <IngestTokenSettingsPage />
            </Suspense>
          }
        />
      </Route>
    </Routes>
  );
}
