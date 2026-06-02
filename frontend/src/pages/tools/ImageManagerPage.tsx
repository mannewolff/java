import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  Paper,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';

import { ApiError } from '../../api/client';
import {
  deleteImages,
  fetchThumbnailObjectUrl,
  listManagedImages,
  type ManagedImage,
} from '../../api/images';
import { useNotify } from '../../notify/NotifyProvider';

const PAGE_SIZE = 24;

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString();
}

/**
 * Image-Manager (#202): listet gespeicherte Bilder mit Metadaten und Verwendungs-Info, erlaubt das
 * Batch-Löschen ungenutzter Bilder. Bilder, die in Widgets benutzt werden, sind nicht löschbar
 * (Checkbox deaktiviert); der Lösch-Schutz wird zusätzlich serverseitig erzwungen.
 */
export default function ImageManagerPage(): JSX.Element {
  const notify = useNotify();
  const [items, setItems] = useState<ManagedImage[]>([]);
  const [total, setTotal] = useState(0);
  const [offset, setOffset] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    listManagedImages(PAGE_SIZE, offset)
      .then((res) => {
        if (cancelled) return;
        setItems(res.images);
        setTotal(res.total);
        setSelected(new Set());
      })
      .catch((e) => {
        if (!cancelled) {
          setError(e instanceof ApiError ? e.message : 'Bilder konnten nicht geladen werden');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [offset]);

  useEffect(() => load(), [load]);

  const deletable = items.filter((i) => i.usageCount === 0);
  const allDeletableSelected =
    deletable.length > 0 && deletable.every((i) => selected.has(i.id));

  function toggle(id: number): void {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleAll(): void {
    setSelected(allDeletableSelected ? new Set() : new Set(deletable.map((i) => i.id)));
  }

  async function confirmDelete(): Promise<void> {
    setConfirmOpen(false);
    setDeleting(true);
    try {
      const result = await deleteImages([...selected]);
      if (result.deleted.length > 0) {
        notify.success(`${result.deleted.length} Bild(er) gelöscht`);
      }
      if (result.failed.length > 0) {
        notify.warning(`${result.failed.length} Bild(er) konnten nicht gelöscht werden (benutzt)`);
      }
      load();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Löschen fehlgeschlagen');
    } finally {
      setDeleting(false);
    }
  }

  const hasPrev = offset > 0;
  const hasNext = offset + items.length < total;

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Bilder verwalten
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 2 }}>
        Ungenutzte Bilder auswählen und löschen. In Widgets verwendete Bilder sind geschützt.
      </Typography>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress aria-label="Bilder laden" />
        </Box>
      ) : error ? (
        <Alert severity="error" action={<Button onClick={load}>Erneut</Button>}>
          {error}
        </Alert>
      ) : items.length === 0 ? (
        <Typography color="text.secondary" sx={{ p: 2 }}>
          Keine Bilder vorhanden.
        </Typography>
      ) : (
        <>
          <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={allDeletableSelected}
                  indeterminate={!allDeletableSelected && selected.size > 0}
                  onChange={toggleAll}
                  disabled={deletable.length === 0}
                  inputProps={{ 'aria-label': 'Alle löschbaren auswählen' }}
                />
              }
              label="Alle auswählen"
            />
            <Button
              variant="contained"
              color="error"
              startIcon={deleting ? <CircularProgress size={16} color="inherit" /> : <DeleteIcon />}
              disabled={selected.size === 0 || deleting}
              onClick={() => setConfirmOpen(true)}
            >
              Löschen{selected.size > 0 ? ` (${selected.size})` : ''}
            </Button>
          </Stack>

          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
            {items.map((img) => (
              <ImageCard
                key={img.id}
                image={img}
                checked={selected.has(img.id)}
                onToggle={() => toggle(img.id)}
              />
            ))}
          </Box>

          {(hasPrev || hasNext) && (
            <Stack direction="row" spacing={1} justifyContent="center" sx={{ mt: 2 }}>
              <Button
                size="small"
                disabled={!hasPrev}
                onClick={() => setOffset((o) => Math.max(0, o - PAGE_SIZE))}
              >
                Zurück
              </Button>
              <Button size="small" disabled={!hasNext} onClick={() => setOffset((o) => o + PAGE_SIZE)}>
                Weiter
              </Button>
            </Stack>
          )}
        </>
      )}

      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>Bilder löschen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {selected.size} Bild(er) löschen? Das kann nicht rückgängig gemacht werden.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Abbrechen</Button>
          <Button color="error" onClick={() => void confirmDelete()}>
            Löschen
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

interface ImageCardProps {
  image: ManagedImage;
  checked: boolean;
  onToggle: () => void;
}

function ImageCard({ image, checked, onToggle }: ImageCardProps): JSX.Element {
  const [url, setUrl] = useState<string | null>(null);
  const used = image.usageCount > 0;

  useEffect(() => {
    let active = true;
    let created: string | null = null;
    fetchThumbnailObjectUrl(image.id)
      .then((u) => {
        if (active) {
          created = u;
          setUrl(u);
        } else {
          URL.revokeObjectURL(u);
        }
      })
      .catch(() => {
        /* fehlerhafte Einzelbilder still auslassen */
      });
    return () => {
      active = false;
      if (created) URL.revokeObjectURL(created);
    };
  }, [image.id]);

  return (
    <Paper variant="outlined" sx={{ p: 1, width: 160 }}>
      <Stack spacing={0.5}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Tooltip title={used ? `Benutzt in ${image.usageCount} Widget(s)` : 'Nicht benutzt'}>
            <span>
              <Checkbox
                size="small"
                checked={checked}
                disabled={used}
                onChange={onToggle}
                sx={{ p: 0.5 }}
                inputProps={{ 'aria-label': `Bild ${image.id} auswählen` }}
              />
            </span>
          </Tooltip>
          <Typography
            variant="caption"
            color={used ? 'warning.main' : 'success.main'}
            sx={{ textAlign: 'right' }}
          >
            {used ? `Benutzt (${image.usageCount})` : 'Nicht benutzt'}
          </Typography>
        </Box>
        <Box
          sx={{
            width: '100%',
            height: 120,
            bgcolor: 'action.hover',
            borderRadius: 1,
            overflow: 'hidden',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {url && (
            <Box
              component="img"
              src={url}
              alt=""
              sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          )}
        </Box>
        <Typography variant="caption" color="text.secondary">
          {formatSize(image.sizeBytes)} · {formatDate(image.createdAt)}
        </Typography>
      </Stack>
    </Paper>
  );
}
