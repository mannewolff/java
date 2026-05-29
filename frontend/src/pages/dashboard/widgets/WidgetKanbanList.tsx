import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  Drawer,
  FormControlLabel,
  IconButton,
  Link,
  MenuItem,
  Paper,
  Stack,
  Switch,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';

import type { WidgetDto } from '../../../api/dashboard';
import {
  KANBAN_COLUMNS,
  getKanbanSettings,
  listKanbanItems,
  updateKanbanItem,
  type KanbanColumn,
  type KanbanItem,
} from '../../../api/kanban';
import { ApiError } from '../../../api/client';
import KanbanDetailModal from '../../kanban/KanbanDetailModal';
import { CONFIG_DRAWER_WIDTH } from './drawerConstants';
import { parseSurfaceConfig, widgetSurface } from './widgetSurface';

const COLUMN_LABELS: Record<KanbanColumn, string> = {
  BACKLOG: 'Backlog',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  DONE: 'Done',
};

const MIN_LIMIT = 1;
const MAX_LIMIT = 20;
const DEFAULT_LIMIT = 5;
const FALLBACK_RETENTION_DAYS = 5;

interface KanbanWidgetConfig {
  column: KanbanColumn;
  limit: number;
  showBorder: boolean;
  backgroundColor?: string;
}

function isColumn(v: unknown): v is KanbanColumn {
  return v === 'BACKLOG' || v === 'IN_PROGRESS' || v === 'IN_REVIEW' || v === 'DONE';
}

function clampLimit(value: unknown): number {
  const n = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
  if (!Number.isFinite(n)) return DEFAULT_LIMIT;
  return Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, Math.round(n)));
}

function parseConfig(raw: string): KanbanWidgetConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return {
      column: isColumn(parsed.column) ? parsed.column : 'BACKLOG',
      limit: clampLimit(parsed.limit),
      ...parseSurfaceConfig(parsed),
    };
  } catch {
    return { column: 'BACKLOG', limit: DEFAULT_LIMIT, showBorder: false };
  }
}

interface Props {
  widget: WidgetDto;
  onChange: (next: WidgetDto) => void;
  onDelete: () => void;
  readOnly?: boolean;
}

/**
 * Kanban-Vorschau-Widget: zeigt die obersten `limit` Items einer Spalte als klickbare
 * Titelliste. Ein Klick öffnet das wiederverwendete {@link KanbanDetailModal} (#119) —
 * auch im Read-Modus, damit Items direkt vom Dashboard aus bearbeitbar sind. Speichern
 * lädt die Liste neu.
 */
export default function WidgetKanbanList({
  widget,
  onChange,
  onDelete,
  readOnly = false,
}: Props): JSX.Element {
  const config = parseConfig(widget.config);
  const surface = widgetSurface(readOnly, config);
  const [items, setItems] = useState<KanbanItem[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [retentionDays, setRetentionDays] = useState(FALLBACK_RETENTION_DAYS);
  const [detailItem, setDetailItem] = useState<KanbanItem | null>(null);

  const [open, setOpen] = useState(false);
  const [draftColumn, setDraftColumn] = useState<KanbanColumn>(config.column);
  const [draftLimit, setDraftLimit] = useState(String(config.limit));
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

  const reload = useCallback(async (): Promise<void> => {
    setLoadError(null);
    try {
      const board = await listKanbanItems();
      const column = [...(board[config.column] ?? [])].sort((a, b) => a.position - b.position);
      setItems(column.slice(0, config.limit));
    } catch (e) {
      setLoadError(e instanceof ApiError ? e.message : 'Laden fehlgeschlagen');
      setItems([]);
    }
  }, [config.column, config.limit]);

  useEffect(() => {
    void reload();
  }, [reload]);

  useEffect(() => {
    getKanbanSettings()
      .then((s) => setRetentionDays(s.doneRetentionDays))
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    if (open) {
      setDraftColumn(config.column);
      setDraftLimit(String(config.limit));
      setDraftShowBorder(config.showBorder);
      setDraftBackgroundColor(config.backgroundColor ?? '');
    }
  }, [open, config.column, config.limit, config.showBorder, config.backgroundColor]);

  function handleApply(): void {
    const next: KanbanWidgetConfig = {
      column: draftColumn,
      limit: clampLimit(draftLimit),
      showBorder: draftShowBorder,
      ...(draftBackgroundColor.trim() !== ''
        ? { backgroundColor: draftBackgroundColor.trim() }
        : {}),
    };
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  async function handleDetailSubmit(title: string, body: string): Promise<void> {
    if (!detailItem) return;
    await updateKanbanItem(detailItem.id, title, body);
    setDetailItem(null);
    await reload();
  }

  return (
    <Paper
      variant={surface.variant}
      elevation={surface.elevation}
      sx={{
        p: 1.5,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        ...surface.sx,
      }}
    >
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 0.5 }}>
        <Typography variant="subtitle2" noWrap>
          {COLUMN_LABELS[config.column]}
        </Typography>
        {!readOnly && (
          <Stack direction="row" spacing={0.5}>
            <IconButton
              size="small"
              aria-label="Kanban-Liste bearbeiten"
              onClick={() => setOpen(true)}
              onMouseDown={(e) => e.stopPropagation()}
            >
              <EditIcon fontSize="small" />
            </IconButton>
            <IconButton
              size="small"
              aria-label="Kanban-Liste löschen"
              onClick={onDelete}
              onMouseDown={(e) => e.stopPropagation()}
            >
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Stack>
        )}
      </Stack>

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {loadError ? (
          <Alert severity="error">{loadError}</Alert>
        ) : items === null ? (
          <Stack alignItems="center" justifyContent="center" sx={{ height: '100%' }}>
            <CircularProgress size={24} aria-label="Kanban-Items werden geladen" />
          </Stack>
        ) : items.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Keine Einträge
          </Typography>
        ) : (
          <Stack component="ul" spacing={0.5} sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {items.map((item) => (
              <Box component="li" key={item.id}>
                <Link
                  component="button"
                  type="button"
                  variant="body2"
                  underline="hover"
                  onClick={() => setDetailItem(item)}
                  onMouseDown={(e) => e.stopPropagation()}
                  sx={{ textAlign: 'left', display: 'block', width: '100%' }}
                >
                  {item.title}
                </Link>
              </Box>
            ))}
          </Stack>
        )}
      </Box>

      {detailItem != null && (
        <KanbanDetailModal
          open
          item={detailItem}
          retentionDays={retentionDays}
          onClose={() => setDetailItem(null)}
          onSubmit={handleDetailSubmit}
        />
      )}

      <Drawer
        anchor="right"
        open={open}
        onClose={() => setOpen(false)}
        PaperProps={{ sx: { width: CONFIG_DRAWER_WIDTH } }}
      >
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Kanban-Liste bearbeiten
          </Typography>
          <Stack spacing={3}>
            <TextField
              label="Spalte"
              select
              value={draftColumn}
              onChange={(e) => setDraftColumn(e.target.value as KanbanColumn)}
              fullWidth
            >
              {KANBAN_COLUMNS.map((c) => (
                <MenuItem key={c} value={c}>
                  {COLUMN_LABELS[c]}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Anzahl (1–20)"
              type="number"
              value={draftLimit}
              onChange={(e) => setDraftLimit(e.target.value)}
              inputProps={{ min: MIN_LIMIT, max: MAX_LIMIT, 'aria-label': 'Anzahl' }}
              fullWidth
            />
            <Divider textAlign="left">Darstellung</Divider>
            <FormControlLabel
              control={
                <Switch
                  checked={draftShowBorder}
                  onChange={(e) => setDraftShowBorder(e.target.checked)}
                />
              }
              label="Rahmen anzeigen"
            />
            <TextField
              label="Hintergrundfarbe (leer = transparent)"
              value={draftBackgroundColor}
              onChange={(e) => setDraftBackgroundColor(e.target.value)}
              fullWidth
              placeholder="z. B. #1e1e1e oder rgba(255,255,255,0.05)"
            />
            <Divider />
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button onClick={() => setOpen(false)}>Abbrechen</Button>
              <Button variant="contained" onClick={handleApply}>
                Übernehmen
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Drawer>
    </Paper>
  );
}
