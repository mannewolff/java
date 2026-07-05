import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  Box,
  Breadcrumbs,
  IconButton,
  Link,
  Paper,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import { QRCodeSVG } from 'qrcode.react';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckIcon from '@mui/icons-material/Check';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';

import { mobilePairingUrl } from '../../auth/mobileDevice';
import { keycloakAccountConsoleUrl } from '../../auth/oidcConfig';
import { useNotify } from '../../notify/NotifyProvider';

/**
 * Desktop-Seite „Handy-Zugang" (#206). Zeigt einen QR-Code, der auf die Mobile-Seite mit
 * Pairing-Flag verweist. Das Handy scannt ihn, meldet sich einmalig bei Keycloak an und
 * erhält einen Offline-Token (~30 Tage). Es gibt bewusst keinen eigenen Pairing-Server —
 * Keycloak übernimmt Token, Refresh und Widerruf.
 */
export default function MobileAuthSettingsPage(): JSX.Element {
  const notify = useNotify();
  const [copied, setCopied] = useState(false);
  const pairingUrl = mobilePairingUrl();

  async function handleCopy(): Promise<void> {
    try {
      await navigator.clipboard.writeText(pairingUrl);
      setCopied(true);
      notify.success('Link kopiert');
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      notify.error('Kopieren fehlgeschlagen');
    }
  }

  return (
    <>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component={RouterLink} to="/settings" underline="hover" color="inherit">
          Einstellungen
        </Link>
        <Typography color="text.primary">Handy-Zugang</Typography>
      </Breadcrumbs>

      <Typography variant="h4" gutterBottom>
        Handy-Zugang
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3, maxWidth: 560 }}>
        Scanne den QR-Code mit der Kamera deines Handys. Du wirst einmalig zur Anmeldung
        geführt; danach bleibt dein Handy bis zu 30 Tage ohne Neuanmeldung eingeloggt.
      </Typography>

      <Paper sx={{ p: 3, maxWidth: 560 }}>
        <Stack spacing={3} alignItems="center">
          <Box
            // Ausnahme von "Farben nur über das Theme" (CLAUDE-react.md): Der QR-Code braucht
            // einen garantiert weißen Hintergrund für die Scanbarkeit, unabhängig vom Theme.
            sx={{ bgcolor: '#fff', p: 2, borderRadius: 1, lineHeight: 0 }}
            aria-label="QR-Code für den Handy-Zugang"
          >
            <QRCodeSVG value={pairingUrl} size={220} level="M" />
          </Box>

          <Box sx={{ width: '100%' }}>
            <Typography variant="subtitle2" gutterBottom>
              Link (falls der QR-Code nicht scannbar ist)
            </Typography>
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography
                variant="body2"
                component="code"
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
                {pairingUrl}
              </Typography>
              <Tooltip title={copied ? 'Kopiert' : 'Link kopieren'}>
                <IconButton
                  aria-label="Link kopieren"
                  onClick={() => void handleCopy()}
                  color={copied ? 'success' : 'default'}
                >
                  {copied ? <CheckIcon /> : <ContentCopyIcon />}
                </IconButton>
              </Tooltip>
            </Stack>
          </Box>

          <Alert severity="info" sx={{ width: '100%' }}>
            Gekoppelte Geräte und Sitzungen kannst du jederzeit in der{' '}
            <Link
              href={keycloakAccountConsoleUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              Konto-Verwaltung <OpenInNewIcon sx={{ fontSize: 14, verticalAlign: 'middle' }} />
            </Link>{' '}
            widerrufen.
          </Alert>
        </Stack>
      </Paper>
    </>
  );
}
