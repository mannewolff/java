import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Link,
  Paper,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DeleteIcon from '@mui/icons-material/Delete';
import VpnKeyIcon from '@mui/icons-material/VpnKey';

import {
  createIngestToken,
  listIngestTokens,
  revokeIngestToken,
  type CreatedIngestToken,
  type IngestTokenSummary,
} from '../../api/ingestTokens';
import { ApiError } from '../../api/client';
import { useNotify } from '../../notify/NotifyProvider';

type LoadState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; tokens: IngestTokenSummary[] };

function formatLocal(ts: string | undefined): string {
  if (!ts) return '—';
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return ts;
  return d.toLocaleString();
}

export default function IngestTokenSettingsPage(): JSX.Element {
  const notify = useNotify();

  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createPending, setCreatePending] = useState(false);
  const [created, setCreated] = useState<CreatedIngestToken | null>(null);
  const [toRevoke, setToRevoke] = useState<IngestTokenSummary | null>(null);

  async function reload(): Promise<void> {
    try {
      const list = await listIngestTokens();
      setState({ kind: 'ready', tokens: list });
    } catch (e) {
      setState({
        kind: 'error',
        message: e instanceof ApiError ? e.message : 'Unbekannter Fehler',
      });
    }
  }

  useEffect(() => {
    void reload();
  }, []);

  function openCreate(): void {
    setCreateName('');
    setCreateOpen(true);
  }

  async function handleCreate(): Promise<void> {
    if (createPending) return;
    const name = createName.trim();
    if (!name) return;
    setCreatePending(true);
    try {
      const result = await createIngestToken(name);
      setCreateOpen(false);
      setCreated(result);
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Anlegen fehlgeschlagen');
    } finally {
      setCreatePending(false);
    }
  }

  async function handleCopy(): Promise<void> {
    if (!created) return;
    try {
      await navigator.clipboard.writeText(created.plaintext);
      notify.success('Token in Zwischenablage kopiert.');
    } catch {
      notify.error('Kopieren fehlgeschlagen — bitte manuell markieren.');
    }
  }

  async function handleConfirmRevoke(): Promise<void> {
    if (!toRevoke) return;
    try {
      await revokeIngestToken(toRevoke.id);
      setToRevoke(null);
      notify.success('Token widerrufen.');
      await reload();
    } catch (e) {
      setToRevoke(null);
      notify.error(e instanceof ApiError ? e.message : 'Widerrufen fehlgeschlagen');
    }
  }

  return (
    <Box>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component={RouterLink} to="/settings" underline="hover">
          Einstellungen
        </Link>
        <Typography color="text.primary">Ingest-Tokens</Typography>
      </Breadcrumbs>

      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
        <Typography variant="h4">Ingest-Tokens</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={openCreate}
          aria-label="Neuen Token erzeugen"
        >
          Neu
        </Button>
      </Stack>

      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Tokens erlauben externen Programmen (Sensoren, Cron-Jobs, Skripte), Werte in deine
        Zeitreihen zu schreiben — ohne Login. Behandle sie wie Passwörter.
      </Typography>

      {state.kind === 'loading' && (
        <Stack spacing={1.5} aria-busy="true" aria-label="Tokens werden geladen">
          <Skeleton variant="rectangular" height={56} />
          <Skeleton variant="rectangular" height={56} />
        </Stack>
      )}

      {state.kind === 'error' && <Alert severity="error">{state.message}</Alert>}

      {state.kind === 'ready' && state.tokens.length === 0 && (
        <Paper
          variant="outlined"
          sx={{
            p: 6,
            textAlign: 'center',
            color: 'text.secondary',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 2,
          }}
        >
          <VpnKeyIcon sx={{ fontSize: 56, color: 'action.disabled' }} />
          <Typography variant="h6">Noch keine Tokens</Typography>
          <Typography variant="body2">
            Erzeuge einen Token, um z. B. einem Raspberry Pi das Schreiben in deine Zeitreihen zu
            erlauben.
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Ersten Token erzeugen
          </Button>
        </Paper>
      )}

      {state.kind === 'ready' && state.tokens.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small" aria-label="Token-Liste">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Erstellt</TableCell>
                <TableCell>Zuletzt verwendet</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Aktion</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {state.tokens.map((t) => (
                <TableRow key={t.id}>
                  <TableCell>{t.name}</TableCell>
                  <TableCell>{formatLocal(t.createdAt)}</TableCell>
                  <TableCell>{formatLocal(t.lastUsedAt)}</TableCell>
                  <TableCell>
                    {t.revoked ? (
                      <Chip label="widerrufen" size="small" color="default" />
                    ) : (
                      <Chip label="aktiv" size="small" color="success" />
                    )}
                  </TableCell>
                  <TableCell align="right">
                    {!t.revoked && (
                      <Tooltip title="Widerrufen">
                        <IconButton
                          aria-label={`Token ${t.name} widerrufen`}
                          onClick={() => setToRevoke(t)}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Anlege-Dialog */}
      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        aria-labelledby="token-create-title"
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle id="token-create-title">Neuen Token erzeugen</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Name"
              required
              value={createName}
              onChange={(e) => setCreateName(e.target.value)}
              autoFocus
              placeholder="z. B. Raspberry Pi Wohnzimmer"
              inputProps={{ maxLength: 100, 'aria-label': 'Token-Name' }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Abbrechen</Button>
          <Button
            variant="contained"
            onClick={() => void handleCreate()}
            disabled={!createName.trim() || createPending}
          >
            Erzeugen
          </Button>
        </DialogActions>
      </Dialog>

      {/* Plaintext-Reveal-Dialog */}
      <Dialog
        open={created !== null}
        onClose={() => setCreated(null)}
        aria-labelledby="token-reveal-title"
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle id="token-reveal-title">Token erzeugt</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Diesen Token jetzt notieren — er wird nicht mehr angezeigt.
          </Alert>
          <Paper
            variant="outlined"
            sx={{
              p: 2,
              fontFamily: 'monospace',
              fontSize: 14,
              wordBreak: 'break-all',
              mb: 2,
            }}
            aria-label="Plaintext-Token"
          >
            {created?.plaintext}
          </Paper>
          <Button
            variant="outlined"
            startIcon={<ContentCopyIcon />}
            onClick={() => void handleCopy()}
            aria-label="In Zwischenablage kopieren"
          >
            In Zwischenablage kopieren
          </Button>
        </DialogContent>
        <DialogActions>
          <Button variant="contained" onClick={() => setCreated(null)}>
            Verstanden
          </Button>
        </DialogActions>
      </Dialog>

      {/* Widerrufen-Confirm */}
      <Dialog
        open={toRevoke !== null}
        onClose={() => setToRevoke(null)}
        aria-labelledby="token-revoke-title"
      >
        <DialogTitle id="token-revoke-title">Token widerrufen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Der Token „{toRevoke?.name}” wird sofort deaktiviert. Externe Programme, die ihn
            verwenden, bekommen ab sofort 401.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setToRevoke(null)}>Abbrechen</Button>
          <Button color="error" variant="contained" onClick={() => void handleConfirmRevoke()}>
            Widerrufen
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
