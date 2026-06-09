import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import RasterToPngPage from './RasterToPngPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

function renderPage(): ReturnType<typeof render> {
  return render(
    <NotifyProvider>
      <RasterToPngPage />
    </NotifyProvider>,
  );
}

function makeJpegFile(name = 'photo.jpg'): File {
  return new File([new Uint8Array([0xff, 0xd8, 0xff])], name, { type: 'image/jpeg' });
}

function makePngFile(name = 'image.png'): File {
  return new File([new Uint8Array([0x89, 0x50, 0x4e, 0x47])], name, { type: 'image/png' });
}

function pngResponse(): Response {
  return new Response(new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' }), {
    status: 200,
    headers: { 'Content-Type': 'image/png' },
  });
}

async function uploadFile(file: File): Promise<void> {
  const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
  await userEvent.upload(input, file);
}

describe('RasterToPngPage', () => {
  beforeEach(() => {
    vi.spyOn(globalThis, 'fetch').mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the empty drop zone initially', () => {
    renderPage();

    expect(screen.getByRole('heading', { name: /Raster zu PNG/i })).toBeInTheDocument();
    expect(screen.getByText(/JPEG oder PNG hier ablegen/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/Breite/)).not.toBeInTheDocument();
  });

  it('rejects SVG (unsupported format)', async () => {
    renderPage();

    const svgFile = new File(['<svg/>'], 'icon.svg', { type: 'image/svg+xml' });
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    await userEvent.upload(input, svgFile, { applyAccept: false });

    expect(await screen.findByRole('alert')).toHaveTextContent(/Format nicht unterstützt/);
    expect(screen.queryByRole('button', { name: /Konvertieren/i })).not.toBeInTheDocument();
  });

  it('rejects oversized file', async () => {
    renderPage();

    const tooBig = new File(['x'.repeat(11 * 1024 * 1024)], 'big.jpg', { type: 'image/jpeg' });
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    await userEvent.upload(input, tooBig);

    expect(await screen.findByRole('alert')).toHaveTextContent(/zu groß/);
  });

  it('shows source info and converts a JPEG to PNG', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    renderPage();

    await uploadFile(makeJpegFile('foto.jpg'));

    expect(screen.getByText(/Quelle:/)).toBeInTheDocument();
    expect(screen.getByText('foto.jpg')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/tools/raster-to-png');
    expect(init.method).toBe('POST');
    await waitFor(() =>
      expect(screen.getByAltText('Konvertiertes PNG')).toBeInTheDocument(),
    );
  });

  it('shows source info and converts a PNG to PNG', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    renderPage();

    await uploadFile(makePngFile('bild.png'));

    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [url] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/tools/raster-to-png');
  });

  it('forwards width and height as form fields', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    renderPage();

    await uploadFile(makeJpegFile());
    await userEvent.type(screen.getByLabelText('Breite'), '800');
    await userEvent.type(screen.getByLabelText('Höhe'), '600');

    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    const body = init.body as FormData;
    expect(body.get('width')).toBe('800');
    expect(body.get('height')).toBe('600');
  });

  it('rejects width out of range without calling the backend', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    renderPage();

    await uploadFile(makeJpegFile());
    await userEvent.type(screen.getByLabelText('Breite'), '99999');
    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/Breite außerhalb/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('reports backend error as an alert', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ message: 'Pillow Fehler', code: 'BAD_GATEWAY' }), {
        status: 502,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    renderPage();

    await uploadFile(makeJpegFile());
    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/Pillow Fehler/),
    );
  });

  it('reset clears the upload state', async () => {
    renderPage();
    await uploadFile(makeJpegFile('clear-me.jpg'));

    expect(screen.getByText('clear-me.jpg')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /Zurücksetzen/i }));

    expect(screen.queryByText('clear-me.jpg')).not.toBeInTheDocument();
  });
});
