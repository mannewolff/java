import { Box, Paper, Stack, Typography } from '@mui/material';
import type { SvgIconProps } from '@mui/material';
import type { ComponentType } from 'react';
import TextSnippetIcon from '@mui/icons-material/TextSnippet';
import InsightsIcon from '@mui/icons-material/Insights';
import ShowChartIcon from '@mui/icons-material/ShowChart';
import ViewListIcon from '@mui/icons-material/ViewList';
import HorizontalRuleIcon from '@mui/icons-material/HorizontalRule';

import type { WidgetType } from '../../api/dashboard';

/**
 * Metadaten je Widget-Typ für die Palette: anzeigbarer Name + Icon-Komponente.
 * Reihenfolge im Array == Reihenfolge in der Palette.
 */
interface PaletteEntry {
  type: WidgetType;
  label: string;
  icon: ComponentType<SvgIconProps>;
}

const PALETTE_ENTRIES: ReadonlyArray<PaletteEntry> = [
  { type: 'TEXTBOX', label: 'Textbox', icon: TextSnippetIcon },
  { type: 'KPI', label: 'KPI', icon: InsightsIcon },
  { type: 'PLOT', label: 'Plot', icon: ShowChartIcon },
  { type: 'KANBAN_LIST', label: 'Kanban-Liste', icon: ViewListIcon },
  { type: 'DIVIDER', label: 'Trennlinie', icon: HorizontalRuleIcon },
];

interface Props {
  /**
   * Wird vom Parent gerufen, wenn eine Palette-Kachel den Drag startet.
   * Der Parent (DashboardPage) merkt sich den Typ, damit der react-grid-layout
   * onDrop-Callback weiß, welchen Widget-Typ er platzieren soll.
   */
  onDragStartWidget: (type: WidgetType) => void;
}

/**
 * Sidebar-Inhalt im Dashboard-Edit-Modus. Ersetzt die normale Navigation und zeigt
 * eine Liste draggable Widget-Kacheln. Jede Kachel ist ein HTML5-draggable Element
 * mit Icon und Label. Das Drop-Ziel ist das Canvas in DashboardPage; dort entscheidet
 * react-grid-layout via isDroppable/onDrop, wohin das Widget kommt.
 */
export default function WidgetPalette({ onDragStartWidget }: Props): JSX.Element {
  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="overline" color="text.secondary">
        Widgets
      </Typography>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
        Zum Hinzufügen auf das Dashboard ziehen.
      </Typography>
      <Stack spacing={1.5}>
        {PALETTE_ENTRIES.map((entry) => {
          const Icon = entry.icon;
          return (
            <Paper
              key={entry.type}
              variant="outlined"
              // react-grid-layout's `isDroppable` reagiert auf HTML5-Drag-Events.
              // Die "droppable-element"-Klasse ist die Konvention, die das Lib
              // erkennt; alternativ funktioniert es auch über onDragStart mit
              // beliebigem MIME-Type — wir setzen beides, damit es robust ist.
              className="droppable-element"
              draggable
              unselectable="on"
              onDragStart={(e) => {
                e.dataTransfer.setData('text/plain', entry.type);
                e.dataTransfer.effectAllowed = 'move';
                onDragStartWidget(entry.type);
              }}
              sx={{
                p: 1.5,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                cursor: 'grab',
                transition: 'border-color 120ms, transform 120ms',
                '&:hover': {
                  borderColor: 'primary.main',
                  transform: 'translateY(-1px)',
                },
                '&:active': { cursor: 'grabbing' },
              }}
              aria-label={`Widget ${entry.label} hinzufügen`}
            >
              <Icon fontSize="large" color="primary" />
              <Typography variant="body2" sx={{ mt: 0.5, fontWeight: 500 }}>
                {entry.label}
              </Typography>
            </Paper>
          );
        })}
      </Stack>
    </Box>
  );
}
