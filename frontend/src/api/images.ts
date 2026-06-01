import { ApiError, authedFetch } from './client';

export interface UploadedImageInfo {
  id: number;
  url: string;
}

/** Erlaubte Upload-Formate (deckt sich mit dem Backend-Whitelist, #182). */
export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
/** Maximale Upload-Größe: 5 MB (Backend lehnt Größeres mit 413 ab). */
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/** Lädt eine Bilddatei zum Image-Store hoch und liefert id + URL zurück. */
export async function uploadImage(file: File): Promise<UploadedImageInfo> {
  const form = new FormData();
  form.append('file', file);
  const response = await authedFetch('/api/images', { method: 'POST', body: form });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      body?.message ?? `Upload fehlgeschlagen (${response.status})`,
      body,
    );
  }
  return (await response.json()) as UploadedImageInfo;
}

/**
 * Lädt ein Bild authentifiziert als Blob und erzeugt eine Object-URL. Der Serve-Endpoint ist
 * bearer-only (#182), daher kein direktes `<img src>`. Aufrufer müssen die URL nach Gebrauch
 * via {@link URL.revokeObjectURL} freigeben.
 */
export async function fetchImageObjectUrl(id: number): Promise<string> {
  const response = await authedFetch(`/api/images/${id}`);
  if (!response.ok) {
    throw new ApiError(response.status, `Bild konnte nicht geladen werden (${response.status})`, null);
  }
  const blob = await response.blob();
  return URL.createObjectURL(blob);
}
