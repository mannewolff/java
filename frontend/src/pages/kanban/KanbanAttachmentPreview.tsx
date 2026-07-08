import { useEffect, useState } from 'react';
import {
  Alert,
  AppBar,
  Box,
  CircularProgress,
  Dialog,
  IconButton,
  Toolbar,
  Typography,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

import { fetchAttachmentBlob, previewKind, type KanbanAttachmentMeta } from '../../api/kanbanAttachments';

interface Props {
  itemId: number;
  attachment: KanbanAttachmentMeta;
  onClose: () => void;
}

/**
 * Vollbild-Vorschau eines Anhangs (#360) für Bild (PNG/JPEG), PDF und Markdown. Lädt den Blob rein
 * clientseitig über {@link fetchAttachmentBlob}; Bild/PDF über eine selbst erzeugte Object-URL
 * (beim Schließen/Unmount via {@code URL.revokeObjectURL} freigegeben), Markdown als Text mit
 * {@code ReactMarkdown} (kein {@code dangerouslySetInnerHTML}). Der Backend-Endpoint bleibt bei
 * {@code Content-Disposition: attachment} — die Vorschau umgeht ihn bewusst nicht serverseitig.
 */
export default function KanbanAttachmentPreview({ itemId, attachment, onClose }: Props): JSX.Element {
  const kind = previewKind(attachment);
  const [url, setUrl] = useState<string | null>(null);
  const [text, setText] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    let objectUrl: string | null = null;
    setLoading(true);
    setError(null);
    fetchAttachmentBlob(itemId, attachment.id)
      .then(async (blob) => {
        if (cancelled) return;
        if (kind === 'markdown') {
          setText(await blob.text());
        } else {
          objectUrl = URL.createObjectURL(blob);
          setUrl(objectUrl);
        }
      })
      .catch(() => {
        if (!cancelled) setError('Vorschau konnte nicht geladen werden.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [itemId, attachment.id, kind]);

  return (
    <Dialog fullScreen open onClose={onClose} aria-labelledby="kanban-attachment-preview-title">
      <AppBar sx={{ position: 'relative' }} color="default" elevation={1}>
        <Toolbar>
          <Typography
            id="kanban-attachment-preview-title"
            variant="subtitle1"
            noWrap
            sx={{ flex: 1, minWidth: 0 }}
          >
            {attachment.filename}
          </Typography>
          <IconButton edge="end" onClick={onClose} aria-label="Vorschau schließen">
            <CloseIcon />
          </IconButton>
        </Toolbar>
      </AppBar>
      <Box sx={{ flex: 1, display: 'flex', overflow: 'auto' }}>
        {loading ? (
          <Box sx={{ m: 'auto' }}>
            <CircularProgress aria-label="Vorschau wird geladen" />
          </Box>
        ) : error ? (
          <Alert severity="error" sx={{ m: 2 }}>
            {error}
          </Alert>
        ) : kind === 'image' && url ? (
          <Box
            component="img"
            src={url}
            alt={attachment.filename}
            sx={{ m: 'auto', maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
          />
        ) : kind === 'pdf' && url ? (
          <Box
            component="iframe"
            src={url}
            title={attachment.filename}
            sx={{ flex: 1, width: '100%', height: '100%', border: 0 }}
          />
        ) : kind === 'markdown' && text != null ? (
          <Box sx={{ width: '100%', maxWidth: 900, mx: 'auto', p: 3 }}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{text}</ReactMarkdown>
          </Box>
        ) : null}
      </Box>
    </Dialog>
  );
}
