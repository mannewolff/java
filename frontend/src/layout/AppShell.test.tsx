import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AppShell from './AppShell';
import { EditModeProvider } from '../pages/dashboard/EditModeContext';
import { KioskModeProvider } from '../pages/dashboard/KioskModeContext';

// AppShell ruft useAuth() fuer User-Info im Header auf. Im Slice-Test gibt es
// keinen AuthProvider, also wird der Hook gemockt.
vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({
    isLoading: false,
    isAuthenticated: true,
    username: 'alice',
    email: 'alice@example.com',
    initial: 'A',
    error: undefined,
    signIn: vi.fn(),
    signOut: vi.fn(),
  }),
}));

// AppShell lädt beim Mount die App-Version für den Header — im Slice-Test gemockt.
vi.mock('../api/appVersion', () => ({
  getAppVersion: vi.fn(),
}));
import { getAppVersion } from '../api/appVersion';
const mockGetAppVersion = getAppVersion as ReturnType<typeof vi.fn>;

beforeEach(() => {
  mockGetAppVersion.mockReset();
  mockGetAppVersion.mockResolvedValue({ major: 0, minor: 1 });
  installMemoryLocalStorage();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

// Node 26 / jsdom stellt window.localStorage nicht zuverlässig bereit — ein Memory-Stub macht
// die Tests deterministisch (AppShell persistiert den collapsed-Zustand in localStorage).
function installMemoryLocalStorage(): void {
  const store = new Map<string, string>();
  vi.stubGlobal('localStorage', {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => void store.set(k, String(v)),
    removeItem: (k: string) => void store.delete(k),
    clear: () => store.clear(),
    key: (i: number) => [...store.keys()][i] ?? null,
    get length() {
      return store.size;
    },
  });
}

function renderShell(initialEntry = '/dashboards/default') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <EditModeProvider>
        <KioskModeProvider>
          <Routes>
            <Route element={<AppShell />}>
              <Route path="/dashboards/default" element={<div>Dashboard-Inhalt</div>} />
              <Route path="/settings" element={<div>Settings-Inhalt</div>} />
              <Route path="/tools/svg-to-png" element={<div>SVG to PNG</div>} />
              <Route path="/tools/color-picker" element={<div>Color Picker</div>} />
              <Route path="/tools/password" element={<div>Password</div>} />
              <Route path="/kanban/board" element={<div>Kanban-Board-Inhalt</div>} />
              <Route path="/kanban/list" element={<div>Kanban-Listen-Inhalt</div>} />
              <Route path="/kanban/epics" element={<div>Kanban-Epics-Inhalt</div>} />
            </Route>
          </Routes>
        </KioskModeProvider>
      </EditModeProvider>
    </MemoryRouter>,
  );
}

describe('AppShell navigation', () => {
  beforeEach(() => {
    installMemoryLocalStorage();
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('renders the two top-level entries without expanding any group', () => {
    // given a fresh shell on /dashboard
    renderShell('/dashboards/default');

    // then Dashboard and Einstellungen are visible immediately
    expect(screen.getByText('Home')).toBeInTheDocument();
    expect(screen.getByText('Einstellungen')).toBeInTheDocument();

    // and the group headers are visible too
    expect(screen.getByText('Bildverarbeitung')).toBeInTheDocument();
    expect(screen.getByText('Nützliche Tools')).toBeInTheDocument();

    // and the children of the collapsed groups stay hidden
    expect(screen.queryByText('SVG zu PNG')).not.toBeInTheDocument();
    expect(screen.queryByText('Passwortgenerator')).not.toBeInTheDocument();
  });

  it('Kanban ist eine aufklappbare Gruppe; Board/Liste/Epics navigieren (#328)', async () => {
    // Auf /kanban/board ist die Kanban-Gruppe offen und alle drei Kinder sichtbar.
    renderShell('/kanban/board');
    expect(screen.getByText('Kanban')).toBeInTheDocument();
    expect(screen.getByText('Board')).toBeInTheDocument();
    expect(screen.getByText('Liste')).toBeInTheDocument();
    expect(screen.getByText('Epics')).toBeInTheDocument();
    expect(screen.getByText('Kanban-Board-Inhalt')).toBeInTheDocument();

    // Klick auf "Liste" navigiert zu /kanban/list.
    const user = userEvent.setup();
    await user.click(screen.getByText('Liste'));
    expect(screen.getByText('Kanban-Listen-Inhalt')).toBeInTheDocument();
  });

  it('auto-opens the group that contains the active route', () => {
    // given the app starts on /tools/svg-to-png
    renderShell('/tools/svg-to-png');

    // then the Bildverarbeitung group is expanded and both children are visible
    expect(screen.getByText('Bildverarbeitung')).toBeInTheDocument();
    expect(screen.getByText('SVG zu PNG')).toBeInTheDocument();
    expect(screen.getByText('Farbpipette')).toBeInTheDocument();

    // and the other group stays collapsed
    expect(screen.queryByText('Passwortgenerator')).not.toBeInTheDocument();
  });

  it('expands a group when its header is clicked and keeps the URL unchanged', async () => {
    // given a fresh shell on /dashboard with no group expanded
    renderShell('/dashboards/default');
    const user = userEvent.setup();
    expect(screen.queryByText('Passwortgenerator')).not.toBeInTheDocument();

    // when the user clicks the group header
    await user.click(screen.getByText('Nützliche Tools'));

    // then the Passwortgenerator link is now visible
    expect(screen.getByText('Passwortgenerator')).toBeInTheDocument();

    // and the route did not change (Dashboard content stays mounted)
    expect(screen.getByText('Dashboard-Inhalt')).toBeInTheDocument();
  });

  it('collapses an already open group on a second header click', async () => {
    // given a shell on /tools/svg-to-png (Bildverarbeitung auto-expanded)
    renderShell('/tools/svg-to-png');
    const user = userEvent.setup();
    expect(screen.getByText('SVG zu PNG')).toBeInTheDocument();

    // when the user clicks the header again
    await user.click(screen.getByText('Bildverarbeitung'));

    // then the children disappear
    expect(screen.queryByText('SVG zu PNG')).not.toBeInTheDocument();
  });

  it('links the Passwortgenerator entry to /tools/password even though the page does not exist yet', async () => {
    // given the shell with the Nützliche Tools group expanded
    renderShell('/dashboards/default');
    const user = userEvent.setup();
    await user.click(screen.getByText('Nützliche Tools'));

    // when the user clicks the Passwortgenerator entry
    await user.click(screen.getByText('Passwortgenerator'));

    // then the route resolves to /tools/password (page is a placeholder until #34)
    expect(screen.getByText('Password')).toBeInTheDocument();
  });

  it('marks the active child link as aria-selected', () => {
    // given the shell starts on /tools/svg-to-png
    renderShell('/tools/svg-to-png');

    // then the SVG zu PNG entry has aria-selected=true
    const link = screen.getByText('SVG zu PNG').closest('[role="button"]');
    expect(link).not.toBeNull();
    expect(link?.getAttribute('aria-selected')).toBe('true');

    // and an inactive entry does not
    const sibling = screen.getByText('Home').closest('[role="button"]');
    expect(sibling?.getAttribute('aria-selected')).toBe('false');
  });

  it('keeps Home outside any group at the top level', () => {
    renderShell('/dashboards/default');
    // Home ist kein Kind einer Gruppe; sein Eintrag steht direkt im Drawer.
    const list = screen.getByText('Home').closest('ul');
    expect(list).not.toBeNull();
    // Within the list there is also Einstellungen at the top level.
    expect(within(list as HTMLElement).getByText('Einstellungen')).toBeInTheDocument();
  });
});

describe('AppShell collapsed sidebar', () => {
  beforeEach(() => {
    installMemoryLocalStorage();
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('shows labels when not collapsed', () => {
    renderShell('/dashboards/default');
    expect(screen.getByText('Home')).toBeInTheDocument();
    expect(screen.getByText('Einstellungen')).toBeInTheDocument();
  });

  it('hides labels and shows only icons when collapsed via toggle', async () => {
    renderShell('/dashboards/default');
    const user = userEvent.setup();

    // Sidebar einklappen
    const toggleBtn = screen.getByRole('button', { name: 'Menü einklappen' });
    await user.click(toggleBtn);

    // Labels nicht mehr sichtbar
    expect(screen.queryByText('Home')).not.toBeInTheDocument();
    expect(screen.queryByText('Einstellungen')).not.toBeInTheDocument();

    // Toggle-Button zeigt jetzt "ausklappen"
    expect(screen.getByRole('button', { name: 'Menü ausklappen' })).toBeInTheDocument();
  });

  it('persists collapsed state to localStorage', async () => {
    renderShell('/dashboards/default');
    const user = userEvent.setup();

    expect(localStorage.getItem('sidebar-collapsed')).toBeNull();

    await user.click(screen.getByRole('button', { name: 'Menü einklappen' }));
    expect(localStorage.getItem('sidebar-collapsed')).toBe('true');

    await user.click(screen.getByRole('button', { name: 'Menü ausklappen' }));
    expect(localStorage.getItem('sidebar-collapsed')).toBe('false');
  });

  it('restores collapsed state from localStorage on mount', () => {
    localStorage.setItem('sidebar-collapsed', 'true');
    renderShell('/dashboards/default');

    // Im collapsed Zustand sind Labels nicht sichtbar
    expect(screen.queryByText('Home')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Menü ausklappen' })).toBeInTheDocument();
  });

  it('shows icon aria-labels as tooltips in collapsed mode', async () => {
    localStorage.setItem('sidebar-collapsed', 'true');
    renderShell('/dashboards/default');

    // Icon-Buttons haben aria-label mit dem Menüpunkt-Namen
    expect(screen.getByRole('button', { name: 'Home' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Einstellungen' })).toBeInTheDocument();
  });
});

describe('AppShell App-Version (#163)', () => {
  beforeEach(() => {
    localStorage.clear();
  });
  afterEach(() => {
    cleanup();
    localStorage.clear();
  });

  it('zeigt die geladene Version im Header-Titel', async () => {
    mockGetAppVersion.mockResolvedValue({ major: 0, minor: 1 });
    renderShell();
    expect(await screen.findByText('mannewolff-tools v0.1')).toBeInTheDocument();
  });

  it('ruft die Version-API genau einmal beim Mount auf', async () => {
    mockGetAppVersion.mockResolvedValue({ major: 1, minor: 4 });
    renderShell();
    await screen.findByText('mannewolff-tools v1.4');
    expect(mockGetAppVersion).toHaveBeenCalledTimes(1);
  });

  it('zeigt nur den Titel ohne Version bei API-Fehler (graceful)', async () => {
    mockGetAppVersion.mockRejectedValue(new Error('boom'));
    renderShell();
    expect(await screen.findByText('mannewolff-tools')).toBeInTheDocument();
    expect(screen.queryByText(/v\d/)).not.toBeInTheDocument();
  });
});
