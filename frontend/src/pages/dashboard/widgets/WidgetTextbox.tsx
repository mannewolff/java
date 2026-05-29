import { useEffect, useRef, useState } from 'react';
import { CONFIG_DRAWER_WIDTH } from './drawerConstants';
import {
  Box,
  Button,
  Divider,
  Drawer,
  FormControlLabel,
  IconButton,
  Paper,
  Stack,
  Switch,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import ReactMarkdown from 'react-markdown';

import type { WidgetDto } from '../../../api/dashboard';
import { parseSurfaceConfig, widgetSurface } from './widgetSurface';

interface TextboxConfig {
  markdown: string;
  showBorder: boolean;
  backgroundColor?: string;
}

function parseConfig(raw: string): TextboxConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return {
      markdown: typeof parsed.markdown === 'string' ? parsed.markdown : '',
      ...parseSurfaceConfig(parsed),
    };
  } catch {
    return { markdown: '', showBorder: false };
  }
}

interface Props {
  widget: WidgetDto;
  onChange: (next: WidgetDto) => void;
  onDelete: () => void;
  /**
   * Read-Modus: keine Aktions-Icons, kein Drawer-Trigger. Default `false` für
   * Rückwärtskompatibilität mit den existierenden Tests (die immer im Edit-Modus rendern).
   */
  readOnly?: boolean;
  /**
   * Read-Modus: meldet die natürliche Pixel-Höhe des Markdown-Containers (inkl. Padding),
   * sobald der Inhalt re-flows. Die DashboardPage rechnet daraus Grid-Rows und expanded
   * das Widget bei Bedarf — nicht persistiert, gilt nur visuell im Read-Modus.
   */
  onContentHeight?: (pxHeight: number) => void;
}

/**
 * Markdown-Textbox-Widget. Rendert Markdown im Grid-Cell, Edit-Drawer rechts mit Live-Preview.
 *
 * Die Stop-Propagation auf `onMouseDown` der Aktions-Buttons ist wichtig, damit
 * react-grid-layout den Klick nicht als Drag-Start interpretiert.
 */
export default function WidgetTextbox({
  widget,
  onChange,
  onDelete,
  readOnly = false,
  onContentHeight,
}: Props): JSX.Element {
  const config = parseConfig(widget.config);
  const surface = widgetSurface(readOnly, config);
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(config.markdown);
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');
  const paperRef = useRef<HTMLDivElement | null>(null);

  // Beim Öffnen den Draft auf den aktuellen Stand setzen — auch wenn der
  // Drawer schon einmal mit Abbrechen verworfen wurde.
  useEffect(() => {
    if (open) {
      setDraft(config.markdown);
      setDraftShowBorder(config.showBorder);
      setDraftBackgroundColor(config.backgroundColor ?? '');
    }
  }, [open, config.markdown, config.showBorder, config.backgroundColor]);

  // Read-Modus: ResizeObserver am Paper meldet `scrollHeight` an die DashboardPage.
  // Greift, wenn der Browser horizontal schrumpft und der Markdown-Wrap mehr Zeilen
  // erzeugt als das Grid-Slot hoch ist — Eltern expandiert dann das Widget.
  // ResizeObserver feuert auch beim ersten observe, daher kein zusätzlicher initial-Trigger nötig.
  useEffect(() => {
    if (!readOnly || !onContentHeight) return;
    const node = paperRef.current;
    if (!node || typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(() => {
      onContentHeight(node.scrollHeight);
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, [readOnly, onContentHeight, config.markdown]);

  function handleApply(): void {
    const next: TextboxConfig = {
      markdown: draft,
      showBorder: draftShowBorder,
      ...(draftBackgroundColor.trim() !== ''
        ? { backgroundColor: draftBackgroundColor.trim() }
        : {}),
    };
    onChange({ ...widget, config: JSON.stringify(next) });
    setOpen(false);
  }

  function handleCancel(): void {
    setDraft(config.markdown);
    setOpen(false);
  }

  return (
    <Paper
      ref={paperRef}
      variant={surface.variant}
      elevation={surface.elevation}
      sx={{ p: 2, height: '100%', position: 'relative', overflow: 'auto', ...surface.sx }}
    >
      {!readOnly && (
        <Stack
          direction="row"
          spacing={0.5}
          sx={{ position: 'absolute', top: 4, right: 4 }}
        >
          <IconButton
            size="small"
            aria-label="Textbox bearbeiten"
            onClick={() => setOpen(true)}
            onMouseDown={(e) => e.stopPropagation()}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            aria-label="Textbox löschen"
            onClick={onDelete}
            onMouseDown={(e) => e.stopPropagation()}
          >
            <DeleteOutlineIcon fontSize="small" />
          </IconButton>
        </Stack>
      )}

      <Box sx={{ mt: 0.5, pr: readOnly ? 0 : 6 }}>
        <ReactMarkdown>{config.markdown}</ReactMarkdown>
      </Box>

      <Drawer
        anchor="right"
        open={open}
        onClose={handleCancel}
        PaperProps={{ sx: { width: CONFIG_DRAWER_WIDTH } }}
      >
        {/* Spacer in AppBar-Höhe — gleiches Muster wie OG-Image-Page. */}
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Textbox bearbeiten
          </Typography>
          <Stack spacing={2}>
            <TextField
              label="Markdown-Quelltext"
              multiline
              minRows={6}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              fullWidth
              inputProps={{ 'aria-label': 'Markdown-Quelltext' }}
            />
            <Box>
              <Typography variant="caption" color="text.secondary">
                Live-Vorschau
              </Typography>
              <Paper
                variant="outlined"
                sx={{ p: 2, mt: 0.5, minHeight: 80 }}
                aria-label="Live-Vorschau"
              >
                <ReactMarkdown>{draft}</ReactMarkdown>
              </Paper>
            </Box>
            <Divider textAlign="left">Darstellung</Divider>
            <FormControlLabel
              control={
                <Switch
                  checked={draftShowBorder}
                  onChange={(e) => setDraftShowBorder(e.target.checked)}
                />
              }
              label="Rahmen anzeigen"
            />
            <TextField
              label="Hintergrundfarbe (leer = transparent)"
              value={draftBackgroundColor}
              onChange={(e) => setDraftBackgroundColor(e.target.value)}
              fullWidth
              placeholder="z. B. #1e1e1e oder rgba(255,255,255,0.05)"
            />
            <Divider />
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button onClick={handleCancel}>Abbrechen</Button>
              <Button variant="contained" onClick={handleApply}>
                Übernehmen
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Drawer>
    </Paper>
  );
}
