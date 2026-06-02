import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/images', () => ({
  listManagedImages: vi.fn(),
  deleteImages: vi.fn(),
  fetchThumbnailObjectUrl: vi.fn(),
}));

import ImageManagerPage from './ImageManagerPage';
import { NotifyProvider } from '../../notify/NotifyProvider';
import {
  listManagedImages,
  deleteImages,
  fetchThumbnailObjectUrl,
  type ManagedImage,
  type ManagedImageListResponse,
} from '../../api/images';

const listMock = vi.mocked(listManagedImages);
const deleteMock = vi.mocked(deleteImages);
const thumbMock = vi.mocked(fetchThumbnailObjectUrl);

function img(over: Partial<ManagedImage> = {}): ManagedImage {
  return {
    id: 1,
    contentType: 'image/png',
    sizeBytes: 2048,
    createdAt: '2026-06-01T00:00:00Z',
    hash: 'abc',
    usageCount: 0,
    ...over,
  };
}

function page(images: ManagedImage[], total = images.length): ManagedImageListResponse {
  return { images, total };
}

function renderPage() {
  return render(
    <NotifyProvider>
      <ImageManagerPage />
    </NotifyProvider>,
  );
}

describe('ImageManagerPage (#202)', () => {
  beforeEach(() => {
    listMock.mockReset();
    deleteMock.mockReset();
    thumbMock.mockReset();
    thumbMock.mockResolvedValue('blob:thumb');
  });
  afterEach(() => cleanup());

  it('zeigt Ladeindikator, dann Bilder mit Metadaten und Usage', async () => {
    listMock.mockResolvedValue(page([img({ id: 1, usageCount: 0 }), img({ id: 2, usageCount: 3 })]));
    renderPage();

    expect(screen.getByLabelText('Bilder laden')).toBeInTheDocument();
    expect(await screen.findByRole('checkbox', { name: 'Bild 1 auswählen' })).toBeEnabled();
    // Benutztes Bild: Checkbox deaktiviert.
    expect(screen.getByRole('checkbox', { name: 'Bild 2 auswählen' })).toBeDisabled();
    expect(screen.getByText(/Benutzt \(3\)/)).toBeInTheDocument();
  });

  it('Löschen-Button ist erst nach Auswahl aktiv', async () => {
    listMock.mockResolvedValue(page([img({ id: 1 })]));
    renderPage();

    const delBtn = await screen.findByRole('button', { name: /Löschen/ });
    expect(delBtn).toBeDisabled();
    await userEvent.click(screen.getByRole('checkbox', { name: 'Bild 1 auswählen' }));
    expect(screen.getByRole('button', { name: /Löschen \(1\)/ })).toBeEnabled();
  });

  it('Alle auswählen markiert nur löschbare Bilder', async () => {
    listMock.mockResolvedValue(page([img({ id: 1, usageCount: 0 }), img({ id: 2, usageCount: 1 })]));
    renderPage();

    await screen.findByRole('checkbox', { name: 'Bild 1 auswählen' });
    await userEvent.click(screen.getByRole('checkbox', { name: 'Alle löschbaren auswählen' }));

    expect(screen.getByRole('checkbox', { name: 'Bild 1 auswählen' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Bild 2 auswählen' })).not.toBeChecked();
  });

  it('löscht nach Bestätigung und lädt neu', async () => {
    listMock.mockResolvedValueOnce(page([img({ id: 1 })]));
    listMock.mockResolvedValueOnce(page([]));
    deleteMock.mockResolvedValue({ deleted: [1], failed: [] });
    renderPage();

    await userEvent.click(await screen.findByRole('checkbox', { name: 'Bild 1 auswählen' }));
    await userEvent.click(screen.getByRole('button', { name: /Löschen \(1\)/ }));
    // Dialog
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Löschen' }));

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith([1]));
    expect(await screen.findByText('1 Bild(er) gelöscht')).toBeInTheDocument();
  });

  it('zeigt Leer-Zustand', async () => {
    listMock.mockResolvedValue(page([]));
    renderPage();
    expect(await screen.findByText('Keine Bilder vorhanden.')).toBeInTheDocument();
  });

  it('zeigt Fehler mit Retry', async () => {
    listMock.mockRejectedValue(new Error('boom'));
    renderPage();
    expect(await screen.findByText(/Bilder konnten nicht geladen werden/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Erneut' })).toBeInTheDocument();
  });
});
