import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import SvgToPngPage from './SvgToPngPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

function renderSvg(): ReturnType<typeof render> {
  return render(
    <NotifyProvider>
      <SvgToPngPage />
    </NotifyProvider>,
  );
}

const SVG_TEXT =
  '<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><rect width="10" height="10"/></svg>';

function makeSvgFile(name = 'logo.svg'): File {
  return new File([SVG_TEXT], name, { type: 'image/svg+xml' });
}

function pngResponse(): Response {
  return new Response(new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' }), {
    status: 200,
    headers: { 'Content-Type': 'image/png' },
  });
}

async function uploadSvg(name = 'logo.svg'): Promise<void> {
  const input = screen.getByLabelText(/SVG auswählen/i) as HTMLInputElement;
  await userEvent.upload(input, makeSvgFile(name));
}

describe('SvgToPngPage', () => {
  beforeEach(() => {
    vi.spyOn(globalThis, 'fetch').mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the empty drop zone initially', () => {
    renderSvg();

    expect(screen.getByRole('heading', { name: /SVG zu PNG/i })).toBeInTheDocument();
    expect(screen.getByText(/SVG hier ablegen/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/Breite/)).not.toBeInTheDocument();
  });

  it('rejects non-SVG content type', async () => {
    renderSvg();

    const input = screen.getByLabelText(/SVG auswählen/i) as HTMLInputElement;
    const pngFile = new File([new Uint8Array([0x89, 0x50])], 'image.png', { type: 'image/png' });
    // applyAccept: false — der accept-Attribute des Inputs (image/svg+xml,.svg) wuerde
    // sonst von userEvent.upload erzwungen, der PNG-Upload nie stattfinden und unser
    // clientseitiger Reject-Pfad nie getroffen. Wir wollen genau diesen pruefen.
    await userEvent.upload(input, pngFile, { applyAccept: false });

    expect(await screen.findByRole('alert')).toHaveTextContent(/Format nicht unterstützt/);
    expect(screen.queryByRole('button', { name: /Konvertieren/i })).not.toBeInTheDocument();
  });

  it('rejects oversized SVG', async () => {
    renderSvg();

    const input = screen.getByLabelText(/SVG auswählen/i) as HTMLInputElement;
    const tooBig = new File(['x'.repeat(11 * 1024 * 1024)], 'big.svg', { type: 'image/svg+xml' });
    await userEvent.upload(input, tooBig);

    expect(await screen.findByRole('alert')).toHaveTextContent(/zu groß/);
  });

  it('shows source info and converts a valid SVG to PNG', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    renderSvg();

    await uploadSvg('icon.svg');

    expect(screen.getByText(/Quelle:/)).toBeInTheDocument();
    expect(screen.getByText('icon.svg')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/tools/svg-to-png');
    expect(init.method).toBe('POST');
    // Result preview is shown.
    await waitFor(() =>
      expect(screen.getByAltText('Konvertiertes PNG')).toBeInTheDocument(),
    );
  });

  it('forwards width, height and background as form fields', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    renderSvg();

    await uploadSvg();
    await userEvent.type(screen.getByLabelText('Breite'), '512');
    await userEvent.type(screen.getByLabelText('Höhe'), '256');

    // Settings drawer: set background to #ffffff
    await userEvent.click(screen.getByLabelText('Einstellungen öffnen'));
    const bgField = await screen.findByLabelText('Hintergrund');
    await userEvent.clear(bgField);
    await userEvent.type(bgField, '#ffffff');
    await userEvent.click(screen.getByRole('button', { name: /Übernehmen/i }));

    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    const body = init.body as FormData;
    expect(body.get('width')).toBe('512');
    expect(body.get('height')).toBe('256');
    expect(body.get('background')).toBe('#ffffff');
  });

  it('rejects invalid background pattern in the drawer', async () => {
    renderSvg();

    await uploadSvg();
    await userEvent.click(screen.getByLabelText('Einstellungen öffnen'));
    const bgField = await screen.findByLabelText('Hintergrund');
    await userEvent.clear(bgField);
    await userEvent.type(bgField, 'red');
    await userEvent.click(screen.getByRole('button', { name: /Übernehmen/i }));

    // Drawer stays open with an error message.
    expect(screen.getByText(/Erlaubt:/)).toBeInTheDocument();
  });

  it('cancels the settings drawer without committing the draft background', async () => {
    renderSvg();
    await uploadSvg();

    await userEvent.click(screen.getByLabelText('Einstellungen öffnen'));
    const bgField = await screen.findByLabelText('Hintergrund');
    await userEvent.clear(bgField);
    await userEvent.type(bgField, '#abcdef');
    await userEvent.click(screen.getByRole('button', { name: /Abbrechen/i }));

    expect(screen.getByText(/Hintergrund:/)).toHaveTextContent('transparent');
  });

  it('reports backend error as an alert', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ message: 'cairo crash', code: 'BAD_GATEWAY' }), {
        status: 502,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    renderSvg();

    await uploadSvg();
    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/cairo crash/),
    );
  });

  it('rejects width out of range without calling the backend', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    renderSvg();

    await uploadSvg();
    await userEvent.type(screen.getByLabelText('Breite'), '99999');
    await userEvent.click(screen.getByRole('button', { name: /Konvertieren/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/Breite außerhalb/);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('reset clears the upload state', async () => {
    renderSvg();
    await uploadSvg('clear-me.svg');

    expect(screen.getByText('clear-me.svg')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /Zurücksetzen/i }));

    expect(screen.queryByText('clear-me.svg')).not.toBeInTheDocument();
  });
});
