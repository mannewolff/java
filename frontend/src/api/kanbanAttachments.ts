import { ApiError, api, authedFetch } from './client';

/** Metadaten eines Kanban-Anhangs ohne die Binärdaten (#351). */
export interface KanbanAttachmentMeta {
  id: number;
  filename: string;
  contentType: string;
  sizeBytes: number;
  uploadedBy: string;
  createdAt: string;
}

/** Maximale Upload-Größe: 10 MB (Backend lehnt Größeres ab). */
export const MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024;

/** Maximale Anzahl Anhänge pro Eintrag (Backend antwortet mit 409 beim Überschreiten). */
export const MAX_ATTACHMENTS_PER_ITEM = 5;

const base = (itemId: number): string => `/kanban/items/${itemId}/attachments`;

/** Listet die Anhang-Metadaten eines Eintrags. */
export function listAttachments(itemId: number): Promise<KanbanAttachmentMeta[]> {
  return api.get<KanbanAttachmentMeta[]>(base(itemId));
}

/** Lädt eine beliebige Datei als Anhang hoch und liefert die Metadaten des gespeicherten Anhangs. */
export async function uploadAttachment(
  itemId: number,
  file: File,
): Promise<KanbanAttachmentMeta> {
  const form = new FormData();
  form.append('file', file);
  const response = await authedFetch(`/api${base(itemId)}`, { method: 'POST', body: form });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      body?.message ?? `Upload fehlgeschlagen (${response.status})`,
      body,
    );
  }
  return (await response.json()) as KanbanAttachmentMeta;
}

/**
 * Lädt einen Anhang authentifiziert als Blob. Der Serve-Endpoint ist bearer-only (kein direktes
 * `<a href>`), daher der Umweg über `authedFetch`. Der `Content-Disposition: attachment`-Header des
 * Servers ist für einen fetch irrelevant — die Verwendung (Download vs. Vorschau) entscheidet der
 * Aufrufer über die selbst erzeugte Object-URL bzw. den Blob (#360).
 */
export async function fetchAttachmentBlob(itemId: number, id: number): Promise<Blob> {
  const response = await authedFetch(
    `/api${base(itemId)}/${id}`,
    {},
    { suppressAuthExpired: true },
  );
  if (!response.ok) {
    throw new ApiError(response.status, `Laden fehlgeschlagen (${response.status})`, null);
  }
  return response.blob();
}

/**
 * Lädt einen Anhang authentifiziert als Blob und löst im Browser einen Download mit dem
 * Original-Dateinamen aus.
 */
export async function downloadAttachment(
  itemId: number,
  id: number,
  filename: string,
): Promise<void> {
  const blob = await fetchAttachmentBlob(itemId, id);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

/** Vorschaufähige Anhang-Arten (#360). */
export type PreviewKind = 'image' | 'pdf' | 'markdown';

/**
 * Entscheidet, ob und wie ein Anhang in der Vollbild-Vorschau darstellbar ist (#360). Bild und PDF
 * werden strikt über den serverseitig per Magic-Bytes ermittelten `contentType` erkannt (eine als
 * `.png` getarnte HTML-Datei kommt als `text/html` und fällt heraus). Markdown zusätzlich über die
 * Dateiendung, weil Tika `.md` häufig als `text/plain` meldet. Alles andere: keine Vorschau.
 */
export function previewKind(meta: KanbanAttachmentMeta): PreviewKind | null {
  const type = meta.contentType.toLowerCase();
  if (type === 'image/png' || type === 'image/jpeg') return 'image';
  if (type === 'application/pdf') return 'pdf';
  const isMarkdownExt = /\.(md|markdown)$/i.test(meta.filename);
  if (type === 'text/markdown' || (type.startsWith('text/') && isMarkdownExt)) return 'markdown';
  return null;
}

/** Löscht einen Anhang. */
export function deleteAttachment(itemId: number, id: number): Promise<void> {
  return api.del(`${base(itemId)}/${id}`);
}
