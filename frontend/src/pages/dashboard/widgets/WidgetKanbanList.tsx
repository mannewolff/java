import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  CircularProgress,
  Divider,
  Drawer,
  FormControlLabel,
  FormGroup,
  IconButton,
  Link,
  Paper,
  Stack,
  Switch,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import type { ComponentType } from 'react';
import type { SvgIconProps } from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import InboxIcon from '@mui/icons-material/Inbox';
import FlagIcon from '@mui/icons-material/Flag';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import VisibilityIcon from '@mui/icons-material/Visibility';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

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
import { useNotify } from '../../../notify/NotifyProvider';
import KanbanDetailModal from '../../kanban/KanbanDetailModal';
import { COLUMN_LABELS } from '../../kanban/columnMeta';
import { STATUS_COLORS } from '../../kanban/statusColors';
import { CONFIG_DRAWER_WIDTH } from './drawerConstants';
import { parseSurfaceConfig, widgetSurface } from './widgetSurface';

/**
 * Feste Anzeige-Reihenfolge der Spalten im Widget (#221): aktivste Spalte zuerst.
 * Unabhängig von KANBAN_COLUMNS (Board-Reihenfolge) — Done wird ans Ende gestellt.
 * Ready (GO-Warteschlange) steht zwischen In Progress und Backlog.
 */
const DISPLAY_ORDER: readonly KanbanColumn[] = [
  'IN_REVIEW',
  'IN_PROGRESS',
  'READY',
  'BACKLOG',
  'DONE',
];

/**
 * Status-Icon je Spalte (#191). Die Akzentfarbe kommt aus {@link STATUS_COLORS} (#288),
 * damit das Widget-Icon exakt der Board-Header-Farbe derselben Spalte entspricht.
 */
const COLUMN_ICON: Record<KanbanColumn, ComponentType<SvgIconProps>> = {
  BACKLOG: InboxIcon,
  READY: FlagIcon,
  IN_PROGRESS: PlayArrowIcon,
  IN_REVIEW: VisibilityIcon,
  DONE: CheckCircleIcon,
};

const MIN_LIMIT = 1;
const MAX_LIMIT = 20;
const DEFAULT_LIMIT = 5;
const FALLBACK_RETENTION_DAYS = 5;

interface KanbanWidgetConfig {
  /** Anzuzeigende Spalten (≥ 1, in KANBAN_COLUMNS-Reihenfolge). */
  columns: KanbanColumn[];
  limit: number;
  showBorder: boolean;
  backgroundColor?: string;
}

function isColumn(v: unknown): v is KanbanColumn {
  return (
    v === 'BACKLOG' || v === 'READY' || v === 'IN_PROGRESS' || v === 'IN_REVIEW' || v === 'DONE'
  );
}

function clampLimit(value: unknown): number {
  const n = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
  if (!Number.isFinite(n)) return DEFAULT_LIMIT;
  return Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, Math.round(n)));
}

/**
 * Liest die Spaltenliste. Migriert Legacy-Single-Config (`column`) zu `columns`. Dedupliziert und
 * sortiert nach KANBAN_COLUMNS-Reihenfolge; faellt bei leerer/ungueltiger Auswahl auf `['BACKLOG']`.
 */
export function parseColumns(parsed: Record<string, unknown>): KanbanColumn[] {
  if (Array.isArray(parsed.columns)) {
    const chosen = parsed.columns.filter(isColumn);
    const ordered = KANBAN_COLUMNS.filter((c) => chosen.includes(c));
    if (ordered.length > 0) return ordered;
  }
  if (isColumn(parsed.column)) return [parsed.column];
  return ['BACKLOG'];
}

function parseConfig(raw: string): KanbanWidgetConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return {
      columns: parseColumns(parsed),
      limit: clampLimit(parsed.limit),
      ...parseSurfaceConfig(parsed),
    };
  } catch {
    return { columns: ['BACKLOG'], limit: DEFAULT_LIMIT, showBorder: false };
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
  const notify = useNotify();
  const [items, setItems] = useState<KanbanItem[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [retentionDays, setRetentionDays] = useState(FALLBACK_RETENTION_DAYS);
  const [detailItem, setDetailItem] = useState<KanbanItem | null>(null);

  const [open, setOpen] = useState(false);
  const [draftColumns, setDraftColumns] = useState<KanbanColumn[]>(config.columns);
  const [draftLimit, setDraftLimit] = useState(String(config.limit));
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');

  const columnsKey = config.columns.join(',');

  const reload = useCallback(async (): Promise<void> => {
    setLoadError(null);
    try {
      const board = await listKanbanItems();
      const colRank = (c: KanbanColumn): number => DISPLAY_ORDER.indexOf(c);
      // #221: Items aller gewählten Spalten zusammenführen und automatisch sortieren —
      // erst nach fester Spalten-Reihenfolge (In Review → In Progress → Ready → Backlog → Done),
      // innerhalb einer Spalte absteigend nach Issue-Nummer (höhere oben). Danach auf das
      // Gesamt-Limit kürzen.
      const merged = config.columns
        .flatMap((c) => board[c] ?? [])
        .sort((a, b) => colRank(a.column) - colRank(b.column) || b.number - a.number);
      setItems(merged.slice(0, config.limit));
    } catch (e) {
      setLoadError(e instanceof ApiError ? e.message : 'Laden fehlgeschlagen');
      setItems([]);
    }
    // columnsKey ist die stabile String-Quelle der Spaltenliste (Array pro Render neu).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [columnsKey, config.limit]);

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
      setDraftColumns(config.columns);
      setDraftLimit(String(config.limit));
      setDraftShowBorder(config.showBorder);
      setDraftBackgroundColor(config.backgroundColor ?? '');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, columnsKey, config.limit, config.showBorder, config.backgroundColor]);

  function toggleColumn(column: KanbanColumn): void {
    setDraftColumns((prev) => {
      const nextSet = prev.includes(column)
        ? prev.filter((c) => c !== column)
        : [...prev, column];
      // In KANBAN_COLUMNS-Reihenfolge halten (Dedupe inklusive).
      return KANBAN_COLUMNS.filter((c) => nextSet.includes(c));
    });
  }

  function handleApply(): void {
    if (draftColumns.length === 0) return;
    const next: KanbanWidgetConfig = {
      columns: draftColumns,
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
    // #316: Speicherfehler abfangen und melden (analog #292) — sonst läuft die Rejection
    // ungefangen durch `void handleSave()` im Modal, ohne Meldung, und das Modal bleibt offen.
    try {
      await updateKanbanItem(detailItem.id, title, body);
      setDetailItem(null);
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen.');
    }
  }

  // Task-Checkbox-Toggle (#359): nur den Body persistieren, Modal offen lassen; Rejection
  // weiterreichen, damit das Modal die optimistische Umschaltung zurücknimmt.
  async function handleToggleTask(body: string): Promise<void> {
    if (!detailItem) return;
    try {
      const updated = await updateKanbanItem(
        detailItem.id,
        detailItem.title,
        body,
        null,
        detailItem.parentId,
        detailItem.dependencies ?? [],
      );
      setDetailItem(updated);
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Speichern fehlgeschlagen.');
      throw e;
    }
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
          {config.columns.map((c) => COLUMN_LABELS[c]).join(', ')}
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
            {items.map((item, i) => (
              <Box
                component="li"
                key={item.id}
                sx={{
                  px: 0.75,
                  py: 0.25,
                  borderRadius: 1,
                  // #173: alternierend gefärbte Zeilen (Zebra) über Theme-Tokens, kein Hex.
                  backgroundColor: i % 2 === 0 ? 'action.hover' : 'transparent',
                  '&:hover': { backgroundColor: 'action.selected' },
                }}
              >
                {/* #191: 4-Spalten-Layout Icon | #Nr | Titel | Body. */}
                <Box
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: '24px 44px 1fr 2fr',
                    columnGap: 1,
                    alignItems: 'center',
                    minWidth: 0,
                  }}
                >
                  {(() => {
                    const Icon = COLUMN_ICON[item.column];
                    return (
                      <Icon
                        fontSize="small"
                        style={{ color: STATUS_COLORS[item.column].dot }}
                        sx={{ justifySelf: 'center' }}
                        aria-label={COLUMN_LABELS[item.column]}
                      />
                    );
                  })()}
                  <Typography variant="caption" color="text.secondary" noWrap sx={{ textAlign: 'right' }}>
                    {item.number > 0 ? `#${item.number}` : ''}
                  </Typography>
                  <Link
                    component="button"
                    type="button"
                    variant="body2"
                    underline="hover"
                    noWrap
                    onClick={() => setDetailItem(item)}
                    onMouseDown={(e) => e.stopPropagation()}
                    sx={{
                      textAlign: 'left',
                      display: 'block',
                      fontWeight: 500,
                      minWidth: 0,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {item.title}
                  </Link>
                  {item.body ? (
                    <Typography
                      component="p"
                      variant="caption"
                      color="text.secondary"
                      sx={{
                        // Body-Vorschau: max. 2 Zeilen, danach "…" (CSS-Ellipsis).
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                        whiteSpace: 'normal',
                        wordBreak: 'break-word',
                        minWidth: 0,
                      }}
                    >
                      {item.body}
                    </Typography>
                  ) : (
                    <Box />
                  )}
                </Box>
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
          onToggleTask={handleToggleTask}
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
            <Box>
              <Typography variant="caption" color="text.secondary">
                Spalten (mindestens eine)
              </Typography>
              <FormGroup>
                {KANBAN_COLUMNS.map((c) => (
                  <FormControlLabel
                    key={c}
                    control={
                      <Checkbox
                        checked={draftColumns.includes(c)}
                        onChange={() => toggleColumn(c)}
                      />
                    }
                    label={COLUMN_LABELS[c]}
                  />
                ))}
              </FormGroup>
              {draftColumns.length === 0 && (
                <Typography variant="caption" color="error">
                  Bitte mindestens eine Spalte auswählen.
                </Typography>
              )}
            </Box>
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
              <Button
                variant="contained"
                onClick={handleApply}
                disabled={draftColumns.length === 0}
              >
                Übernehmen
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Drawer>
    </Paper>
  );
}
