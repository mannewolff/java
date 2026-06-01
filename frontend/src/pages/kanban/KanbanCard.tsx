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
import ReactMarkdown from 'react-markdown';

import type { KanbanItem } from '../../api/kanban';
import { cleanupCountdownLabel, cleanupDaysRemaining } from './cleanupCountdown';

interface KanbanCardProps {
  item: KanbanItem;
  retentionDays: number;
  onOpenDetail: (item: KanbanItem) => void;
  onEdit: (item: KanbanItem) => void;
  onArchive: (item: KanbanItem) => void;
  onRestore: (item: KanbanItem) => void;
  onForceDelete: (item: KanbanItem) => void;
}

export default function KanbanCard({
  item,
  retentionDays,
  onOpenDetail,
  onEdit,
  onArchive,
  onRestore,
  onForceDelete,
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

  const showDoneHint = item.column === 'DONE' && item.movedToDoneAt && !item.archived;
  const daysRemaining = showDoneHint
    ? cleanupDaysRemaining(item.movedToDoneAt!, retentionDays)
    : 0;

  return (
    <Paper
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...(item.archived ? {} : listeners)}
      variant="outlined"
      sx={{
        p: 1.5,
        mb: 1,
        cursor: item.archived ? 'default' : 'grab',
        userSelect: 'none',
        '&:active': { cursor: item.archived ? 'default' : 'grabbing' },
      }}
      aria-label={`Kanban-Item ${item.title}`}
    >
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
        <Chip label="Archiviert" size="small" sx={{ mt: 0.5 }} />
      )}

      {item.body.trim().length > 0 && (
        <Box
          sx={{
            mt: 0.5,
            color: 'text.secondary',
            fontSize: '0.85rem',
            display: '-webkit-box',
            WebkitLineClamp: 3,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
            '& p': { margin: 0 },
            '& ul, & ol': { margin: 0, paddingLeft: '1.2em' },
          }}
        >
          <ReactMarkdown>{item.body}</ReactMarkdown>
        </Box>
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
