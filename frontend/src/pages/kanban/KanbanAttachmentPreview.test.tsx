import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import KanbanAttachmentPreview from './KanbanAttachmentPreview';
import type { KanbanAttachmentMeta } from '../../api/kanbanAttachments';

vi.mock('../../api/kanbanAttachments', async () => {
  const actual =
    await vi.importActual<typeof import('../../api/kanbanAttachments')>('../../api/kanbanAttachments');
  return { ...actual, fetchAttachmentBlob: vi.fn() };
});

import { fetchAttachmentBlob, previewKind } from '../../api/kanbanAttachments';

const fetchBlob = fetchAttachmentBlob as ReturnType<typeof vi.fn>;

function meta(overrides: Partial<KanbanAttachmentMeta> = {}): KanbanAttachmentMeta {
  return {
    id: 1,
    filename: 'bild.png',
    contentType: 'image/png',
    sizeBytes: 100,
    uploadedBy: 'sub-1',
    createdAt: '2026-07-08T10:00:00Z',
    ...overrides,
  };
}

describe('previewKind (#360)', () => {
  it('erkennt Bilder über den sniffed contentType', () => {
    expect(previewKind(meta({ contentType: 'image/png' }))).toBe('image');
    expect(previewKind(meta({ contentType: 'image/jpeg' }))).toBe('image');
  });

  it('erkennt PDF', () => {
    expect(previewKind(meta({ filename: 'a.pdf', contentType: 'application/pdf' }))).toBe('pdf');
  });

  it('erkennt Markdown über contentType oder über Endung bei text/plain', () => {
    expect(previewKind(meta({ filename: 'a.md', contentType: 'text/markdown' }))).toBe('markdown');
    expect(previewKind(meta({ filename: 'a.md', contentType: 'text/plain' }))).toBe('markdown');
    expect(previewKind(meta({ filename: 'a.markdown', contentType: 'text/plain' }))).toBe('markdown');
  });

  it('gibt für nicht vorschaufähige und getarnte Typen null zurück', () => {
    expect(previewKind(meta({ filename: 'a.zip', contentType: 'application/zip' }))).toBeNull();
    // Als .png getarnte HTML-Datei: sniffed als text/html → keine Bild-Vorschau.
    expect(previewKind(meta({ filename: 'evil.png', contentType: 'text/html' }))).toBeNull();
    // Plain-Text ohne Markdown-Endung ist keine Vorschau.
    expect(previewKind(meta({ filename: 'notes.txt', contentType: 'text/plain' }))).toBeNull();
  });
});

describe('KanbanAttachmentPreview (#360)', () => {
  beforeEach(() => {
    fetchBlob.mockReset();
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:mock-url'),
      revokeObjectURL: vi.fn(),
    });
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('rendert ein Bild als <img> mit Object-URL', async () => {
    fetchBlob.mockResolvedValue(new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' }));
    render(
      <KanbanAttachmentPreview itemId={5} attachment={meta()} onClose={vi.fn()} />,
    );

    const img = (await screen.findByAltText('bild.png')) as HTMLImageElement;
    expect(img.tagName).toBe('IMG');
    expect(img).toHaveAttribute('src', 'blob:mock-url');
  });

  it('rendert PDF in einem iframe', async () => {
    fetchBlob.mockResolvedValue(new Blob([new Uint8Array([1])], { type: 'application/pdf' }));
    render(
      <KanbanAttachmentPreview
        itemId={5}
        attachment={meta({ filename: 'doc.pdf', contentType: 'application/pdf' })}
        onClose={vi.fn()}
      />,
    );

    const frame = await screen.findByTitle('doc.pdf');
    expect(frame.tagName).toBe('IFRAME');
    expect(frame).toHaveAttribute('src', 'blob:mock-url');
  });

  it('rendert Markdown als gerenderten Text (keine Object-URL)', async () => {
    fetchBlob.mockResolvedValue({
      text: () => Promise.resolve('# Überschrift\nHallo'),
    } as unknown as Blob);
    render(
      <KanbanAttachmentPreview
        itemId={5}
        attachment={meta({ filename: 'a.md', contentType: 'text/markdown' })}
        onClose={vi.fn()}
      />,
    );

    expect(await screen.findByRole('heading', { name: 'Überschrift' })).toBeInTheDocument();
    expect(screen.getByText('Hallo')).toBeInTheDocument();
    expect(URL.createObjectURL).not.toHaveBeenCalled();
  });

  it('zeigt eine Fehlermeldung, wenn der Blob nicht geladen werden kann', async () => {
    fetchBlob.mockRejectedValue(new Error('boom'));
    render(
      <KanbanAttachmentPreview itemId={5} attachment={meta()} onClose={vi.fn()} />,
    );

    expect(await screen.findByText(/konnte nicht geladen werden/)).toBeInTheDocument();
  });

  it('gibt die Object-URL beim Unmount wieder frei', async () => {
    fetchBlob.mockResolvedValue(new Blob([new Uint8Array([1])], { type: 'image/png' }));
    const { unmount } = render(
      <KanbanAttachmentPreview itemId={5} attachment={meta()} onClose={vi.fn()} />,
    );

    await screen.findByAltText('bild.png');
    unmount();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
  });

  it('ruft onClose über den Schließen-Button', async () => {
    fetchBlob.mockResolvedValue(new Blob([new Uint8Array([1])], { type: 'image/png' }));
    const onClose = vi.fn();
    render(
      <KanbanAttachmentPreview itemId={5} attachment={meta()} onClose={onClose} />,
    );

    await screen.findByAltText('bild.png');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Vorschau schließen' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
