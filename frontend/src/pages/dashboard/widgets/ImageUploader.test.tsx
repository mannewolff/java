import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../../api/images', async () => {
  const actual = await vi.importActual<typeof import('../../../api/images')>('../../../api/images');
  return { ...actual, uploadImage: vi.fn() };
});

import { uploadImage } from '../../../api/images';
import ImageUploader from './ImageUploader';

const upload = uploadImage as ReturnType<typeof vi.fn>;

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
  afterEach(() => {
    cleanup();
    upload.mockReset();
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
});
