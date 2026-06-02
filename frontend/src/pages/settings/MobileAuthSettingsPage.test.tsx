import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import MobileAuthSettingsPage from './MobileAuthSettingsPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

function renderPage(): ReturnType<typeof render> {
  return render(
    <MemoryRouter>
      <NotifyProvider>
        <MobileAuthSettingsPage />
      </NotifyProvider>
    </MemoryRouter>,
  );
}

const expectedUrl = `${window.location.origin}/mobile?pair=1`;

describe('MobileAuthSettingsPage', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('zeigt Überschrift, QR-Code und den Pairing-Link', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: 'Handy-Zugang' })).toBeInTheDocument();
    expect(screen.getByLabelText('QR-Code für den Handy-Zugang')).toBeInTheDocument();
    expect(screen.getByText(expectedUrl)).toBeInTheDocument();
  });

  it('verlinkt auf die Keycloak-Konto-Verwaltung zum Widerruf', () => {
    renderPage();
    const link = screen.getByRole('link', { name: /Konto-Verwaltung/ });
    expect(link).toHaveAttribute('href', expect.stringContaining('/account'));
  });

  it('kopiert den Link in die Zwischenablage', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: 'Link kopieren' }));
    expect(writeText).toHaveBeenCalledWith(expectedUrl);
    expect(await screen.findByText('Link kopiert')).toBeInTheDocument();
  });
});
