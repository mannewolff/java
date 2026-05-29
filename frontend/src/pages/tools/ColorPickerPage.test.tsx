import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import ColorPickerPage from './ColorPickerPage';
import { NotifyProvider } from '../../notify/NotifyProvider';

// Bekannte Pixelfarbe, die der gemockte Canvas-Context zurückgibt: #1a1a2e.
const PIXEL = new Uint8ClampedArray([26, 26, 46, 255]);

const fakeCtx = {
  drawImage: vi.fn(),
  getImageData: vi.fn(() => ({ data: PIXEL })),
};

function renderPage(): ReturnType<typeof render> {
  return render(
    <NotifyProvider>
      <ColorPickerPage />
    </NotifyProvider>,
  );
}

function makeImageFile(name = 'pic.png', type = 'image/png'): File {
  return new File([new Uint8Array([0x89, 0x50, 0x4e, 0x47])], name, { type });
}

/** Lädt ein Bild hoch und simuliert load + Klick auf das Pixel (10,10). */
async function uploadAndPick(): Promise<void> {
  const input = screen.getByLabelText('Bild auswählen') as HTMLInputElement;
  await userEvent.upload(input, makeImageFile());

  const img = screen.getByAltText('Hochgeladenes Bild') as HTMLImageElement;
  Object.defineProperty(img, 'naturalWidth', { value: 100, configurable: true });
  Object.defineProperty(img, 'naturalHeight', { value: 100, configurable: true });
  img.getBoundingClientRect = () =>
    ({ left: 0, top: 0, width: 100, height: 100, right: 100, bottom: 100, x: 0, y: 0, toJSON() {} }) as DOMRect;

  fireEvent.load(img);
  fireEvent.click(img, { clientX: 10, clientY: 10 });
}

describe('ColorPickerPage', () => {
  beforeEach(() => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(
      fakeCtx as unknown as CanvasRenderingContext2D,
    );
    fakeCtx.getImageData.mockClear();
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('renders the empty drop zone initially', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /Farbpipette/i })).toBeInTheDocument();
    expect(screen.getByText(/Bild hier ablegen/i)).toBeInTheDocument();
    expect(screen.queryByAltText('Hochgeladenes Bild')).not.toBeInTheDocument();
  });

  it('rejects an unsupported file type', async () => {
    renderPage();
    const input = screen.getByLabelText('Bild auswählen') as HTMLInputElement;
    const txt = new File(['hello'], 'note.txt', { type: 'text/plain' });
    await userEvent.upload(input, txt, { applyAccept: false });

    expect(await screen.findByRole('alert')).toHaveTextContent(/Format nicht unterstützt/);
    expect(screen.queryByAltText('Hochgeladenes Bild')).not.toBeInTheDocument();
  });

  it('fills HEX and RGB fields when a pixel is clicked', async () => {
    renderPage();
    await uploadAndPick();

    expect(fakeCtx.getImageData).toHaveBeenCalled();
    expect(screen.getByLabelText('HEX')).toHaveValue('#1a1a2e');
    expect(screen.getByLabelText('RGB')).toHaveValue('rgb(26, 26, 46)');
  });

  it('copies the HEX value and shows feedback', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
      writable: true,
    });

    renderPage();
    await uploadAndPick();

    // userEvent.setup registriert einen eigenen Clipboard-Stub — danach erneut patchen.
    const user = userEvent.setup();
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
      writable: true,
    });

    await user.click(screen.getByRole('button', { name: 'HEX kopieren' }));

    expect(writeText).toHaveBeenCalledWith('#1a1a2e');
    expect(await screen.findByRole('alert')).toHaveTextContent('Farbwert kopiert');
  });

  it('copies the RGB value', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
      writable: true,
    });

    renderPage();
    await uploadAndPick();

    const user = userEvent.setup();
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
      writable: true,
    });

    await user.click(screen.getByRole('button', { name: 'RGB kopieren' }));
    expect(writeText).toHaveBeenCalledWith('rgb(26, 26, 46)');
  });

  it('disables the copy buttons before a pixel is picked', async () => {
    renderPage();
    const input = screen.getByLabelText('Bild auswählen') as HTMLInputElement;
    await userEvent.upload(input, makeImageFile());

    expect(screen.getByRole('button', { name: 'HEX kopieren' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'RGB kopieren' })).toBeDisabled();
  });
});
