import { afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('./client', async () => {
  const actual = await vi.importActual<typeof import('./client')>('./client');
  return { ...actual, authedFetch: vi.fn() };
});

import { authedFetch, ApiError } from './client';
import {
  checkImageHash,
  deleteImages,
  fetchImageFile,
  fetchImageObjectUrl,
  fetchThumbnailObjectUrl,
  listImages,
  listManagedImages,
  sha256Hex,
  uploadImage,
} from './images';

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
    // #233: passiver Resource-Fetch unterdrückt den globalen Re-Login-Trigger.
    expect(fetchMock).toHaveBeenCalledWith('/api/images/5', {}, { suppressAuthExpired: true });
    spy.mockRestore();
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 404 });
    await expect(fetchImageObjectUrl(9)).rejects.toBeInstanceOf(ApiError);
  });
});

describe('fetchThumbnailObjectUrl (#200)', () => {
  afterEach(() => fetchMock.mockReset());

  it('lädt den Thumbnail-Endpoint mit size-Parameter', async () => {
    const spy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:thumb');
    fetchMock.mockResolvedValue({ ok: true, blob: async () => new Blob(['x']) });

    await expect(fetchThumbnailObjectUrl(5)).resolves.toBe('blob:thumb');
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/images/5/thumbnail?size=160',
      {},
      { suppressAuthExpired: true },
    );
    spy.mockRestore();
  });

  it('reicht eine abweichende Größe durch', async () => {
    const spy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:thumb');
    fetchMock.mockResolvedValue({ ok: true, blob: async () => new Blob(['x']) });

    await fetchThumbnailObjectUrl(5, 64);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/images/5/thumbnail?size=64',
      {},
      { suppressAuthExpired: true },
    );
    spy.mockRestore();
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 404 });
    await expect(fetchThumbnailObjectUrl(9)).rejects.toBeInstanceOf(ApiError);
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
    expect(fetchMock).toHaveBeenCalledWith('/api/images/7', {}, { suppressAuthExpired: true });
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 404 });
    await expect(fetchImageFile(9)).rejects.toBeInstanceOf(ApiError);
  });
});

describe('checkImageHash (#199)', () => {
  afterEach(() => fetchMock.mockReset());

  it('POSTet den Hash und liefert das Ergebnis', async () => {
    fetchMock.mockResolvedValue({ ok: true, json: async () => ({ exists: true, id: 3 }) });

    const res = await checkImageHash('abc');

    expect(res).toEqual({ exists: true, id: 3 });
    const [path, init] = fetchMock.mock.calls[0];
    expect(path).toBe('/api/images/check-hash');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ hash: 'abc' });
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 400 });
    await expect(checkImageHash('x')).rejects.toBeInstanceOf(ApiError);
  });
});

describe('listManagedImages (#202)', () => {
  afterEach(() => fetchMock.mockReset());

  it('lädt /manage mit limit/offset', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ({ images: [], total: 0 }),
    });

    await listManagedImages(24, 48);

    expect(fetchMock).toHaveBeenCalledWith('/api/images/manage?limit=24&offset=48');
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 500 });
    await expect(listManagedImages()).rejects.toBeInstanceOf(ApiError);
  });
});

describe('deleteImages (#202)', () => {
  afterEach(() => fetchMock.mockReset());

  it('POSTet die Ids und liefert deleted/failed', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: async () => ({ deleted: [1], failed: [{ id: 2, reason: 'IN_USE' }] }),
    });

    const res = await deleteImages([1, 2]);

    expect(res.deleted).toEqual([1]);
    expect(res.failed).toEqual([{ id: 2, reason: 'IN_USE' }]);
    const [path, init] = fetchMock.mock.calls[0];
    expect(path).toBe('/api/images/batch-delete');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ ids: [1, 2] });
  });

  it('wirft ApiError bei Fehlerstatus', async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 500 });
    await expect(deleteImages([1])).rejects.toBeInstanceOf(ApiError);
  });
});

describe('sha256Hex (#199)', () => {
  it('berechnet den bekannten SHA-256 von "abc"', async () => {
    const bytes = new Uint8Array([0x61, 0x62, 0x63]);
    const file = new File([bytes], 'abc.bin');
    // jsdom-File implementiert arrayBuffer() nicht — für den Test bereitstellen.
    Object.defineProperty(file, 'arrayBuffer', { value: async () => bytes.buffer });
    await expect(sha256Hex(file)).resolves.toBe(
      'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
    );
  });
});
