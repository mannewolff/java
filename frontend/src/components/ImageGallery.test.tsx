import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../api/images', () => ({
  listImages: vi.fn(),
  fetchImageObjectUrl: vi.fn(),
}));

import ImageGallery from './ImageGallery';
import { listImages, fetchImageObjectUrl, type ImageListResponse } from '../api/images';

const listMock = vi.mocked(listImages);
const fetchUrlMock = vi.mocked(fetchImageObjectUrl);

function page(ids: number[], total: number): ImageListResponse {
  return {
    images: ids.map((id) => ({
      id,
      contentType: 'image/png',
      sizeBytes: 10,
      createdAt: '2026-06-01T00:00:00Z',
      hash: null,
    })),
    total,
  };
}

describe('ImageGallery (#198)', () => {
  beforeEach(() => {
    listMock.mockReset();
    fetchUrlMock.mockReset();
    fetchUrlMock.mockResolvedValue('blob:thumb');
  });
  afterEach(() => cleanup());

  it('zeigt einen Ladeindikator, dann Thumbnails', async () => {
    listMock.mockResolvedValue(page([1, 2], 2));
    render(<ImageGallery onSelect={vi.fn()} />);

    expect(screen.getByLabelText('Galerie lädt')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Bild 1 auswählen' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Bild 2 auswählen' })).toBeInTheDocument();
  });

  it('ruft onSelect mit der Bild-ID auf', async () => {
    listMock.mockResolvedValue(page([3], 1));
    const onSelect = vi.fn();
    render(<ImageGallery onSelect={onSelect} />);

    await userEvent.click(await screen.findByRole('button', { name: 'Bild 3 auswählen' }));
    expect(onSelect).toHaveBeenCalledWith(3);
  });

  it('zeigt einen Leer-Zustand ohne Bilder', async () => {
    listMock.mockResolvedValue(page([], 0));
    render(<ImageGallery onSelect={vi.fn()} />);

    expect(await screen.findByText(/Noch keine Bilder/)).toBeInTheDocument();
  });

  it('zeigt eine Fehlermeldung bei Ladefehler', async () => {
    listMock.mockRejectedValue(new Error('boom'));
    render(<ImageGallery onSelect={vi.fn()} />);

    expect(await screen.findByText(/Galerie konnte nicht geladen werden/)).toBeInTheDocument();
  });

  it('blättert mit Weiter zur nächsten Seite (höherer Offset)', async () => {
    // Erste Seite: 24 Bilder, total 30 → "Weiter" aktiv.
    const firstIds = Array.from({ length: 24 }, (_, i) => i + 1);
    listMock.mockResolvedValueOnce(page(firstIds, 30));
    listMock.mockResolvedValueOnce(page([25, 26], 30));
    render(<ImageGallery onSelect={vi.fn()} />);

    await screen.findByRole('button', { name: 'Bild 1 auswählen' });
    await userEvent.click(screen.getByRole('button', { name: 'Weiter' }));

    await waitFor(() => expect(listMock).toHaveBeenCalledWith(24, 24));
    expect(await screen.findByRole('button', { name: 'Bild 25 auswählen' })).toBeInTheDocument();
  });
});
