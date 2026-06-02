import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckIcon from '@mui/icons-material/Check';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import AspectRatioIcon from '@mui/icons-material/AspectRatio';
import CompressIcon from '@mui/icons-material/Compress';

import { useAuth } from '../auth/useAuth';
import { useNotify } from '../notify/NotifyProvider';

const SWAGGER_UI_PATH = '/api/swagger-ui.html';

/** Bildtools, die aus dem Hauptmenü ausgelagert wurden (#131) — Routen bleiben aktiv. */
const IMAGE_TOOLS = [
  {
    path: '/tools/remove-background',
    label: 'Hintergrund entfernen',
    description: 'Freisteller per KI — entfernt den Bildhintergrund',
    icon: AutoFixHighIcon,
  },
  {
    path: '/tools/og-image',
    label: 'Beitragsbild',
    description: 'Bild auf Standard-Format für Social-/OG-Vorschau zuschneiden',
    icon: AspectRatioIcon,
  },
  {
    path: '/tools/resize',
    label: 'Bild verkleinern',
    description: 'Bilder auf eine kleinere Auflösung herunterrechnen',
    icon: CompressIcon,
  },
] as const;

/** Maskiert den Token zu `eyJ…[letzte 6 Zeichen]`, ohne ihn ganz preiszugeben. */
function maskToken(token: string): string {
  if (token.length <= 9) return token;
  return `${token.slice(0, 3)}…${token.slice(-6)}`;
}

export default function SettingsPage() {
  const { accessToken } = useAuth();
  const notify = useNotify();
  const [copied, setCopied] = useState(false);
  const [toolsDialogOpen, setToolsDialogOpen] = useState(false);

  async function handleCopy(): Promise<void> {
    if (!accessToken) return;
    try {
      await navigator.clipboard.writeText(accessToken);
      setCopied(true);
      notify.success('Token kopiert');
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      notify.error('Kopieren fehlgeschlagen');
    }
  }

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Einstellungen
      </Typography>
      <List>
        <ListItem disablePadding>
          <ListItemButton component={RouterLink} to="/settings/tokens">
            <ListItemIcon>
              <VpnKeyIcon />
            </ListItemIcon>
            <ListItemText
              primary="Ingest-Tokens"
              secondary="Tokens für externen Schreibzugriff auf Zeitreihen verwalten"
            />
          </ListItemButton>
        </ListItem>
      </List>

      <Divider sx={{ my: 3 }} />

      <Stack direction="row" alignItems="center" spacing={0.5}>
        <Typography variant="h6">Bildverarbeitung</Typography>
        <Tooltip title="Bildtools anzeigen">
          <IconButton
            aria-label="Bildtools anzeigen"
            size="small"
            onClick={() => setToolsDialogOpen(true)}
          >
            <HelpOutlineIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </Stack>
      <Dialog
        open={toolsDialogOpen}
        onClose={() => setToolsDialogOpen(false)}
        fullWidth
        maxWidth="xs"
        aria-labelledby="image-tools-dialog-title"
      >
        <DialogTitle id="image-tools-dialog-title">Bildverarbeitung</DialogTitle>
        <DialogContent dividers>
          <List>
            {IMAGE_TOOLS.map((tool) => {
              const Icon = tool.icon;
              return (
                <ListItem key={tool.path} disablePadding>
                  <ListItemButton
                    component={RouterLink}
                    to={tool.path}
                    onClick={() => setToolsDialogOpen(false)}
                  >
                    <ListItemIcon>
                      <Icon />
                    </ListItemIcon>
                    <ListItemText primary={tool.label} secondary={tool.description} />
                  </ListItemButton>
                </ListItem>
              );
            })}
          </List>
        </DialogContent>
      </Dialog>

      <Divider sx={{ my: 3 }} />

      <Typography variant="h6" gutterBottom>
        Entwickler / API
      </Typography>
      <Stack spacing={2} sx={{ maxWidth: 560 }}>
        <Box>
          <Typography variant="subtitle2" gutterBottom>
            Bearer-Token
          </Typography>
          {accessToken ? (
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography
                variant="body2"
                component="code"
                aria-label="Maskierter Bearer-Token"
                sx={{
                  fontFamily: 'monospace',
                  bgcolor: 'action.hover',
                  px: 1,
                  py: 0.5,
                  borderRadius: 1,
                  flex: 1,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
              >
                {maskToken(accessToken)}
              </Typography>
              <Tooltip title={copied ? 'Kopiert' : 'Token kopieren'}>
                <IconButton
                  aria-label="Token kopieren"
                  onClick={() => void handleCopy()}
                  color={copied ? 'success' : 'default'}
                >
                  {copied ? <CheckIcon /> : <ContentCopyIcon />}
                </IconButton>
              </Tooltip>
            </Stack>
          ) : (
            <Typography variant="body2" color="text.secondary">
              Kein Token verfügbar.
            </Typography>
          )}
          <Typography variant="caption" color="text.secondary">
            Für die Authentifizierung in der Swagger UI: „Authorize” → Token einfügen.
          </Typography>
        </Box>

        <Box>
          <Button
            variant="outlined"
            startIcon={<OpenInNewIcon />}
            component="a"
            href={SWAGGER_UI_PATH}
            target="_blank"
            rel="noopener noreferrer"
          >
            Swagger UI öffnen
          </Button>
        </Box>
      </Stack>
    </>
  );
}
