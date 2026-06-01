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

/** Padding in MUI-Spacing-Units (1 Unit = 8px). Default 2 (= 16px, bisheriges Verhalten). */
const DEFAULT_PADDING = 2;
const MAX_PADDING = 8;

function clampPadding(value: unknown): number {
  const n = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
  if (!Number.isFinite(n)) return DEFAULT_PADDING;
  return Math.min(MAX_PADDING, Math.max(0, Math.round(n)));
}

interface TextboxConfig {
  markdown: string;
  paddingTop: number;
  paddingLeft: number;
  paddingRight: number;
  paddingBottom: number;
  showBorder: boolean;
  backgroundColor?: string;
}

function parseConfig(raw: string): TextboxConfig {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return {
      markdown: typeof parsed.markdown === 'string' ? parsed.markdown : '',
      paddingTop: clampPadding(parsed.paddingTop),
      paddingLeft: clampPadding(parsed.paddingLeft),
      paddingRight: clampPadding(parsed.paddingRight),
      paddingBottom: clampPadding(parsed.paddingBottom),
      ...parseSurfaceConfig(parsed),
    };
  } catch {
    return {
      markdown: '',
      paddingTop: DEFAULT_PADDING,
      paddingLeft: DEFAULT_PADDING,
      paddingRight: DEFAULT_PADDING,
      paddingBottom: DEFAULT_PADDING,
      showBorder: false,
    };
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
  const [draftPadTop, setDraftPadTop] = useState(String(config.paddingTop));
  const [draftPadLeft, setDraftPadLeft] = useState(String(config.paddingLeft));
  const [draftPadRight, setDraftPadRight] = useState(String(config.paddingRight));
  const [draftPadBottom, setDraftPadBottom] = useState(String(config.paddingBottom));
  const [draftShowBorder, setDraftShowBorder] = useState(config.showBorder);
  const [draftBackgroundColor, setDraftBackgroundColor] = useState(config.backgroundColor ?? '');
  const paperRef = useRef<HTMLDivElement | null>(null);

  // Beim Öffnen den Draft auf den aktuellen Stand setzen — auch wenn der
  // Drawer schon einmal mit Abbrechen verworfen wurde.
  useEffect(() => {
    if (open) {
      setDraft(config.markdown);
      setDraftPadTop(String(config.paddingTop));
      setDraftPadLeft(String(config.paddingLeft));
      setDraftPadRight(String(config.paddingRight));
      setDraftPadBottom(String(config.paddingBottom));
      setDraftShowBorder(config.showBorder);
      setDraftBackgroundColor(config.backgroundColor ?? '');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
      paddingTop: clampPadding(draftPadTop),
      paddingLeft: clampPadding(draftPadLeft),
      paddingRight: clampPadding(draftPadRight),
      paddingBottom: clampPadding(draftPadBottom),
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
      sx={{
        pt: config.paddingTop,
        pl: config.paddingLeft,
        pr: config.paddingRight,
        pb: config.paddingBottom,
        height: '100%',
        position: 'relative',
        overflow: 'auto',
        ...surface.sx,
      }}
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
                sx={{
                  pt: clampPadding(draftPadTop),
                  pl: clampPadding(draftPadLeft),
                  pr: clampPadding(draftPadRight),
                  pb: clampPadding(draftPadBottom),
                  mt: 0.5,
                  minHeight: 80,
                }}
                aria-label="Live-Vorschau"
              >
                <ReactMarkdown>{draft}</ReactMarkdown>
              </Paper>
            </Box>
            <Divider textAlign="left">Abstände zum Rahmen (0–8)</Divider>
            <Stack direction="row" spacing={1}>
              <TextField
                label="Oben"
                type="number"
                value={draftPadTop}
                onChange={(e) => setDraftPadTop(e.target.value)}
                fullWidth
                inputProps={{ min: 0, max: MAX_PADDING, 'aria-label': 'Abstand oben' }}
              />
              <TextField
                label="Unten"
                type="number"
                value={draftPadBottom}
                onChange={(e) => setDraftPadBottom(e.target.value)}
                fullWidth
                inputProps={{ min: 0, max: MAX_PADDING, 'aria-label': 'Abstand unten' }}
              />
            </Stack>
            <Stack direction="row" spacing={1}>
              <TextField
                label="Links"
                type="number"
                value={draftPadLeft}
                onChange={(e) => setDraftPadLeft(e.target.value)}
                fullWidth
                inputProps={{ min: 0, max: MAX_PADDING, 'aria-label': 'Abstand links' }}
              />
              <TextField
                label="Rechts"
                type="number"
                value={draftPadRight}
                onChange={(e) => setDraftPadRight(e.target.value)}
                fullWidth
                inputProps={{ min: 0, max: MAX_PADDING, 'aria-label': 'Abstand rechts' }}
              />
            </Stack>
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
