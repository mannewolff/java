import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';

import KanbanAttachmentList from './KanbanAttachmentList';
import type { KanbanAttachmentMeta } from '../../api/kanbanAttachments';
import { ApiError } from '../../api/client';

vi.mock('../../api/kanbanAttachments', async () => {
  const actual =
    await vi.importActual<typeof import('../../api/kanbanAttachments')>('../../api/kanbanAttachments');
  return {
    ...actual,
    listAttachments: vi.fn(),
    uploadAttachment: vi.fn(),
    downloadAttachment: vi.fn(),
    deleteAttachment: vi.fn(),
    fetchAttachmentBlob: vi.fn(),
  };
});

import {
  MAX_ATTACHMENT_BYTES,
  deleteAttachment,
  fetchAttachmentBlob,
  listAttachments,
  uploadAttachment,
} from '../../api/kanbanAttachments';

const list = listAttachments as ReturnType<typeof vi.fn>;
const upload = uploadAttachment as ReturnType<typeof vi.fn>;
const del = deleteAttachment as ReturnType<typeof vi.fn>;
const fetchBlob = fetchAttachmentBlob as ReturnType<typeof vi.fn>;

const ITEM = 5;

function meta(overrides: Partial<KanbanAttachmentMeta> = {}): KanbanAttachmentMeta {
  return {
    id: 1,
    filename: 'notes.txt',
    contentType: 'text/plain',
    sizeBytes: 2048,
    uploadedBy: 'sub-1',
    createdAt: '2026-07-07T10:00:00Z',
    ...overrides,
  };
}

function fileInput(): HTMLInputElement {
  return screen.getByLabelText('Datei anhängen') as HTMLInputElement;
}

describe('KanbanAttachmentList', () => {
  beforeEach(() => {
    list.mockReset();
    list.mockResolvedValue([]);
    upload.mockReset();
    upload.mockResolvedValue(meta());
    del.mockReset();
    del.mockResolvedValue(undefined);
    fetchBlob.mockReset();
  });

  afterEach(() => cleanup());

  it('rendert die Anhang-Metadaten (Name und Größe)', async () => {
    list.mockResolvedValue([meta({ id: 7, filename: 'doc.pdf', sizeBytes: 3 * 1024 * 1024 })]);
    render(<KanbanAttachmentList itemId={ITEM} />);

    expect(await screen.findByText('doc.pdf')).toBeInTheDocument();
    expect(screen.getByText('3.0 MB')).toBeInTheDocument();
  });

  it('zeigt einen Empty-State, wenn keine Anhänge existieren', async () => {
    render(<KanbanAttachmentList itemId={ITEM} />);
    expect(await screen.findByText('Keine Anhänge')).toBeInTheDocument();
  });

  it('lädt eine gewählte Datei hoch und aktualisiert die Liste', async () => {
    list.mockResolvedValueOnce([]); // initial
    list.mockResolvedValueOnce([meta({ id: 9, filename: 'upload.txt' })]); // nach Upload
    render(<KanbanAttachmentList itemId={ITEM} />);
    await screen.findByText('Keine Anhänge');

    const file = new File(['hello'], 'upload.txt', { type: 'text/plain' });
    fireEvent.change(fileInput(), { target: { files: [file] } });

    await waitFor(() => expect(upload).toHaveBeenCalledWith(ITEM, file));
    expect(await screen.findByText('upload.txt')).toBeInTheDocument();
  });

  it('lehnt eine zu große Datei ohne Upload ab', async () => {
    render(<KanbanAttachmentList itemId={ITEM} />);
    await screen.findByText('Keine Anhänge');

    const big = new File([new Uint8Array(MAX_ATTACHMENT_BYTES + 1)], 'big.bin');
    fireEvent.change(fileInput(), { target: { files: [big] } });

    expect(await screen.findByText(/zu groß/)).toBeInTheDocument();
    expect(upload).not.toHaveBeenCalled();
  });

  it('deaktiviert den Upload-Button und zeigt einen Hinweis bei 5 Anhängen', async () => {
    list.mockResolvedValue(Array.from({ length: 5 }, (_, i) => meta({ id: i + 1, filename: `f${i}.txt` })));
    render(<KanbanAttachmentList itemId={ITEM} />);

    await screen.findByText('f0.txt');
    expect(screen.getByRole('button', { name: 'Datei anhängen' })).toBeDisabled();
    expect(screen.getByText(/Maximal 5 Anhänge erreicht/)).toBeInTheDocument();
  });

  it('löscht einen Anhang und aktualisiert die Liste', async () => {
    list.mockResolvedValueOnce([meta({ id: 7, filename: 'weg.txt' })]);
    list.mockResolvedValueOnce([]);
    render(<KanbanAttachmentList itemId={ITEM} />);

    await screen.findByText('weg.txt');
    fireEvent.click(screen.getByRole('button', { name: 'Löschen: weg.txt' }));

    await waitFor(() => expect(del).toHaveBeenCalledWith(ITEM, 7));
    await waitFor(() => expect(screen.queryByText('weg.txt')).not.toBeInTheDocument());
  });

  it('zeigt bei Upload-Fehler eine Meldung (kein Crash)', async () => {
    upload.mockRejectedValue(new Error('boom'));
    render(<KanbanAttachmentList itemId={ITEM} />);
    await screen.findByText('Keine Anhänge');

    const file = new File(['x'], 'a.txt', { type: 'text/plain' });
    fireEvent.change(fileInput(), { target: { files: [file] } });

    expect(await screen.findByText('Upload fehlgeschlagen.')).toBeInTheDocument();
  });

  it('zeigt das Vorschau-Icon nur für vorschaufähige Anhänge (#360)', async () => {
    list.mockResolvedValue([
      meta({ id: 1, filename: 'doc.pdf', contentType: 'application/pdf' }),
      meta({ id: 2, filename: 'notes.txt', contentType: 'text/plain' }),
    ]);
    render(<KanbanAttachmentList itemId={ITEM} />);

    await screen.findByText('doc.pdf');
    expect(screen.getByRole('button', { name: 'Vorschau: doc.pdf' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Vorschau: notes.txt' })).not.toBeInTheDocument();
  });

  it('öffnet die Vollbild-Vorschau beim Klick auf das Vorschau-Icon (#360)', async () => {
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:mock'),
      revokeObjectURL: vi.fn(),
    });
    fetchBlob.mockResolvedValue(new Blob([new Uint8Array([1])], { type: 'image/png' }));
    list.mockResolvedValue([meta({ id: 3, filename: 'bild.png', contentType: 'image/png' })]);
    render(<KanbanAttachmentList itemId={ITEM} />);

    await screen.findByText('bild.png');
    fireEvent.click(screen.getByRole('button', { name: 'Vorschau: bild.png' }));

    expect(await screen.findByRole('button', { name: 'Vorschau schließen' })).toBeInTheDocument();
    vi.unstubAllGlobals();
  });

  it('meldet das 5er-Limit, wenn das Backend mit 409 antwortet', async () => {
    upload.mockRejectedValue(new ApiError(409, 'limit', null));
    render(<KanbanAttachmentList itemId={ITEM} />);
    await screen.findByText('Keine Anhänge');

    const file = new File(['x'], 'a.txt', { type: 'text/plain' });
    fireEvent.change(fileInput(), { target: { files: [file] } });

    expect(await screen.findByText(/Maximal 5 Anhänge pro Eintrag/)).toBeInTheDocument();
  });
});
