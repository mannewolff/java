import { Box, IconButton, Paper, Stack, Tooltip, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';

import type { KanbanColumn as KanbanColumnId, KanbanEpic, KanbanItem } from '../../api/kanban';
import { COLUMN_SURFACE_BG, STATUS_COLORS } from './statusColors';
import KanbanCard from './KanbanCard';

interface KanbanColumnProps {
  column: KanbanColumnId;
  label: string;
  items: KanbanItem[];
  retentionDays: number;
  /** Epics nach ID — für das Epic-Badge zugeordneter Karten (#325). */
  epicsById: Record<number, KanbanEpic>;
  onCreate: (column: KanbanColumnId) => void;
  onOpenDetail: (item: KanbanItem) => void;
  onEdit: (item: KanbanItem) => void;
  onArchive: (item: KanbanItem) => void;
  onRestore: (item: KanbanItem) => void;
  onForceDelete: (item: KanbanItem) => void;
  onMove: (item: KanbanItem, targetColumn: KanbanColumnId) => void;
}

export default function KanbanColumnView({
  column,
  label,
  items,
  retentionDays,
  epicsById,
  onCreate,
  onOpenDetail,
  onEdit,
  onArchive,
  onRestore,
  onForceDelete,
  onMove,
}: KanbanColumnProps): JSX.Element {
  const { setNodeRef, isOver } = useDroppable({
    id: `column-${column}`,
    data: { type: 'column', column },
  });

  const colors = STATUS_COLORS[column];

  return (
    <Paper
      elevation={0}
      sx={{
        flex: 1,
        minWidth: 240,
        display: 'flex',
        flexDirection: 'column',
        bgcolor: COLUMN_SURFACE_BG,
        borderRadius: 2,
        overflow: 'hidden',
      }}
      aria-label={`Spalte ${label}`}
    >
      <Stack
        direction="row"
        alignItems="center"
        spacing={1}
        sx={{
          px: 1.5,
          py: 1,
          bgcolor: colors.bg,
          color: colors.text,
        }}
      >
        <Box
          sx={{
            width: 10,
            height: 10,
            borderRadius: '50%',
            bgcolor: colors.dot,
            flexShrink: 0,
          }}
        />
        <Typography
          variant="caption"
          sx={{
            fontWeight: 600,
            letterSpacing: '.04em',
            textTransform: 'uppercase',
          }}
        >
          {label}
        </Typography>
        <Typography variant="caption" sx={{ ml: 'auto', opacity: 0.7 }}>
          {items.length}
        </Typography>
        <Tooltip title="Neues Item in dieser Spalte">
          <IconButton
            size="small"
            aria-label={`Neues Item in ${label}`}
            onClick={() => onCreate(column)}
            sx={{ color: colors.text, p: 0.25 }}
          >
            <AddIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </Stack>

      <Box
        ref={setNodeRef}
        sx={{
          flex: 1,
          minHeight: 80,
          transition: 'background-color 150ms',
          bgcolor: isOver ? 'action.hover' : 'transparent',
          border: items.length === 0 ? '2px dashed' : 'none',
          borderColor: isOver ? 'primary.main' : 'divider',
          p: items.length === 0 ? 2 : 1,
          textAlign: items.length === 0 ? 'center' : 'left',
        }}
      >
        <SortableContext items={items.map((i) => i.id)} strategy={verticalListSortingStrategy}>
          {items.map((item) => (
            <KanbanCard
              key={item.id}
              item={item}
              retentionDays={retentionDays}
              epic={item.parentId != null ? epicsById[item.parentId] ?? null : null}
              onOpenDetail={onOpenDetail}
              onEdit={onEdit}
              onArchive={onArchive}
              onRestore={onRestore}
              onForceDelete={onForceDelete}
              onMove={onMove}
            />
          ))}
        </SortableContext>
        {items.length === 0 && (
          <Typography variant="caption" color="text.secondary">
            Hier herziehen oder + klicken
          </Typography>
        )}
      </Box>
    </Paper>
  );
}
