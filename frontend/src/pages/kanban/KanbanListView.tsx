import { useCallback, useEffect, useRef, useState } from 'react';
import type { DragEvent } from 'react';
import { Alert, Box, Chip, CircularProgress, Stack, Typography } from '@mui/material';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';

import {
  KANBAN_COLUMNS,
  forceDeleteKanbanItem,
  getKanbanEpics,
  getKanbanSettings,
  listKanbanItems,
  moveKanbanItem,
  restoreKanbanItem,
  updateKanbanItem,
  updateKanbanSettings,
  type KanbanColumn,
  type KanbanEpic,
  type KanbanItem,
} from '../../api/kanban';
import { ApiError } from '../../api/client';
import { useNotify } from '../../notify/NotifyProvider';
import EpicBadge from './EpicBadge';
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

/** Erlaubte Filter-Keys — filtert unbekannte Werte aus einer gespeicherten Antwort heraus. */
const FILTER_KEYS: readonly string[] = FILTERS.map((f) => f.key);

/** Entprellung der serverseitigen Filter-Persistenz, damit schnelles Klicken nicht viele PUTs auslöst. */
const SAVE_DEBOUNCE_MS = 500;

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
  /**
   * Vom Parent kontrollierter Reload-Trigger (#308): jede Wertänderung erzwingt ein
   * Neuladen der Liste. Nötig, weil mutierende Aktionen (Neues Item, Archivieren,
   * Wiederherstellen) im Parent {@link KanbanPage} passieren, dessen Board-State die
   * Liste nicht teilt. Default 0 hält die Ansicht ohne Parent-Trigger funktionsfähig.
   */
  reloadKey?: number;
}

/**
 * Listenansicht des Kanban-Boards (#282), angelehnt an die Kit-Liste (kit/board-ui.mjs):
 * Status-Filter-Chips (inkl. Archiv), pro Zeile Nummer + Status-Badge + Titel + resizable
 * Excerpt-Spalte. Selbstständig — lädt eigene Daten und besitzt eine eigene
 * {@link KanbanDetailModal}-Instanz; nur eine Ansicht (Board oder Liste) ist gleichzeitig sichtbar.
 */
export default function KanbanListView({ retentionDays, reloadKey = 0 }: Props): JSX.Element {
  const notify = useNotify();
  const [activeFilters, setActiveFilters] = useState<Set<FilterKey>>(
    () => new Set<FilterKey>(KANBAN_COLUMNS),
  );
  const [board, setBoard] = useState<KanbanItem[] | null>(null);
  const [epicsById, setEpicsById] = useState<Record<number, KanbanEpic>>({});
  const [error, setError] = useState<string | null>(null);
  const [reloadNonce, setReloadNonce] = useState(0);
  const [detailItem, setDetailItem] = useState<KanbanItem | null>(null);
  const [excerptWidth, setExcerptWidth] = useState<number>(readStoredExcerptWidth);
  const [dragItemId, setDragItemId] = useState<number | null>(null);
  const [dragOverItemId, setDragOverItemId] = useState<number | null>(null);

  const archiveActive = activeFilters.has(ARCHIVED_KEY);
  const viewRef = useRef<HTMLDivElement>(null);
  const resizingRef = useRef(false);
  const draggingRef = useRef(false);
  // Entfernt die document-Listener eines gerade laufenden Resize-Drags bei Unmount (#316).
  const resizeCleanupRef = useRef<(() => void) | null>(null);
  // Aktueller retentionDays-Wert für den entprellten Save (vermeidet stale closure).
  const retentionRef = useRef(retentionDays);
  retentionRef.current = retentionDays;
  const saveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => () => resizeCleanupRef.current?.(), []);

  // Laufenden Save-Timer bei Unmount abräumen.
  useEffect(
    () => () => {
      if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    },
    [],
  );

  // Gespeicherte Filter-Auswahl serverseitig laden (#346). Fehler bleiben still (Default-Filter
  // bleiben aktiv), damit die Ansicht nie blockiert.
  useEffect(() => {
    let cancelled = false;
    void getKanbanSettings()
      .then((s) => {
        if (cancelled || !Array.isArray(s.activeFilters)) return;
        setActiveFilters(
          new Set(s.activeFilters.filter((k): k is FilterKey => FILTER_KEYS.includes(k))),
        );
      })
      .catch((e) => {
        console.warn('Kanban-Listen-Filter konnten nicht geladen werden.', e);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Entprelltes Persistieren der Filter. Best-Effort: schlägt der PUT fehl, wirken die Filter
  // lokal weiter, es erscheint nur eine Konsolen-Warnung (kein Störer).
  const scheduleFilterSave = useCallback((filters: Set<FilterKey>): void => {
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(() => {
      void updateKanbanSettings(retentionRef.current, [...filters]).catch((e) => {
        console.warn('Kanban-Listen-Filter konnten nicht gespeichert werden.', e);
      });
    }, SAVE_DEBOUNCE_MS);
  }, []);

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
  }, [archiveActive, reloadNonce, reloadKey]);

  // Epics laden, um pro Zeile das Epic-Badge zu zeigen (#342). Fehler bleiben still (kein Badge).
  useEffect(() => {
    let cancelled = false;
    getKanbanEpics()
      .then((epics) => {
        if (!cancelled) {
          setEpicsById(Object.fromEntries(epics.map((e) => [e.id, e])));
        }
      })
      .catch(() => {
        if (!cancelled) setEpicsById({});
      });
    return () => {
      cancelled = true;
    };
  }, [reloadNonce, reloadKey]);

  function toggleFilter(key: FilterKey): void {
    setActiveFilters((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      scheduleFilterSave(next);
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
    const detach = (): void => {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      resizeCleanupRef.current = null;
    };
    const onUp = (): void => {
      detach();
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
    // Cleanup-Handle fuer den Unmount-Fall (Drag laeuft noch, mouseup kommt nie) — #316.
    resizeCleanupRef.current = detach;
  }, []);

  async function handleDetailSubmit(
    title: string,
    body: string,
    parentId: number | null,
  ): Promise<void> {
    if (!detailItem) return;
    try {
      await updateKanbanItem(detailItem.id, title, body, null, parentId);
      notify.success('Item gespeichert.');
      setDetailItem(null);
      setReloadNonce((n) => n + 1);
    } catch (err) {
      notify.error(err instanceof ApiError ? err.message : 'Speichern fehlgeschlagen.');
    }
  }

  async function handleDetailRestore(): Promise<void> {
    if (!detailItem) return;
    try {
      await restoreKanbanItem(detailItem.id);
      notify.success('Item wiederhergestellt.');
      setDetailItem(null);
      setReloadNonce((n) => n + 1);
    } catch (err) {
      notify.error(err instanceof ApiError ? err.message : 'Wiederherstellen fehlgeschlagen.');
    }
  }

  async function handleDetailForceDelete(): Promise<void> {
    if (!detailItem) return;
    try {
      await forceDeleteKanbanItem(detailItem.id);
      notify.success('Item endgültig gelöscht.');
      setDetailItem(null);
      setReloadNonce((n) => n + 1);
    } catch (err) {
      notify.error(err instanceof ApiError ? err.message : 'Löschen fehlgeschlagen.');
    }
  }

  const visible = (board ?? [])
    .filter((i) => (i.archived ? archiveActive : activeFilters.has(i.column)))
    .sort(
      (a, b) => KANBAN_COLUMNS.indexOf(a.column) - KANBAN_COLUMNS.indexOf(b.column) || a.position - b.position,
    );

  // Reorder per Drag&Drop (#283) — nur innerhalb desselben Status. Die Toolbox kennt
  // `position` nur pro Spalte, ein Drop auf einen fremden Status ist daher ein No-op.
  function handleDragStart(e: DragEvent<HTMLDivElement>, item: KanbanItem): void {
    draggingRef.current = true;
    setDragItemId(item.id);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', String(item.id));
  }

  function isValidDropTarget(target: KanbanItem): KanbanItem | null {
    if (dragItemId == null || dragItemId === target.id || target.archived) return null;
    const dragged = visible.find((i) => i.id === dragItemId);
    if (!dragged || dragged.column !== target.column) return null;
    return dragged;
  }

  function handleDragOver(e: DragEvent<HTMLDivElement>, target: KanbanItem): void {
    if (!isValidDropTarget(target)) return;
    e.preventDefault();
    setDragOverItemId(target.id);
  }

  async function handleDrop(e: DragEvent<HTMLDivElement>, target: KanbanItem): Promise<void> {
    e.preventDefault();
    const dragged = isValidDropTarget(target);
    setDragOverItemId(null);
    if (!dragged) return;
    try {
      await moveKanbanItem(dragged.id, target.column, target.position);
      setReloadNonce((n) => n + 1);
    } catch (err) {
      notify.error(err instanceof ApiError ? err.message : 'Verschieben fehlgeschlagen.');
    }
  }

  function handleDragEnd(): void {
    setDragItemId(null);
    setDragOverItemId(null);
    // Flag erst nach dem Click-Event zuruecksetzen, damit kein Modal aufgeht.
    setTimeout(() => {
      draggingRef.current = false;
    }, 0);
  }

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
                draggable={!item.archived}
                onDragStart={(e) => handleDragStart(e, item)}
                onDragOver={(e) => handleDragOver(e, item)}
                onDrop={(e) => void handleDrop(e, item)}
                onDragEnd={handleDragEnd}
                onClick={() => {
                  if (!resizingRef.current && !draggingRef.current) setDetailItem(item);
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
                  borderTopColor: dragOverItemId === item.id ? 'primary.main' : 'divider',
                  borderTopWidth: dragOverItemId === item.id ? 2 : 1,
                  borderRadius: 1.5,
                  px: 1.5,
                  py: 1,
                  cursor: 'pointer',
                  userSelect: 'none',
                  transition: 'box-shadow 150ms',
                  '&:hover': { boxShadow: 2 },
                }}
              >
                <DragIndicatorIcon
                  fontSize="small"
                  aria-label="Reihenfolge ändern"
                  sx={{
                    flexShrink: 0,
                    color: 'action.disabled',
                    visibility: item.archived ? 'hidden' : 'visible',
                    cursor: item.archived ? 'default' : 'grab',
                  }}
                />
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
                {item.parentId != null && epicsById[item.parentId] && (
                  <EpicBadge epic={epicsById[item.parentId]} />
                )}
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
          onRestore={handleDetailRestore}
          onForceDelete={handleDetailForceDelete}
        />
      )}
    </Box>
  );
}
