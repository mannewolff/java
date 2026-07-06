import { Box, LinearProgress, Paper, Stack, Typography } from '@mui/material';

import type { KanbanEpic } from '../../api/kanban';
import { epicColor, epicShortcode } from './epicMeta';
import { stripMarkdown } from './listExcerpt';

interface KanbanEpicsViewProps {
  epics: KanbanEpic[];
  onOpen: (epic: KanbanEpic) => void;
}

/**
 * Kachel-Liste aller Epics (#326). Jede Kachel zeigt farbigen linken Rand, Kürzel-Chip, Titel,
 * einen Beschreibungs-Auszug und einen Fortschrittsbalken „done/total Stories fertig". Klick öffnet
 * das Epic-Detail. Vorlage board-ui.
 */
export default function KanbanEpicsView({ epics, onOpen }: KanbanEpicsViewProps): JSX.Element {
  if (epics.length === 0) {
    return (
      <Paper variant="outlined" sx={{ p: 6, textAlign: 'center', color: 'text.secondary' }}>
        <Typography variant="h6">Noch keine Epics</Typography>
        <Typography variant="body2">
          Lege über „Neues Item" einen Eintrag vom Typ Epic an.
        </Typography>
      </Paper>
    );
  }

  return (
    <Stack spacing={1.5}>
      {epics.map((epic) => {
        const hue = epicColor(epic.id);
        const { done, total } = epic.progress;
        const pct = total > 0 ? Math.round((done / total) * 100) : 0;
        const excerpt = stripMarkdown(epic.body);
        return (
          <Paper
            key={epic.id}
            variant="outlined"
            role="button"
            tabIndex={0}
            aria-label={`Epic öffnen: ${epic.title}`}
            onClick={() => onOpen(epic)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onOpen(epic);
              }
            }}
            sx={{
              p: 2,
              borderLeft: `4px solid ${hue}`,
              cursor: 'pointer',
              transition: 'box-shadow 150ms',
              '&:hover': { boxShadow: 3 },
            }}
          >
            <Stack direction="row" alignItems="center" spacing={1}>
              <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: hue, flexShrink: 0 }} />
              <Typography variant="caption" sx={{ fontWeight: 700, color: hue }}>
                {epicShortcode(epic.title, epic.shortcode)}
              </Typography>
              <Typography sx={{ fontWeight: 600 }}>{epic.title}</Typography>
            </Stack>
            {excerpt && (
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{
                  mt: 0.5,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
              >
                {excerpt}
              </Typography>
            )}
            <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mt: 1 }}>
              <LinearProgress
                variant="determinate"
                value={pct}
                aria-label={`Fortschritt ${done} von ${total}`}
                sx={{
                  flex: 1,
                  height: 8,
                  borderRadius: 4,
                  '& .MuiLinearProgress-bar': { backgroundColor: hue },
                }}
              />
              <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
                {done}/{total} Stories fertig
              </Typography>
            </Stack>
          </Paper>
        );
      })}
    </Stack>
  );
}
