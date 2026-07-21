import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import ApiConsolePage from './ApiConsolePage';
import { NotifyProvider } from '../../notify/NotifyProvider';
import { useAuth } from '../../auth/useAuth';

vi.mock('../../auth/useAuth', () => ({
  useAuth: vi.fn(() => ({ accessToken: 'SESSION_TKN' })),
}));

const useAuthMock = vi.mocked(useAuth);

function renderPage(): ReturnType<typeof render> {
  return render(
    <NotifyProvider>
      <ApiConsolePage />
    </NotifyProvider>,
  );
}

function mockFetch(): ReturnType<typeof vi.fn> {
  const fn = vi.fn().mockResolvedValue(
    new Response(JSON.stringify({ ok: true }), {
      status: 200,
      statusText: 'OK',
      headers: { 'Content-Type': 'application/json' },
    }),
  );
  vi.stubGlobal('fetch', fn);
  return fn;
}

describe('ApiConsolePage', () => {
  beforeEach(() => {
    window.localStorage.clear();
    useAuthMock.mockReturnValue({ accessToken: 'SESSION_TKN' } as never);
  });
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('rendert Überschrift und Default-Pfad', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: 'API-Konsole' })).toBeInTheDocument();
    expect(screen.getByLabelText('Pfad')).toHaveValue('/api/timeseries');
  });

  it('sendet mit Bearer-Token an die eigene API und zeigt den Response', async () => {
    const fetchFn = mockFetch();
    renderPage();
    await userEvent.click(screen.getByRole('button', { name: 'Senden' }));

    await waitFor(() => expect(fetchFn).toHaveBeenCalled());
    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe(`${window.location.origin}/api/timeseries`);
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer SESSION_TKN');

    const responsePanel = await screen.findByLabelText('Response');
    expect(within(responsePanel).getByText('200 OK')).toBeInTheDocument();
    expect(within(responsePanel).getByText(/"ok": true/)).toBeInTheDocument();
  });

  it('blockt fremde Origins (Senden deaktiviert, Hinweis sichtbar)', async () => {
    renderPage();
    const pathField = screen.getByLabelText('Pfad');
    await userEvent.clear(pathField);
    await userEvent.type(pathField, 'https://evil.example/x');

    expect(screen.getByRole('button', { name: 'Senden' })).toBeDisabled();
    expect(
      screen.getByText('Nur eigene API (same-origin, z. B. /api/timeseries)'),
    ).toBeInTheDocument();
  });

  it('sendet im Ingest-Modus X-Ingest-Token statt Authorization', async () => {
    const fetchFn = mockFetch();
    renderPage();
    await userEvent.click(screen.getByRole('button', { name: 'Ingest-Token' }));
    await userEvent.type(screen.getByLabelText('Ingest-Token'), 'ING123');
    await userEvent.click(screen.getByRole('button', { name: 'Senden' }));

    await waitFor(() => expect(fetchFn).toHaveBeenCalled());
    const headers = fetchFn.mock.calls[0][1].headers as Record<string, string>;
    expect(headers['X-Ingest-Token']).toBe('ING123');
    expect(headers.Authorization).toBeUndefined();
  });

  it('speichert, lädt und löscht einen Request (localStorage, ohne Token)', async () => {
    renderPage();
    await userEvent.type(screen.getByLabelText('Als…'), 'Zeitreihen-Liste');
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }));

    expect(await screen.findByText('Request „Zeitreihen-Liste" gespeichert')).toBeInTheDocument();
    // Token darf nicht im localStorage landen.
    expect(window.localStorage.getItem('toolbox-api-console-requests')).not.toContain(
      'SESSION_TKN',
    );

    const savedEntry = screen.getByRole('button', {
      name: /Zeitreihen-Liste — \/api\/timeseries/,
    });
    expect(savedEntry).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Zeitreihen-Liste löschen' }));
    expect(
      screen.queryByRole('button', { name: /Zeitreihen-Liste — / }),
    ).not.toBeInTheDocument();
  });
});
