import { useState } from 'react';
import {
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Paper,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

import {
  KANBAN_COLUMNS,
  type KanbanColumn,
  type KanbanEpic,
  type KanbanItem,
} from '../../api/kanban';
import { cleanupCountdownLabel, cleanupDaysRemaining } from './cleanupCountdown';
import { COLUMN_LABELS } from './columnMeta';
import { epicColor } from './epicMeta';
import EpicBadge from './EpicBadge';
import { ARCHIVED_STATUS_COLOR } from './statusColors';

interface KanbanCardProps {
  item: KanbanItem;
  retentionDays: number;
  /** Das Epic, dem dieses Item zugeordnet ist (für das Badge). {@code null} = keinem Epic. */
  epic?: KanbanEpic | null;
  onOpenDetail: (item: KanbanItem) => void;
  onEdit: (item: KanbanItem) => void;
  onArchive: (item: KanbanItem) => void;
  onRestore: (item: KanbanItem) => void;
  onForceDelete: (item: KanbanItem) => void;
  onMove: (item: KanbanItem, targetColumn: KanbanColumn) => void;
}

export default function KanbanCard({
  item,
  retentionDays,
  epic = null,
  onOpenDetail,
  onEdit,
  onArchive,
  onRestore,
  onForceDelete,
  onMove,
}: KanbanCardProps): JSX.Element {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: item.id,
    data: { type: 'item', column: item.column, position: item.position },
    disabled: item.archived,
  });

  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : item.archived ? 0.5 : 1,
  };

  const { movedToDoneAt } = item;
  const showDoneHint = item.column === 'DONE' && movedToDoneAt != null && !item.archived;
  const daysRemaining =
    showDoneHint && movedToDoneAt != null
      ? cleanupDaysRemaining(movedToDoneAt, retentionDays)
      : 0;

  // Epic-Badge (#325): farbiger linker Rand + Kürzel-Chip, wenn das Item einem Epic zugeordnet ist.
  const epicHue = epic ? epicColor(epic.id) : null;

  return (
    <Paper
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...(item.archived ? {} : listeners)}
      elevation={1}
      sx={{
        p: 1.5,
        mb: 1,
        bgcolor: 'common.white',
        borderRadius: 1.5,
        borderLeft: epicHue ? `4px solid ${epicHue}` : undefined,
        cursor: item.archived ? 'default' : 'grab',
        userSelect: 'none',
        transition: 'box-shadow 150ms',
        '&:hover': { boxShadow: 4 },
        '&:active': { cursor: item.archived ? 'default' : 'grabbing' },
      }}
      aria-label={`Kanban-Item ${item.title}`}
    >
      {epic && epicHue && <EpicBadge epic={epic} sx={{ mb: 0.5 }} />}

      <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={0.5}>
        <Tooltip title={item.title} enterDelay={500}>
          <Typography
            variant="subtitle2"
            role="button"
            tabIndex={0}
            aria-label={`Detail öffnen: ${item.title}`}
            onPointerDown={(e) => e.stopPropagation()}
            onClick={() => onOpenDetail(item)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onOpenDetail(item);
              }
            }}
            sx={{
              flex: 1,
              minWidth: 0,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {item.number > 0 && (
              <Box component="span" sx={{ color: 'text.secondary', fontWeight: 400 }}>
                #{item.number} –{' '}
              </Box>
            )}
            {item.title}
          </Typography>
        </Tooltip>
        <IconButton
          size="small"
          aria-label="Item-Menü"
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => setMenuAnchor(e.currentTarget)}
          sx={{ mt: -0.5, mr: -0.5 }}
        >
          <MoreVertIcon fontSize="small" />
        </IconButton>
      </Stack>

      {item.archived && (
        <Chip
          label="Archiviert"
          size="small"
          sx={{ mt: 0.5 }}
          style={{ backgroundColor: ARCHIVED_STATUS_COLOR.bg, color: ARCHIVED_STATUS_COLOR.text }}
        />
      )}

      {showDoneHint && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
          {cleanupCountdownLabel(daysRemaining)}
        </Typography>
      )}

      <Menu
        anchorEl={menuAnchor}
        open={menuAnchor != null}
        onClose={() => setMenuAnchor(null)}
        onPointerDown={(e) => e.stopPropagation()}
      >
        {!item.archived && (
          <MenuItem
            onClick={() => {
              setMenuAnchor(null);
              onEdit(item);
            }}
          >
            Bearbeiten
          </MenuItem>
        )}
        {!item.archived && (
          <MenuItem
            onClick={() => {
              setMenuAnchor(null);
              onArchive(item);
            }}
          >
            Archivieren
          </MenuItem>
        )}
        {/* Tastaturbedienbarer Statuswechsel (#316): das Menü ist per Tastatur/Screenreader
            nutzbar, anders als das reine Maus-Drag&Drop. Ein Eintrag je Zielspalte außer der
            aktuellen. */}
        {!item.archived &&
          KANBAN_COLUMNS.filter((column) => column !== item.column).map((column) => (
            <MenuItem
              key={column}
              onClick={() => {
                setMenuAnchor(null);
                onMove(item, column);
              }}
            >
              Nach {COLUMN_LABELS[column]}
            </MenuItem>
          ))}
        {item.archived && (
          <MenuItem
            onClick={() => {
              setMenuAnchor(null);
              onRestore(item);
            }}
          >
            Wiederherstellen
          </MenuItem>
        )}
        {item.archived && (
          <MenuItem
            sx={{ color: 'error.main' }}
            onClick={() => {
              setMenuAnchor(null);
              onForceDelete(item);
            }}
          >
            Endgültig löschen
          </MenuItem>
        )}
      </Menu>
    </Paper>
  );
}
