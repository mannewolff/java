import { useCallback, useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { Alert, Box, Button, CircularProgress, IconButton, Stack, Typography } from '@mui/material';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import DownloadIcon from '@mui/icons-material/Download';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';

import { ApiError } from '../../api/client';
import {
  MAX_ATTACHMENTS_PER_ITEM,
  MAX_ATTACHMENT_BYTES,
  deleteAttachment,
  downloadAttachment,
  listAttachments,
  uploadAttachment,
  type KanbanAttachmentMeta,
} from '../../api/kanbanAttachments';

/** Formatiert eine Byte-Größe menschenlesbar (KB/MB). */
function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * Anhang-Sektion eines Kanban-Eintrags (Item oder Epic, #351): listet die Anhänge, erlaubt Upload
 * beliebiger Dateien (max. 10 MB, max. 5 pro Eintrag), Download und Löschen. Selbstständig — lädt
 * die eigene Liste anhand der {@code itemId}.
 */
export default function KanbanAttachmentList({ itemId }: { itemId: number }): JSX.Element {
  const [attachments, setAttachments] = useState<KanbanAttachmentMeta[]>([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const reload = useCallback(async (): Promise<void> => {
    setAttachments(await listAttachments(itemId));
  }, [itemId]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    listAttachments(itemId)
      .then((loaded) => {
        if (!cancelled) setAttachments(loaded);
      })
      .catch(() => {
        if (!cancelled) setError('Anhänge konnten nicht geladen werden.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [itemId]);

  const atLimit = attachments.length >= MAX_ATTACHMENTS_PER_ITEM;

  const handleFile = async (event: ChangeEvent<HTMLInputElement>): Promise<void> => {
    const file = event.target.files?.[0];
    // Zurücksetzen, damit dieselbe Datei erneut ausgewählt werden kann.
    event.target.value = '';
    if (!file) return;
    if (file.size > MAX_ATTACHMENT_BYTES) {
      setError(`Datei zu groß (max. ${MAX_ATTACHMENT_BYTES / 1024 / 1024} MB).`);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await uploadAttachment(itemId, file);
      await reload();
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 409
          ? `Maximal ${MAX_ATTACHMENTS_PER_ITEM} Anhänge pro Eintrag.`
          : 'Upload fehlgeschlagen.',
      );
    } finally {
      setBusy(false);
    }
  };

  const handleDownload = async (attachment: KanbanAttachmentMeta): Promise<void> => {
    setError(null);
    try {
      await downloadAttachment(itemId, attachment.id, attachment.filename);
    } catch {
      setError('Download fehlgeschlagen.');
    }
  };

  const handleDelete = async (id: number): Promise<void> => {
    setError(null);
    try {
      await deleteAttachment(itemId, id);
      await reload();
    } catch {
      setError('Löschen fehlgeschlagen.');
    }
  };

  return (
    <Stack spacing={1.5}>
      <Typography variant="subtitle1">Anhänge</Typography>
      {error && <Alert severity="error">{error}</Alert>}

      {loading ? (
        <Stack alignItems="center" sx={{ py: 2 }}>
          <CircularProgress size={24} aria-label="Anhänge werden geladen" />
        </Stack>
      ) : attachments.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          Keine Anhänge
        </Typography>
      ) : (
        <Stack spacing={0.5}>
          {attachments.map((a) => (
            <Box
              key={a.id}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                px: 1,
                py: 0.5,
              }}
            >
              <Typography variant="body2" sx={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {a.filename}
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ flexShrink: 0 }}>
                {formatBytes(a.sizeBytes)}
              </Typography>
              <IconButton
                size="small"
                aria-label={`Herunterladen: ${a.filename}`}
                onClick={() => void handleDownload(a)}
              >
                <DownloadIcon fontSize="small" />
              </IconButton>
              <IconButton
                size="small"
                aria-label={`Löschen: ${a.filename}`}
                onClick={() => void handleDelete(a.id)}
              >
                <DeleteOutlineIcon fontSize="small" />
              </IconButton>
            </Box>
          ))}
        </Stack>
      )}

      <Box>
        <input
          ref={inputRef}
          type="file"
          hidden
          aria-label="Datei anhängen"
          onChange={(e) => void handleFile(e)}
        />
        <Button
          size="small"
          variant="outlined"
          startIcon={busy ? <CircularProgress size={16} /> : <AttachFileIcon />}
          disabled={atLimit || busy}
          onClick={() => inputRef.current?.click()}
        >
          Datei anhängen
        </Button>
        {atLimit && (
          <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
            Maximal {MAX_ATTACHMENTS_PER_ITEM} Anhänge erreicht.
          </Typography>
        )}
      </Box>
    </Stack>
  );
}
