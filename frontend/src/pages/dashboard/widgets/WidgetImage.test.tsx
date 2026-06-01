import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import WidgetImage, { clamp01, panBy, parseImageConfig } from './WidgetImage';
import type { WidgetDto } from '../../../api/dashboard';

vi.mock('../../../api/images', async () => {
  const actual = await vi.importActual<typeof import('../../../api/images')>('../../../api/images');
  return {
    ...actual,
    fetchImageObjectUrl: vi.fn(async () => 'blob:mock-url'),
    uploadImage: vi.fn(),
  };
});

import { fetchImageObjectUrl } from '../../../api/images';

const fetchUrl = fetchImageObjectUrl as ReturnType<typeof vi.fn>;

// jsdom kennt URL.revokeObjectURL nicht — Cleanup im Effekt darf nicht crashen.
if (typeof URL.revokeObjectURL !== 'function') {
  URL.revokeObjectURL = () => undefined;
}

function widget(config: object): WidgetDto {
  return { id: 1, type: 'IMAGE', posX: 0, posY: 0, width: 4, height: 4, config: JSON.stringify(config) };
}

describe('WidgetImage (#183)', () => {
  afterEach(() => {
    cleanup();
    fetchUrl.mockClear();
  });

  it('rendert den Platzhalter und im Edit-Modus die Aktions-Icons', () => {
    render(<WidgetImage widget={widget({ imageId: null })} onChange={vi.fn()} onDelete={vi.fn()} />);
    expect(screen.getByText(/Kein Bild/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bild bearbeiten' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bild löschen' })).toBeInTheDocument();
  });

  it('blendet im Read-Modus die Aktions-Icons aus', () => {
    render(
      <WidgetImage widget={widget({ imageId: null })} onChange={vi.fn()} onDelete={vi.fn()} readOnly />,
    );
    expect(screen.queryByRole('button', { name: 'Bild bearbeiten' })).not.toBeInTheDocument();
  });

  it('ruft onDelete beim Klick auf das Lösch-Icon', async () => {
    const onDelete = vi.fn();
    render(<WidgetImage widget={widget({ imageId: null })} onChange={vi.fn()} onDelete={onDelete} />);
    await userEvent.click(screen.getByRole('button', { name: 'Bild löschen' }));
    expect(onDelete).toHaveBeenCalledTimes(1);
  });

  it('speichert Rahmen + Hintergrundfarbe aus dem Drawer', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<WidgetImage widget={widget({ imageId: null })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Bild bearbeiten' }));
    await user.click(screen.getByRole('checkbox', { name: 'Rahmen anzeigen' }));
    await user.type(screen.getByLabelText('Hintergrundfarbe (leer = transparent)'), '#1e1e1e');
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const parsed = JSON.parse((onChange.mock.calls[0][0] as WidgetDto).config) as {
      showBorder: boolean;
      backgroundColor?: string;
      mode: string;
      objectFit: string;
    };
    expect(parsed.showBorder).toBe(true);
    expect(parsed.backgroundColor).toBe('#1e1e1e');
    // Forward-Compat-Felder bleiben erhalten.
    expect(parsed.mode).toBe('resize');
    expect(parsed.objectFit).toBe('contain');
  });

  it('rendert bei invalider Config ohne Crash (Defaults)', () => {
    const w: WidgetDto = { type: 'IMAGE', posX: 0, posY: 0, width: 4, height: 4, config: 'kein-json' };
    render(<WidgetImage widget={w} onChange={vi.fn()} onDelete={vi.fn()} />);
    expect(screen.getByText(/Kein Bild/)).toBeInTheDocument();
  });

  it('lädt und zeigt das Bild bei gesetzter imageId (#184)', async () => {
    fetchUrl.mockResolvedValueOnce('blob:mock-url');
    render(<WidgetImage widget={widget({ imageId: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    const img = (await screen.findByAltText('Widget-Bild')) as HTMLImageElement;
    expect(img.src).toContain('blob:mock-url');
    expect(fetchUrl).toHaveBeenCalledWith(5);
  });

  it('zeigt einen Fehler, wenn das Laden scheitert (#184)', async () => {
    fetchUrl.mockRejectedValueOnce(new Error('boom'));
    render(<WidgetImage widget={widget({ imageId: 5 })} onChange={vi.fn()} onDelete={vi.fn()} />);

    expect(await screen.findByText(/konnte nicht geladen/)).toBeInTheDocument();
  });

  it('speichert objectFit aus dem Drawer (#185)', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<WidgetImage widget={widget({ imageId: 5 })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Bild bearbeiten' }));
    await user.click(await screen.findByRole('combobox', { name: 'Anpassung' }));
    await user.click(await screen.findByRole('option', { name: /Füllen/ }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const parsed = JSON.parse((onChange.mock.calls[0][0] as WidgetDto).config) as { objectFit: string };
    expect(parsed.objectFit).toBe('cover');
  });

  it('rendert das Bild mit dem konfigurierten objectFit (#185)', async () => {
    render(
      <WidgetImage widget={widget({ imageId: 5, objectFit: 'cover' })} onChange={vi.fn()} onDelete={vi.fn()} />,
    );
    const img = (await screen.findByAltText('Widget-Bild')) as HTMLImageElement;
    expect(img).toHaveStyle('object-fit: cover');
  });

  it('persistiert die imageId nach Entfernen (#184)', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<WidgetImage widget={widget({ imageId: 5 })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Bild bearbeiten' }));
    await user.click(await screen.findByRole('button', { name: 'Bild entfernen' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const parsed = JSON.parse((onChange.mock.calls[0][0] as WidgetDto).config) as { imageId: number | null };
    expect(parsed.imageId).toBeNull();
  });
});

describe('WidgetImage Crop-Modus (#186)', () => {
  afterEach(() => {
    cleanup();
    fetchUrl.mockClear();
  });

  it('speichert den Modus "crop" aus dem Drawer', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<WidgetImage widget={widget({ imageId: 5 })} onChange={onChange} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Bild bearbeiten' }));
    await user.click(await screen.findByRole('radio', { name: 'Ausschnitt' }));
    await user.click(screen.getByRole('button', { name: 'Übernehmen' }));

    const parsed = JSON.parse((onChange.mock.calls[0][0] as WidgetDto).config) as { mode: string };
    expect(parsed.mode).toBe('crop');
  });

  it('pannt im Read-Modus NICHT (kein onChange beim Ziehen)', async () => {
    const onChange = vi.fn();
    render(
      <WidgetImage
        widget={widget({ imageId: 5, mode: 'crop' })}
        onChange={onChange}
        onDelete={vi.fn()}
        readOnly
      />,
    );
    const img = await screen.findByAltText('Widget-Bild');
    const container = img.parentElement as HTMLElement;
    expect(container).toHaveStyle('cursor: default');
    fireEvent.pointerDown(container, { clientX: 10, clientY: 10 });
    fireEvent.pointerMove(container, { clientX: 80, clientY: 10 });
    fireEvent.pointerUp(container);
    expect(onChange).not.toHaveBeenCalled();
  });

  it('zeigt die Aktions-Icons auch im Crop-Modus mit geladenem Bild', async () => {
    render(
      <WidgetImage widget={widget({ imageId: 5, mode: 'crop' })} onChange={vi.fn()} onDelete={vi.fn()} />,
    );
    await screen.findByAltText('Widget-Bild');
    expect(screen.getByRole('button', { name: 'Bild bearbeiten' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bild löschen' })).toBeInTheDocument();
  });

  it('rendert im Crop-Modus mit objectFit none + objectPosition', async () => {
    render(
      <WidgetImage
        widget={widget({ imageId: 5, mode: 'crop', cropOffsetX: 0.5, cropOffsetY: 0.25 })}
        onChange={vi.fn()}
        onDelete={vi.fn()}
      />,
    );
    const img = (await screen.findByAltText('Widget-Bild')) as HTMLImageElement;
    expect(img).toHaveStyle('object-fit: none');
    expect(img).toHaveStyle('object-position: 50% 25%');
  });
});

describe('panBy / clamp01 (#186)', () => {
  it('clamp01 begrenzt auf [0,1]', () => {
    expect(clamp01(-0.5)).toBe(0);
    expect(clamp01(0.4)).toBe(0.4);
    expect(clamp01(2)).toBe(1);
  });

  it('panBy verschiebt gegenläufig und klemmt', () => {
    // 200px breiter Container, 50px nach rechts ziehen → Offset -0.25, von 0.5 → 0.25.
    expect(panBy(0.5, 50, 200)).toBeCloseTo(0.25);
    // Klemmt bei 0 und 1.
    expect(panBy(0.1, 1000, 200)).toBe(0);
    expect(panBy(0.9, -1000, 200)).toBe(1);
  });

  it('panBy ohne Container-Breite lässt den Offset unverändert', () => {
    expect(panBy(0.5, 50, 0)).toBe(0.5);
  });
});

describe('WidgetImage Download (#192)', () => {
  afterEach(() => {
    cleanup();
    fetchUrl.mockClear();
  });

  it('zeigt die Download-Sektion; Button ist ohne Bild deaktiviert', async () => {
    const user = userEvent.setup();
    render(<WidgetImage widget={widget({ imageId: null })} onChange={vi.fn()} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Bild bearbeiten' }));
    expect(screen.getByRole('radio', { name: 'PNG' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'JPEG' })).toBeInTheDocument();
    expect(screen.getByText('Export-Größe: –')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Bild herunterladen/ })).toBeDisabled();
  });

  it('blendet den Qualitäts-Slider nur bei JPEG ein', async () => {
    const user = userEvent.setup();
    render(<WidgetImage widget={widget({ imageId: null })} onChange={vi.fn()} onDelete={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Bild bearbeiten' }));
    expect(screen.queryByRole('slider', { name: 'JPEG-Qualität' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: 'JPEG' }));
    expect(screen.getByRole('slider', { name: 'JPEG-Qualität' })).toBeInTheDocument();
  });
});

describe('parseImageConfig (#183)', () => {
  it('liefert Defaults bei invalider Config', () => {
    expect(parseImageConfig('nope')).toEqual({
      imageId: null,
      mode: 'resize',
      objectFit: 'contain',
      cropOffsetX: 0,
      cropOffsetY: 0,
      showBorder: false,
    });
  });

  it('liest gültige Werte und klemmt Offsets auf 0..1', () => {
    const c = parseImageConfig(
      JSON.stringify({ imageId: 7, mode: 'crop', objectFit: 'cover', cropOffsetX: 2, cropOffsetY: -1 }),
    );
    expect(c.imageId).toBe(7);
    expect(c.mode).toBe('crop');
    expect(c.objectFit).toBe('cover');
    expect(c.cropOffsetX).toBe(1);
    expect(c.cropOffsetY).toBe(0);
  });

  it('fällt bei unbekanntem mode/objectFit auf Defaults zurück', () => {
    const c = parseImageConfig(JSON.stringify({ mode: 'xxx', objectFit: 'yyy' }));
    expect(c.mode).toBe('resize');
    expect(c.objectFit).toBe('contain');
  });
});
