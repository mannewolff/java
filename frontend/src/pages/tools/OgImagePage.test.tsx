import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OgImagePage from './OgImagePage';

const PNG_BYTES = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

function makePngFile(name = 'photo.png'): File {
  return new File([PNG_BYTES], name, { type: 'image/png' });
}

function jpegResponse(): Response {
  return new Response(new Blob([new Uint8Array([1, 2, 3])], { type: 'image/jpeg' }), {
    status: 200,
    headers: { 'Content-Type': 'image/jpeg' },
  });
}

function paletteResponse(colors: string[]): Response {
  return new Response(JSON.stringify({ colors }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

function routeFetchByUrl(handlers: Record<string, () => Response>) {
  return vi.fn(async (input: RequestInfo | URL) => {
    const url = typeof input === 'string' ? input : input.toString();
    const path = Object.keys(handlers).find((p) => url.includes(p));
    if (!path) throw new Error(`unexpected fetch URL: ${url}`);
    return handlers[path]();
  });
}

describe('OgImagePage', () => {
  beforeEach(() => {
    vi.spyOn(globalThis, 'fetch').mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the empty drop zone initially', () => {
    render(<OgImagePage />);
    expect(screen.getByText(/Beitragsbild/i)).toBeInTheDocument();
    expect(screen.getByText(/Bild hier ablegen/i)).toBeInTheDocument();
    expect(screen.queryByRole('img', { name: /Vorschau/i })).not.toBeInTheDocument();
  });

  it('rejects unsupported content types', async () => {
    render(<OgImagePage />);
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    const txt = new File(['hello'], 'notes.txt', { type: 'text/plain' });

    await userEvent.upload(input, txt, { applyAccept: false });

    expect(screen.getByRole('alert')).toHaveTextContent(/Format nicht unterstützt/i);
  });

  it('crops, palettes, and shows download link after upload', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      routeFetchByUrl({
        '/api/tools/crop-og': jpegResponse,
        '/api/tools/palette': () =>
          paletteResponse(['#aabbcc', '#001122', '#abcdef', '#112233', '#445566', '#778899']),
      }),
    );
    render(<OgImagePage />);
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;

    await userEvent.upload(input, makePngFile());

    await waitFor(
      () =>
        expect(
          screen.getByRole('img', { name: /Beitragsbild Vorschau/i }),
        ).toBeInTheDocument(),
      { timeout: 2000 },
    );
    expect(screen.getByRole('button', { name: /Farbe #aabbcc kopieren/i })).toBeInTheDocument();
    const download = screen.getByRole('link', { name: /JPEG herunterladen/i });
    expect(download).toHaveAttribute('download', 'photo-1200x630.jpg');
  });

  it('copies a hex value to the clipboard when a swatch is clicked', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    });
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      routeFetchByUrl({
        '/api/tools/crop-og': jpegResponse,
        '/api/tools/palette': () => paletteResponse(['#aabbcc']),
      }),
    );
    render(<OgImagePage />);
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;

    await userEvent.upload(input, makePngFile());
    const swatch = await screen.findByRole('button', { name: /Farbe #aabbcc kopieren/i }, { timeout: 2000 });
    await userEvent.click(swatch);

    expect(writeText).toHaveBeenCalledWith('#aabbcc');
  });

  it('shows backend error message on failure', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      routeFetchByUrl({
        '/api/tools/crop-og': () =>
          new Response(JSON.stringify({ status: 502, message: 'upstream down' }), {
            status: 502,
            headers: { 'Content-Type': 'application/json' },
          }),
        '/api/tools/palette': () => paletteResponse(['#000000']),
      }),
    );
    render(<OgImagePage />);
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;

    await userEvent.upload(input, makePngFile());

    await waitFor(
      () => expect(screen.getByRole('alert')).toHaveTextContent('upstream down'),
      { timeout: 2000 },
    );
  });

  it('opens settings drawer and applies a preset to the next crop request', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(
      routeFetchByUrl({
        '/api/tools/crop-og': jpegResponse,
        '/api/tools/palette': () => paletteResponse(['#000000']),
      }),
    );
    render(<OgImagePage />);
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;

    await userEvent.upload(input, makePngFile());
    await screen.findByRole('img', { name: /Beitragsbild Vorschau/i }, { timeout: 2000 });

    // Open settings + switch to Twitter Card.
    await userEvent.click(screen.getByRole('button', { name: /Einstellungen öffnen/i }));
    const presetSelect = await screen.findByRole('combobox', { name: /Preset/i });
    await userEvent.click(presetSelect);
    await userEvent.click(screen.getByRole('option', { name: /Twitter Card/i }));
    await userEvent.click(screen.getByRole('button', { name: /Übernehmen/i }));

    // The latest crop call must carry width=1200 and height=675.
    await waitFor(() => {
      const cropCalls = fetchSpy.mock.calls.filter(
        ([url]) => typeof url === 'string' && url.includes('/api/tools/crop-og'),
      );
      expect(cropCalls.length).toBeGreaterThanOrEqual(2);
      const lastBody = cropCalls[cropCalls.length - 1][1]?.body as FormData;
      expect(lastBody.get('width')).toBe('1200');
      expect(lastBody.get('height')).toBe('675');
    }, { timeout: 2000 });

    // Download filename reflects the new dimensions.
    const download = screen.getByRole('link', { name: /JPEG herunterladen/i });
    expect(download).toHaveAttribute('download', 'photo-1200x675.jpg');
  });

  it('disables apply when custom dimensions are out of range', async () => {
    render(<OgImagePage />);

    await userEvent.click(screen.getByRole('button', { name: /Einstellungen öffnen/i }));
    const widthInput = await screen.findByLabelText(/Breite/);
    await userEvent.clear(widthInput);
    await userEvent.type(widthInput, '50');

    const apply = screen.getByRole('button', { name: /Übernehmen/i });
    expect(apply).toBeDisabled();
  });

  it('resets state when the reset button is clicked', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(
      routeFetchByUrl({
        '/api/tools/crop-og': jpegResponse,
        '/api/tools/palette': () => paletteResponse(['#000000']),
      }),
    );
    render(<OgImagePage />);
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;

    await userEvent.upload(input, makePngFile());
    await screen.findByRole('img', { name: /Beitragsbild Vorschau/i }, { timeout: 2000 });

    await userEvent.click(screen.getByRole('button', { name: /Zurücksetzen/i }));

    expect(screen.queryByRole('img', { name: /Beitragsbild Vorschau/i })).not.toBeInTheDocument();
  });
});
