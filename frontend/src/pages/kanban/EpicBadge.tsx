import { Box, Stack, Typography } from '@mui/material';
import type { SxProps, Theme } from '@mui/material';

import type { KanbanEpic } from '../../api/kanban';
import { epicColor, epicShortcode } from './epicMeta';

interface EpicBadgeProps {
  epic: KanbanEpic;
  /** Zusätzliche Layout-Styles der Caller (z. B. Abstand auf der Karte). */
  sx?: SxProps<Theme>;
}

/**
 * Kürzel-Badge eines Epics (#325/#342): farbiger Punkt + Kürzel auf zartem Hintergrund in der
 * Epic-Farbe. Geteilt zwischen Board-Karte ({@link KanbanCard}) und Listenansicht
 * ({@link KanbanListView}), damit beide identisch aussehen.
 */
export default function EpicBadge({ epic, sx }: EpicBadgeProps): JSX.Element {
  const hue = epicColor(epic.id);
  const label = epicShortcode(epic.title, epic.shortcode);
  return (
    <Stack
      direction="row"
      alignItems="center"
      spacing={0.5}
      sx={{
        width: 'fit-content',
        px: 0.75,
        py: 0.25,
        borderRadius: 1,
        bgcolor: `${hue}22`,
        flexShrink: 0,
        ...sx,
      }}
      title={epic.title}
      aria-label={`Epic ${label}`}
    >
      <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: hue, flexShrink: 0 }} />
      <Typography variant="caption" sx={{ fontWeight: 700, color: hue, lineHeight: 1 }}>
        {label}
      </Typography>
    </Stack>
  );
}
