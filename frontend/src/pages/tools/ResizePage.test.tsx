import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ResizePage from './ResizePage';

const PNG_BYTES = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

function makePngFile(name = 'photo.png'): File {
  return new File([PNG_BYTES], name, { type: 'image/png' });
}

function setImageNaturalSize(img: HTMLImageElement, w: number, h: number): void {
  Object.defineProperty(img, 'naturalWidth', { configurable: true, get: () => w });
  Object.defineProperty(img, 'naturalHeight', { configurable: true, get: () => h });
}

async function uploadAndLoad(natural: { w: number; h: number }): Promise<void> {
  const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
  await userEvent.upload(input, makePngFile());
  // The hidden image used to detect natural size has alt="".
  const hidden = document.querySelector('img[alt=""]') as HTMLImageElement;
  setImageNaturalSize(hidden, natural.w, natural.h);
  fireEvent.load(hidden);
}

function pngResponse(): Response {
  return new Response(new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' }), {
    status: 200,
    headers: { 'Content-Type': 'image/png' },
  });
}

describe('ResizePage', () => {
  beforeEach(() => {
    vi.spyOn(globalThis, 'fetch').mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the empty drop zone initially', () => {
    render(<ResizePage />);
    expect(screen.getByText(/Bild verkleinern/i)).toBeInTheDocument();
    expect(screen.getByText(/Bild hier ablegen/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/Neue Breite/)).not.toBeInTheDocument();
  });

  it('shows original dimensions and pre-fills target after upload', async () => {
    render(<ResizePage />);
    await uploadAndLoad({ w: 1600, h: 1200 });

    expect(screen.getByText(/1600/)).toBeInTheDocument();
    expect(screen.getByText(/1200/)).toBeInTheDocument();
    expect(screen.getByLabelText('Neue Breite')).toHaveValue(1600);
    expect(screen.getByLabelText('Neue Höhe')).toHaveValue(1200);
  });

  it('keeps aspect ratio when width is edited while lock is on', async () => {
    render(<ResizePage />);
    await uploadAndLoad({ w: 1600, h: 1200 });

    const widthInput = screen.getByLabelText('Neue Breite');
    await userEvent.clear(widthInput);
    await userEvent.type(widthInput, '800');

    expect(screen.getByLabelText('Neue Höhe')).toHaveValue(600);
  });

  it('keeps height free when aspect lock is disabled', async () => {
    render(<ResizePage />);
    await uploadAndLoad({ w: 1600, h: 1200 });

    await userEvent.click(screen.getByLabelText(/Aspect Ratio entkoppeln/i));
    const widthInput = screen.getByLabelText('Neue Breite');
    await userEvent.clear(widthInput);
    await userEvent.type(widthInput, '800');

    expect(screen.getByLabelText('Neue Höhe')).toHaveValue(1200);
  });

  it('clamps target to original dimensions when downsize-only is on', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    render(<ResizePage />);
    await uploadAndLoad({ w: 1600, h: 1200 });

    // Unlock so width does not change height — easier to assert clamp on width.
    await userEvent.click(screen.getByLabelText(/Aspect Ratio entkoppeln/i));
    const widthInput = screen.getByLabelText('Neue Breite');
    await userEvent.clear(widthInput);
    await userEvent.type(widthInput, '3000');

    await userEvent.click(screen.getByRole('button', { name: /^Verkleinern$/i }));

    await waitFor(() => expect(widthInput).toHaveValue(1600));
  });

  it('submits width and height to the backend and shows result + download', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(pngResponse());
    render(<ResizePage />);
    await uploadAndLoad({ w: 1600, h: 1200 });

    const widthInput = screen.getByLabelText('Neue Breite');
    await userEvent.clear(widthInput);
    await userEvent.type(widthInput, '800');

    await userEvent.click(screen.getByRole('button', { name: /^Verkleinern$/i }));

    await waitFor(() => expect(screen.getByAltText(/Verkleinertes Bild/i)).toBeInTheDocument());
    const call = fetchSpy.mock.calls.find(
      ([url]) => typeof url === 'string' && url.includes('/api/tools/resize'),
    );
    expect(call).toBeDefined();
    const body = call![1]?.body as FormData;
    expect(body.get('width')).toBe('800');
    expect(body.get('height')).toBe('600');
    const downloadLink = screen.getByRole('link', { name: /Herunterladen/i });
    expect(downloadLink).toHaveAttribute('download', 'photo-800x600.png');
  });

  it('shows backend error message on failure', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ status: 502, message: 'upstream down' }), {
        status: 502,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    render(<ResizePage />);
    await uploadAndLoad({ w: 1600, h: 1200 });

    await userEvent.click(screen.getByRole('button', { name: /^Verkleinern$/i }));

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('upstream down'),
    );
  });

  it('rejects unsupported content types', async () => {
    render(<ResizePage />);
    const input = screen.getByLabelText(/Bild auswählen/i) as HTMLInputElement;
    const txt = new File(['hello'], 'notes.txt', { type: 'text/plain' });
    await userEvent.upload(input, txt, { applyAccept: false });
    expect(screen.getByRole('alert')).toHaveTextContent(/Format nicht unterstützt/i);
  });
});
