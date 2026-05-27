import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RemoveBackgroundPage from './RemoveBackgroundPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

function renderRb(): ReturnType<typeof render> {
  return render(
    <NotifyProvider>
      <RemoveBackgroundPage />
    </NotifyProvider>,
  );
}

const PNG_BYTES = new Uint8Array([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
]);

function makePngFile(name = 'icon.png', size = PNG_BYTES.byteLength): File {
  if (size === PNG_BYTES.byteLength) {
    return new File([PNG_BYTES], name, { type: 'image/png' });
  }
  return new File([new Uint8Array(size)], name, { type: 'image/png' });
}

describe('RemoveBackgroundPage', () => {
  beforeEach(() => {
    vi.spyOn(globalThis, 'fetch').mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the empty drop zone initially', () => {
    renderRb();
    expect(screen.getByText(/Hintergrund entfernen/i)).toBeInTheDocument();
    expect(screen.getByText(/Bild hier ablegen/i)).toBeInTheDocument();
    expect(screen.queryByRole('img', { name: /Original-Bild/i })).not.toBeInTheDocument();
  });

  it('shows the before-preview once a file is selected', async () => {
    renderRb();
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;

    await userEvent.upload(input, makePngFile());

    expect(screen.getByRole('img', { name: /Original-Bild/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Hintergrund entfernen$/i })).toBeEnabled();
  });

  it('rejects unsupported content types', async () => {
    renderRb();
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    const txt = new File(['hello'], 'notes.txt', { type: 'text/plain' });

    await userEvent.upload(input, txt, { applyAccept: false });

    expect(await screen.findByRole('alert')).toHaveTextContent(/Format nicht unterstützt/i);
  });

  it('rejects files larger than 10 MB', async () => {
    renderRb();
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    const big = makePngFile('huge.png', 11 * 1024 * 1024);

    await userEvent.upload(input, big);

    expect(await screen.findByRole('alert')).toHaveTextContent(/Datei zu groß/i);
  });

  it('processes the image and shows after-preview + download link', async () => {
    const processed = new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' });
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(processed, { status: 200, headers: { 'Content-Type': 'image/png' } }),
    );
    renderRb();
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    await userEvent.upload(input, makePngFile());

    await userEvent.click(screen.getByRole('button', { name: /Hintergrund entfernen$/i }));

    await waitFor(() =>
      expect(
        screen.getByRole('img', { name: /Bild mit transparentem Hintergrund/i }),
      ).toBeInTheDocument(),
    );
    const download = screen.getByRole('link', { name: /PNG herunterladen/i });
    expect(download).toHaveAttribute('download', 'icon-transparent.png');
  });

  it('shows backend error message on failure', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ status: 502, message: 'upstream down' }), {
        status: 502,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    renderRb();
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    await userEvent.upload(input, makePngFile());

    await userEvent.click(screen.getByRole('button', { name: /Hintergrund entfernen$/i }));

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('upstream down'),
    );
  });

  it('resets state when the reset button is clicked', async () => {
    renderRb();
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    await userEvent.upload(input, makePngFile());
    expect(screen.getByRole('img', { name: /Original-Bild/i })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Zurücksetzen/i }));

    expect(screen.queryByRole('img', { name: /Original-Bild/i })).not.toBeInTheDocument();
  });
});
