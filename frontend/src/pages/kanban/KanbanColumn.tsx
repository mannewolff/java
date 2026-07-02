import type { ComponentType } from 'react';
import { Box, Chip, IconButton, Paper, Stack, Tooltip, Typography } from '@mui/material';
import type { SvgIconProps } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import InboxIcon from '@mui/icons-material/Inbox';
import FlagIcon from '@mui/icons-material/Flag';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import VisibilityIcon from '@mui/icons-material/Visibility';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { useDroppable } from '@dnd-kit/core';

import type { KanbanColumn as KanbanColumnId, KanbanItem } from '../../api/kanban';
import KanbanCard from './KanbanCard';

/** Icon + Akzentfarbe je Spalte (#189). Icons sind dekorativ — das Label liefert den Text. */
const COLUMN_HEADER: Record<KanbanColumnId, { Icon: ComponentType<SvgIconProps>; color: string }> = {
  BACKLOG: { Icon: InboxIcon, color: 'text.secondary' },
  READY: { Icon: FlagIcon, color: 'primary.main' },
  IN_PROGRESS: { Icon: PlayArrowIcon, color: 'info.main' },
  IN_REVIEW: { Icon: VisibilityIcon, color: 'warning.main' },
  DONE: { Icon: CheckCircleIcon, color: 'success.main' },
};

interface KanbanColumnProps {
  column: KanbanColumnId;
  label: string;
  items: KanbanItem[];
  retentionDays: number;
  onCreate: (column: KanbanColumnId) => void;
  onOpenDetail: (item: KanbanItem) => void;
  onEdit: (item: KanbanItem) => void;
  onArchive: (item: KanbanItem) => void;
  onRestore: (item: KanbanItem) => void;
  onForceDelete: (item: KanbanItem) => void;
}

export default function KanbanColumnView({
  column,
  label,
  items,
  retentionDays,
  onCreate,
  onOpenDetail,
  onEdit,
  onArchive,
  onRestore,
  onForceDelete,
}: KanbanColumnProps): JSX.Element {
  const { setNodeRef, isOver } = useDroppable({
    id: `column-${column}`,
    data: { type: 'column', column },
  });

  return (
    <Paper
      variant="outlined"
      sx={{
        p: 1.5,
        flex: 1,
        minWidth: 240,
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'background.default',
      }}
      aria-label={`Spalte ${label}`}
    >
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Stack direction="row" alignItems="center" spacing={1}>
          {(() => {
            const { Icon, color } = COLUMN_HEADER[column];
            return <Icon fontSize="small" sx={{ color }} aria-hidden />;
          })()}
          <Typography variant="subtitle1" fontWeight={600}>
            {label}
          </Typography>
          <Chip size="small" label={items.length} />
        </Stack>
        <Tooltip title="Neues Item in dieser Spalte">
          <IconButton
            size="small"
            aria-label={`Neues Item in ${label}`}
            onClick={() => onCreate(column)}
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
          borderRadius: 1,
          transition: 'background-color 150ms',
          bgcolor: isOver ? 'action.hover' : 'transparent',
          border: items.length === 0 ? '2px dashed' : 'none',
          borderColor: isOver ? 'primary.main' : 'divider',
          p: items.length === 0 ? 2 : 0.5,
          textAlign: items.length === 0 ? 'center' : 'left',
        }}
      >
        <SortableContext
          items={items.map((i) => i.id)}
          strategy={verticalListSortingStrategy}
        >
          {items.map((item) => (
            <KanbanCard
              key={item.id}
              item={item}
              retentionDays={retentionDays}
              onOpenDetail={onOpenDetail}
              onEdit={onEdit}
              onArchive={onArchive}
              onRestore={onRestore}
              onForceDelete={onForceDelete}
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
