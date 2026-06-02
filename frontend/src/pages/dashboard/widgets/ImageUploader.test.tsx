import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../../api/images', async () => {
  const actual = await vi.importActual<typeof import('../../../api/images')>('../../../api/images');
  return {
    ...actual,
    uploadImage: vi.fn(),
    checkImageHash: vi.fn(),
    sha256Hex: vi.fn(),
    listImages: vi.fn(),
    fetchThumbnailObjectUrl: vi.fn(),
  };
});

import {
  uploadImage,
  checkImageHash,
  sha256Hex,
  listImages,
  fetchThumbnailObjectUrl,
} from '../../../api/images';
import ImageUploader from './ImageUploader';

const upload = uploadImage as ReturnType<typeof vi.fn>;
const checkHash = checkImageHash as ReturnType<typeof vi.fn>;
const hashOf = sha256Hex as ReturnType<typeof vi.fn>;
const list = listImages as ReturnType<typeof vi.fn>;
const fetchUrl = fetchThumbnailObjectUrl as ReturnType<typeof vi.fn>;

if (typeof URL.createObjectURL !== 'function') {
  URL.createObjectURL = () => 'blob:preview';
}
if (typeof URL.revokeObjectURL !== 'function') {
  URL.revokeObjectURL = () => undefined;
}

function fileOfSize(name: string, type: string, size: number): File {
  const file = new File(['x'], name, { type });
  Object.defineProperty(file, 'size', { value: size });
  return file;
}

describe('ImageUploader (#184)', () => {
  beforeEach(() => {
    // Default: Hash-Prüfung läuft, findet kein Duplikat → normaler Upload-Pfad.
    hashOf.mockResolvedValue('a'.repeat(64));
    checkHash.mockResolvedValue({ exists: false });
    list.mockResolvedValue({ images: [], total: 0 });
    fetchUrl.mockResolvedValue('blob:thumb');
  });
  afterEach(() => {
    cleanup();
    upload.mockReset();
    checkHash.mockReset();
    hashOf.mockReset();
    list.mockReset();
    fetchUrl.mockReset();
  });

  it('lädt eine gültige Datei hoch und meldet das Ergebnis', async () => {
    upload.mockResolvedValue({ id: 7, url: '/api/images/7' });
    const onUploaded = vi.fn();
    const user = userEvent.setup();
    render(<ImageUploader label="Bild hochladen" onUploaded={onUploaded} />);

    await user.upload(
      screen.getByTestId('image-file-input'),
      fileOfSize('x.png', 'image/png', 1234),
    );

    await waitFor(() => expect(onUploaded).toHaveBeenCalledWith({ id: 7, url: '/api/images/7' }));
    expect(upload).toHaveBeenCalledTimes(1);
  });

  it('lehnt ein nicht unterstütztes Format ab (kein Upload)', async () => {
    const onUploaded = vi.fn();
    render(<ImageUploader label="Bild hochladen" onUploaded={onUploaded} />);

    // fireEvent statt userEvent.upload, da Letzteres per accept-Attribut filtert.
    fireEvent.change(screen.getByTestId('image-file-input'), {
      target: { files: [fileOfSize('x.pdf', 'application/pdf', 100)] },
    });

    expect(await screen.findByRole('alert')).toHaveTextContent(/nicht unterstützt/);
    expect(upload).not.toHaveBeenCalled();
    expect(onUploaded).not.toHaveBeenCalled();
  });

  it('lehnt eine zu große Datei ab (>5 MB, kein Upload)', async () => {
    const onUploaded = vi.fn();
    const user = userEvent.setup();
    render(<ImageUploader label="Bild hochladen" onUploaded={onUploaded} />);

    await user.upload(
      screen.getByTestId('image-file-input'),
      fileOfSize('big.png', 'image/png', 6 * 1024 * 1024),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent(/zu groß/);
    expect(upload).not.toHaveBeenCalled();
  });

  it('zeigt einen Fehler, wenn der Upload scheitert', async () => {
    upload.mockRejectedValue(new Error('boom'));
    const user = userEvent.setup();
    render(<ImageUploader label="Bild hochladen" onUploaded={vi.fn()} />);

    await user.upload(
      screen.getByTestId('image-file-input'),
      fileOfSize('x.png', 'image/png', 1234),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent(/fehlgeschlagen/);
  });

  it('referenziert ein existierendes Duplikat statt erneut hochzuladen (#199)', async () => {
    hashOf.mockResolvedValue('b'.repeat(64));
    checkHash.mockResolvedValue({ exists: true, id: 9 });
    const onUploaded = vi.fn();
    const user = userEvent.setup();
    render(<ImageUploader label="Bild hochladen" onUploaded={onUploaded} />);

    await user.upload(
      screen.getByTestId('image-file-input'),
      fileOfSize('dup.png', 'image/png', 1234),
    );

    await waitFor(() => expect(onUploaded).toHaveBeenCalledWith({ id: 9, url: '/api/images/9' }));
    expect(upload).not.toHaveBeenCalled();
  });

  it('lädt ein Bild aus der Galerie ohne erneuten Upload (#199)', async () => {
    list.mockResolvedValue({
      images: [
        { id: 5, contentType: 'image/png', sizeBytes: 10, createdAt: 'x', hash: null },
      ],
      total: 1,
    });
    const onUploaded = vi.fn();
    const user = userEvent.setup();
    render(<ImageUploader label="Bild hochladen" onUploaded={onUploaded} />);

    await user.click(screen.getByRole('button', { name: 'Aus Datenbank laden' }));
    await user.click(await screen.findByRole('button', { name: 'Bild 5 auswählen' }));

    expect(onUploaded).toHaveBeenCalledWith({ id: 5, url: '/api/images/5' });
    expect(upload).not.toHaveBeenCalled();
  });
});
