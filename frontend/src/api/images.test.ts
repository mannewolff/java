import { afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('./client', async () => {
  const actual = await vi.importActual<typeof import('./client')>('./client');
  return { ...actual, authedFetch: vi.fn() };
});

import { authedFetch, ApiError } from './client';
import { fetchImageFile, fetchImageObjectUrl, listImages, uploadImage } from './images';

const fetchMock = authedFetch as ReturnType<typeof vi.fn>;

if (typeof URL.createObjectURL !== 'function') {
  URL.createObjectURL = () => 'blob:created';
}

describe('uploadImage', () => {
  afterEach(() => fetchMock.mockReset());

  it('POSTet FormData und liefert id + url', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ({ id: 5, url: '/api/images/5' }),
    });

    const result = await uploadImage(new File(['x'], 'x.png', { type: 'image/png' }));

    expect(result).toEqual({ id: 5, url: '/api/images/5' });
    const [path, init] = fetchMock.mock.calls[0];
    expect(path).toBe('/api/images');
    expect(init.method).toBe('POST');
    expect(init.body).toBeInstanceOf(FormData);
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 413,
      json: async () => ({ message: 'zu groß' }),
    });

    await expect(uploadImage(new File(['x'], 'x.png', { type: 'image/png' }))).rejects.toBeInstanceOf(
      ApiError,
    );
  });
});

describe('fetchImageObjectUrl', () => {
  afterEach(() => fetchMock.mockReset());

  it('liefert eine Object-URL aus dem Blob', async () => {
    const spy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:abc');
    fetchMock.mockResolvedValue({ ok: true, blob: async () => new Blob(['x']) });

    await expect(fetchImageObjectUrl(5)).resolves.toBe('blob:abc');
    expect(fetchMock).toHaveBeenCalledWith('/api/images/5');
    spy.mockRestore();
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 404 });
    await expect(fetchImageObjectUrl(9)).rejects.toBeInstanceOf(ApiError);
  });
});

describe('listImages (#198)', () => {
  afterEach(() => fetchMock.mockReset());

  it('lädt eine Seite mit limit/offset und liefert images + total', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ({
        images: [{ id: 1, contentType: 'image/png', sizeBytes: 10, createdAt: 'x', hash: null }],
        total: 1,
      }),
    });

    const res = await listImages(24, 0);

    expect(res.total).toBe(1);
    expect(res.images).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledWith('/api/images?limit=24&offset=0');
  });

  it('lässt Query-Parameter weg, wenn nicht gesetzt', async () => {
    fetchMock.mockResolvedValue({ ok: true, json: async () => ({ images: [], total: 0 }) });

    await listImages();

    expect(fetchMock).toHaveBeenCalledWith('/api/images');
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 500 });
    await expect(listImages()).rejects.toBeInstanceOf(ApiError);
  });
});

describe('fetchImageFile (#198)', () => {
  afterEach(() => fetchMock.mockReset());

  it('liefert ein File mit Endung aus dem Content-Type', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      blob: async () => new Blob(['x'], { type: 'image/webp' }),
    });

    const file = await fetchImageFile(7);

    expect(file).toBeInstanceOf(File);
    expect(file.type).toBe('image/webp');
    expect(file.name).toBe('image-7.webp');
    expect(fetchMock).toHaveBeenCalledWith('/api/images/7');
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 404 });
    await expect(fetchImageFile(9)).rejects.toBeInstanceOf(ApiError);
  });
});
