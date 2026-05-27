import { render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { NotifyProvider, useNotify } from './NotifyProvider';

describe('NotifyProvider', () => {
  it('throws when useNotify is called outside the provider', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => renderHook(() => useNotify())).toThrow(
      /useNotify must be used inside <NotifyProvider>/,
    );
    consoleError.mockRestore();
  });

  it('renders a success toast with filled-success styling', async () => {
    function Trigger() {
      const notify = useNotify();
      return (
        <button type="button" onClick={() => notify.success('alles ok')}>
          go
        </button>
      );
    }
    render(
      <NotifyProvider>
        <Trigger />
      </NotifyProvider>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'go' }));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('alles ok');
    expect(alert.className).toMatch(/MuiAlert-filledSuccess/);
  });

  it('renders an error toast with filled-error styling', async () => {
    function Trigger() {
      const notify = useNotify();
      return (
        <button type="button" onClick={() => notify.error('kaputt')}>
          go
        </button>
      );
    }
    render(
      <NotifyProvider>
        <Trigger />
      </NotifyProvider>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'go' }));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('kaputt');
    expect(alert.className).toMatch(/MuiAlert-filledError/);
  });

  it('auto-hides a success toast after ~3 s', async () => {
    function Trigger() {
      const notify = useNotify();
      return (
        <button type="button" onClick={() => notify.success('huch')}>
          go
        </button>
      );
    }
    render(
      <NotifyProvider>
        <Trigger />
      </NotifyProvider>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'go' }));
    await screen.findByRole('alert');

    // Auto-Hide = 3000 ms, Transition-Exit nochmal ein paar 100 ms. Wir warten
    // mit realen Timern (waitFor pollt) und einer großzügigen Obergrenze.
    await waitFor(
      () => expect(screen.queryByRole('alert')).not.toBeInTheDocument(),
      { timeout: 5000 },
    );
  }, 8000);

  it('queues a second toast and shows it after the first one closes', async () => {
    function Trigger() {
      const notify = useNotify();
      return (
        <button
          type="button"
          onClick={() => {
            notify.success('erste');
            notify.success('zweite');
          }}
        >
          go
        </button>
      );
    }
    render(
      <NotifyProvider>
        <Trigger />
      </NotifyProvider>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'go' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('erste'));

    // Der nächste Toast erscheint nach Auto-Hide + Exit-Transition.
    await waitFor(
      () => expect(screen.getByRole('alert')).toHaveTextContent('zweite'),
      { timeout: 6000 },
    );
  }, 8000);

  it('closes the toast when the close button is clicked', async () => {
    function Trigger() {
      const notify = useNotify();
      return (
        <button type="button" onClick={() => notify.error('manuell')}>
          go
        </button>
      );
    }
    render(
      <NotifyProvider>
        <Trigger />
      </NotifyProvider>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'go' }));
    await screen.findByRole('alert');

    const closeButton = screen.getByRole('button', { name: /close/i });
    await userEvent.click(closeButton);

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });
});
