import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Box, Chip, CircularProgress, Stack, Typography } from '@mui/material';

import {
  KANBAN_COLUMNS,
  listKanbanItems,
  updateKanbanItem,
  type KanbanColumn,
  type KanbanItem,
} from '../../api/kanban';
import { ApiError } from '../../api/client';
import KanbanDetailModal from './KanbanDetailModal';
import { COLUMN_LABELS } from './columnMeta';
import { ARCHIVED_STATUS_COLOR, STATUS_COLORS, type StatusColorSet } from './statusColors';
import { EXCERPT_DEFAULT_PCT, clampExcerptWidth, stripMarkdown } from './listExcerpt';

/** Pseudo-Status fuer den Archiv-Filter — kein echter Board-Status. */
const ARCHIVED_KEY = 'archived';
type FilterKey = KanbanColumn | typeof ARCHIVED_KEY;

const FILTERS: readonly { key: FilterKey; label: string }[] = [
  ...KANBAN_COLUMNS.map((c) => ({ key: c as FilterKey, label: COLUMN_LABELS[c] })),
  { key: ARCHIVED_KEY, label: 'Archiv' },
];

const EXCERPT_WIDTH_KEY = 'kanban.listExcerptWidth';

function readStoredExcerptWidth(): number {
  try {
    const raw = localStorage.getItem(EXCERPT_WIDTH_KEY);
    if (raw == null) return EXCERPT_DEFAULT_PCT;
    return clampExcerptWidth(Number.parseFloat(raw));
  } catch {
    return EXCERPT_DEFAULT_PCT;
  }
}

function storeExcerptWidth(pct: number): void {
  try {
    localStorage.setItem(EXCERPT_WIDTH_KEY, String(pct));
  } catch {
    // localStorage nicht verfügbar — Breite bleibt sitzungslokal.
  }
}

function badgeFor(item: KanbanItem): { label: string; colors: StatusColorSet } {
  if (item.archived) return { label: 'Archiv', colors: ARCHIVED_STATUS_COLOR };
  return { label: COLUMN_LABELS[item.column], colors: STATUS_COLORS[item.column] };
}

interface Props {
  retentionDays: number;
}

/**
 * Listenansicht des Kanban-Boards (#282), angelehnt an die Kit-Liste (kit/board-ui.mjs):
 * Status-Filter-Chips (inkl. Archiv), pro Zeile Nummer + Status-Badge + Titel + resizable
 * Excerpt-Spalte. Selbstständig — lädt eigene Daten und besitzt eine eigene
 * {@link KanbanDetailModal}-Instanz; nur eine Ansicht (Board oder Liste) ist gleichzeitig sichtbar.
 */
export default function KanbanListView({ retentionDays }: Props): JSX.Element {
  const [activeFilters, setActiveFilters] = useState<Set<FilterKey>>(
    () => new Set<FilterKey>(KANBAN_COLUMNS),
  );
  const [board, setBoard] = useState<KanbanItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadNonce, setReloadNonce] = useState(0);
  const [detailItem, setDetailItem] = useState<KanbanItem | null>(null);
  const [excerptWidth, setExcerptWidth] = useState<number>(readStoredExcerptWidth);

  const archiveActive = activeFilters.has(ARCHIVED_KEY);
  const viewRef = useRef<HTMLDivElement>(null);
  const resizingRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setBoard(null);
    setError(null);
    listKanbanItems(archiveActive)
      .then((b) => {
        if (!cancelled) setBoard(KANBAN_COLUMNS.flatMap((c) => b[c]));
      })
      .catch((e) => {
        if (!cancelled) {
          setError(e instanceof ApiError ? e.message : 'Kanban-Items konnten nicht geladen werden.');
          setBoard([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [archiveActive, reloadNonce]);

  function toggleFilter(key: FilterKey): void {
    setActiveFilters((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }

  const startResize = useCallback((): void => {
    resizingRef.current = true;
    const onMove = (e: MouseEvent): void => {
      const el = viewRef.current;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      setExcerptWidth(clampExcerptWidth(((rect.right - e.clientX) / rect.width) * 100));
    };
    const onUp = (): void => {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      setExcerptWidth((w) => {
        storeExcerptWidth(w);
        return w;
      });
      // Flag erst nach dem Click-Event zuruecksetzen, damit kein Modal aufgeht.
      setTimeout(() => {
        resizingRef.current = false;
      }, 0);
    };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  }, []);

  async function handleDetailSubmit(title: string, body: string): Promise<void> {
    if (!detailItem) return;
    await updateKanbanItem(detailItem.id, title, body);
    setDetailItem(null);
    setReloadNonce((n) => n + 1);
  }

  const visible = (board ?? [])
    .filter((i) => (i.archived ? archiveActive : activeFilters.has(i.column)))
    .sort(
      (a, b) => KANBAN_COLUMNS.indexOf(a.column) - KANBAN_COLUMNS.indexOf(b.column) || a.position - b.position,
    );

  return (
    <Box ref={viewRef} data-excerpt-width={excerptWidth} sx={{ '--excerpt-w': `${excerptWidth}%` }}>
      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
        {FILTERS.map((f) => {
          const active = activeFilters.has(f.key);
          return (
            <Chip
              key={f.key}
              label={f.label}
              aria-label={`Filter ${f.label}`}
              aria-pressed={active}
              onClick={() => toggleFilter(f.key)}
              variant={active ? 'filled' : 'outlined'}
              color={active ? 'primary' : 'default'}
              size="small"
            />
          );
        })}
      </Stack>

      {error ? (
        <Alert severity="error">{error}</Alert>
      ) : board === null ? (
        <Stack alignItems="center" sx={{ py: 4 }}>
          <CircularProgress aria-label="Kanban-Items werden geladen" />
        </Stack>
      ) : visible.length === 0 ? (
        <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
          Keine Items
        </Typography>
      ) : (
        <Stack spacing={0.75}>
          {visible.map((item) => {
            const badge = badgeFor(item);
            return (
              <Box
                key={item.id}
                role="button"
                tabIndex={0}
                aria-label={`Detail öffnen: ${item.title}`}
                onClick={() => {
                  if (!resizingRef.current) setDetailItem(item);
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    setDetailItem(item);
                  }
                }}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  bgcolor: 'common.white',
                  border: '1px solid',
                  borderColor: 'divider',
                  borderRadius: 1.5,
                  px: 1.5,
                  py: 1,
                  cursor: 'pointer',
                  userSelect: 'none',
                  transition: 'box-shadow 150ms',
                  '&:hover': { boxShadow: 2 },
                }}
              >
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ flexShrink: 0, width: 40 }}
                >
                  {item.number > 0 ? `#${item.number}` : ''}
                </Typography>
                <Chip
                  label={badge.label}
                  size="small"
                  sx={{
                    flexShrink: 0,
                    bgcolor: badge.colors.bg,
                    color: badge.colors.text,
                    fontWeight: 600,
                  }}
                />
                <Typography
                  variant="body2"
                  sx={{
                    flex: 1,
                    minWidth: 0,
                    fontWeight: 500,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {item.title}
                </Typography>
                <Box
                  role="separator"
                  aria-orientation="vertical"
                  aria-label="Spaltenbreite ziehen"
                  onMouseDown={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    startResize();
                  }}
                  onClick={(e) => e.stopPropagation()}
                  sx={{
                    alignSelf: 'stretch',
                    width: '6px',
                    flexShrink: 0,
                    cursor: 'col-resize',
                    borderRight: '2px solid',
                    borderColor: 'divider',
                    '&:hover': { borderColor: 'primary.main' },
                  }}
                />
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{
                    flex: `0 0 var(--excerpt-w)`,
                    minWidth: 0,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {stripMarkdown(item.body)}
                </Typography>
              </Box>
            );
          })}
        </Stack>
      )}

      {detailItem != null && (
        <KanbanDetailModal
          open
          item={detailItem}
          retentionDays={retentionDays}
          onClose={() => setDetailItem(null)}
          onSubmit={handleDetailSubmit}
        />
      )}
    </Box>
  );
}
