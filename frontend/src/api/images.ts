import { ApiError, authedFetch } from './client';

export interface UploadedImageInfo {
  id: number;
  url: string;
}

/** Metadaten eines gespeicherten Bildes ohne Binärdaten (#198). `hash` ist erst ab #199 gesetzt. */
export interface ImageMetadata {
  id: number;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
  hash: string | null;
}

/** Paginierte Galerie-Antwort des Backends (#198). */
export interface ImageListResponse {
  images: ImageMetadata[];
  total: number;
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

/** Listet Bild-Metadaten paginiert (ohne Binärdaten) für Galerie-Ansichten (#198). */
export async function listImages(limit?: number, offset?: number): Promise<ImageListResponse> {
  const search = new URLSearchParams();
  if (limit != null) search.set('limit', String(limit));
  if (offset != null) search.set('offset', String(offset));
  const query = search.toString();
  const response = await authedFetch(`/api/images${query ? `?${query}` : ''}`);
  if (!response.ok) {
    throw new ApiError(response.status, `Galerie konnte nicht geladen werden (${response.status})`, null);
  }
  return (await response.json()) as ImageListResponse;
}

/** Ergebnis der Duplikat-Erkennung (#199). */
export interface CheckHashResult {
  exists: boolean;
  id?: number | null;
}

/** Berechnet den SHA-256-Hash einer Datei client-seitig als Hex-String (#199). */
export async function sha256Hex(file: File): Promise<string> {
  const buffer = await file.arrayBuffer();
  const digest = await crypto.subtle.digest('SHA-256', buffer);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/** Fragt das Backend, ob bereits ein Bild mit diesem SHA-256-Hash existiert (#199). */
export async function checkImageHash(hash: string): Promise<CheckHashResult> {
  const response = await authedFetch('/api/images/check-hash', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ hash }),
  });
  if (!response.ok) {
    throw new ApiError(response.status, `Hash-Prüfung fehlgeschlagen (${response.status})`, null);
  }
  return (await response.json()) as CheckHashResult;
}

/**
 * Lädt ein gespeichertes Bild authentifiziert als {@link File}, damit es wie ein lokaler Upload
 * weiterverarbeitet werden kann (z. B. resizeImage). Der Serve-Endpoint ist bearer-only (#182).
 */
export async function fetchImageFile(id: number): Promise<File> {
  const response = await authedFetch(`/api/images/${id}`);
  if (!response.ok) {
    throw new ApiError(response.status, `Bild konnte nicht geladen werden (${response.status})`, null);
  }
  const blob = await response.blob();
  const ext = blob.type.split('/')[1] ?? 'bin';
  return new File([blob], `image-${id}.${ext}`, { type: blob.type });
}
