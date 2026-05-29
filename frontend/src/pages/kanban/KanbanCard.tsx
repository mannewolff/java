import { useState } from 'react';
import {
  Box,
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
  /** Triggered when the user clicks the card title — öffnet das Detail-Modal. */
  onOpenDetail: (item: KanbanItem) => void;
  /** Triggered when the user picks "Bearbeiten" from the card menu. */
  onEdit: (item: KanbanItem) => void;
  /** Triggered when the user picks "Löschen". UI fragt eigene Confirm im Eltern-State. */
  onDelete: (item: KanbanItem) => void;
}

/**
 * Kompakte Card-Anzeige eines Kanban-Items. Markdown-Body wird auf drei Zeilen via CSS-clamp
 * begrenzt, lange Titles per Tooltip sichtbar. Drei-Punkte-Menue oben rechts mit "Bearbeiten"
 * und "Löschen".
 *
 * Die Card ist ueber {@link useSortable} an dnd-kit angeschlossen — Drag-Handle ist die ganze
 * Card, der Menue-Button stoppt {@code pointerDown} damit Klicks nicht als Drag interpretiert
 * werden.
 */
export default function KanbanCard({
  item,
  retentionDays,
  onOpenDetail,
  onEdit,
  onDelete,
}: KanbanCardProps): JSX.Element {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: item.id,
    data: { type: 'item', column: item.column, position: item.position },
  });

  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const showDoneHint = item.column === 'DONE' && item.movedToDoneAt;
  const daysRemaining = showDoneHint
    ? cleanupDaysRemaining(item.movedToDoneAt!, retentionDays)
    : 0;

  return (
    <Paper
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      variant="outlined"
      sx={{
        p: 1.5,
        mb: 1,
        cursor: 'grab',
        userSelect: 'none',
        '&:active': { cursor: 'grabbing' },
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
            // onPointerDown stoppt die Propagation an den dnd-kit-Sensor, damit ein Klick auf
            // den Titel das Detail-Modal oeffnet statt einen Drag zu starten.
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

      {item.body.trim().length > 0 && (
        <Box
          sx={{
            mt: 0.5,
            color: 'text.secondary',
            fontSize: '0.85rem',
            // Mehrzeiliges Clamp ohne den Markdown-Text vorher abschneiden zu muessen.
            // Browser-Support: WebKit + alle modernen Browser. Fallback ist einfach lang.
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
        <MenuItem
          onClick={() => {
            setMenuAnchor(null);
            onEdit(item);
          }}
        >
          Bearbeiten
        </MenuItem>
        <MenuItem
          onClick={() => {
            setMenuAnchor(null);
            onDelete(item);
          }}
        >
          Löschen
        </MenuItem>
      </Menu>
    </Paper>
  );
}
