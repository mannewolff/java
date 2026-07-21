import { useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import SaveIcon from '@mui/icons-material/Save';

import { useAuth } from '../../auth/useAuth';
import { useNotify } from '../../notify/NotifyProvider';
import {
  HTTP_METHODS,
  METHODS_WITH_BODY,
  buildHeaders,
  deleteSavedRequest,
  isValidJsonBody,
  loadSavedRequests,
  resolveSameOriginUrl,
  saveRequest,
  type AuthMode,
  type HeaderPair,
  type HttpMethod,
  type RequestDraft,
  type SavedRequest,
} from './apiConsole';

interface ResponseView {
  status: number;
  statusText: string;
  durationMs: number;
  headers: [string, string][];
  body: string;
}

function prettyBody(text: string): string {
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
}

function statusColor(status: number): 'success' | 'warning' | 'error' | 'default' {
  if (status >= 200 && status < 300) return 'success';
  if (status >= 300 && status < 400) return 'warning';
  if (status >= 400) return 'error';
  return 'default';
}

/**
 * API-Konsole (#211) — „Mini-Postman" für die eigene Toolbox-API. Baut Requests gegen `/api/*`
 * (same-origin erzwungen), feuert sie ab, zeigt den Response und speichert Requests in
 * localStorage (ohne Token-Werte). Auth-Modus deckt Bearer (User-API) und Ingest-Token ab.
 */
export default function ApiConsolePage(): JSX.Element {
  const { accessToken } = useAuth();
  const notify = useNotify();

  const [method, setMethod] = useState<HttpMethod>('GET');
  const [path, setPath] = useState('/api/timeseries');
  const [headers, setHeaders] = useState<HeaderPair[]>([]);
  const [body, setBody] = useState('');
  const [authMode, setAuthMode] = useState<AuthMode>('bearer');
  const [bearerToken, setBearerToken] = useState(accessToken ?? '');
  const [ingestToken, setIngestToken] = useState('');

  const [response, setResponse] = useState<ResponseView | null>(null);
  const [errorText, setErrorText] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  const [saveName, setSaveName] = useState('');
  const [saved, setSaved] = useState<SavedRequest[]>(() => loadSavedRequests());

  const draft: RequestDraft = useMemo(
    () => ({ method, path, headers, body, authMode }),
    [method, path, headers, body, authMode],
  );

  const allowsBody = METHODS_WITH_BODY.includes(method);
  const jsonValid = isValidJsonBody(body);
  const pathValid = resolveSameOriginUrl(path) !== null;
  const canSend = pathValid && (!allowsBody || jsonValid) && !sending;

  const updateHeader = (index: number, patch: Partial<HeaderPair>): void => {
    setHeaders((prev) => prev.map((h, i) => (i === index ? { ...h, ...patch } : h)));
  };
  const addHeader = (): void => setHeaders((prev) => [...prev, { key: '', value: '' }]);
  const removeHeader = (index: number): void =>
    setHeaders((prev) => prev.filter((_, i) => i !== index));

  const handleSend = async (): Promise<void> => {
    const url = resolveSameOriginUrl(path);
    if (!url) {
      setErrorText('Nur Anfragen an die eigene API erlaubt (same-origin, z. B. /api/…).');
      return;
    }
    setSending(true);
    setErrorText(null);
    setResponse(null);

    const finalHeaders = buildHeaders(draft, { bearer: bearerToken, ingest: ingestToken });
    const init: RequestInit = { method, headers: finalHeaders };
    if (allowsBody && body.trim() !== '') {
      if (!finalHeaders['Content-Type']) finalHeaders['Content-Type'] = 'application/json';
      init.body = body;
    }

    const start = performance.now();
    try {
      const res = await fetch(url, init);
      const text = await res.text();
      setResponse({
        status: res.status,
        statusText: res.statusText,
        durationMs: Math.round(performance.now() - start),
        headers: Array.from(res.headers.entries()),
        body: text,
      });
    } catch (err) {
      setErrorText(err instanceof Error ? err.message : 'Netzwerkfehler');
    } finally {
      setSending(false);
    }
  };

  const handleSave = (): void => {
    if (saveName.trim() === '') return;
    setSaved(saveRequest(saveName, draft));
    notify.success(`Request „${saveName.trim()}" gespeichert`);
    setSaveName('');
  };

  const handleLoad = (req: SavedRequest): void => {
    setMethod(req.method);
    setPath(req.path);
    setHeaders(req.headers);
    setBody(req.body);
    setAuthMode(req.authMode);
    // Token-Werte werden nicht gespeichert: Bearer aus der Session neu befüllen, Ingest leeren.
    setBearerToken(accessToken ?? '');
    setIngestToken('');
    setResponse(null);
    setErrorText(null);
  };

  const handleDelete = (id: string): void => {
    setSaved(deleteSavedRequest(id));
  };

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>
        API-Konsole
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Teste die eigene Toolbox-API direkt im Browser. Nur Anfragen an diese Anwendung
        (same-origin, <code>/api/…</code>) sind erlaubt.
      </Typography>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Stack spacing={2}>
          <Stack direction="row" spacing={1}>
            <TextField
              select
              label="Methode"
              value={method}
              onChange={(e) => setMethod(e.target.value as HttpMethod)}
              sx={{ minWidth: 120 }}
            >
              {HTTP_METHODS.map((m) => (
                <MenuItem key={m} value={m}>
                  {m}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Pfad"
              value={path}
              onChange={(e) => setPath(e.target.value)}
              fullWidth
              error={path.trim() !== '' && !pathValid}
              helperText={
                path.trim() !== '' && !pathValid
                  ? 'Nur eigene API (same-origin, z. B. /api/timeseries)'
                  : ' '
              }
              inputProps={{ 'aria-label': 'Pfad' }}
            />
          </Stack>

          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Authentifizierung
            </Typography>
            <ToggleButtonGroup
              exclusive
              size="small"
              value={authMode}
              onChange={(_, v: AuthMode | null) => v && setAuthMode(v)}
              aria-label="Auth-Modus"
            >
              <ToggleButton value="none">Kein</ToggleButton>
              <ToggleButton value="bearer">Bearer (Session)</ToggleButton>
              <ToggleButton value="ingest">Ingest-Token</ToggleButton>
            </ToggleButtonGroup>

            {authMode === 'bearer' && (
              <Stack direction="row" spacing={1} alignItems="flex-start" sx={{ mt: 1 }}>
                <TextField
                  label="Bearer-Token"
                  value={bearerToken}
                  onChange={(e) => setBearerToken(e.target.value)}
                  fullWidth
                  size="small"
                  inputProps={{ 'aria-label': 'Bearer-Token' }}
                />
                <Button
                  size="small"
                  onClick={() => setBearerToken(accessToken ?? '')}
                  sx={{ whiteSpace: 'nowrap', mt: 0.5 }}
                >
                  Session-Token
                </Button>
              </Stack>
            )}
            {authMode === 'ingest' && (
              <TextField
                label="Ingest-Token (X-Ingest-Token)"
                value={ingestToken}
                onChange={(e) => setIngestToken(e.target.value)}
                fullWidth
                size="small"
                sx={{ mt: 1 }}
                inputProps={{ 'aria-label': 'Ingest-Token' }}
              />
            )}
          </Box>

          <Box>
            <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
              <Typography variant="subtitle2">Header</Typography>
              <Tooltip title="Header hinzufügen">
                <IconButton size="small" aria-label="Header hinzufügen" onClick={addHeader}>
                  <AddIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Stack>
            <Stack spacing={1}>
              {headers.map((h, i) => (
                <Stack direction="row" spacing={1} key={i}>
                  <TextField
                    label="Name"
                    value={h.key}
                    onChange={(e) => updateHeader(i, { key: e.target.value })}
                    size="small"
                    sx={{ flex: 1 }}
                    inputProps={{ 'aria-label': `Header-Name ${i + 1}` }}
                  />
                  <TextField
                    label="Wert"
                    value={h.value}
                    onChange={(e) => updateHeader(i, { value: e.target.value })}
                    size="small"
                    sx={{ flex: 2 }}
                    inputProps={{ 'aria-label': `Header-Wert ${i + 1}` }}
                  />
                  <IconButton
                    size="small"
                    aria-label={`Header ${i + 1} entfernen`}
                    onClick={() => removeHeader(i)}
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Stack>
              ))}
            </Stack>
          </Box>

          {allowsBody && (
            <TextField
              label="Body (JSON)"
              value={body}
              onChange={(e) => setBody(e.target.value)}
              multiline
              minRows={4}
              fullWidth
              error={!jsonValid}
              helperText={!jsonValid ? 'Ungültiges JSON' : ' '}
              inputProps={{ 'aria-label': 'Body' }}
              sx={{ fontFamily: 'monospace' }}
            />
          )}

          <Stack direction="row" spacing={1} alignItems="center">
            <Button
              variant="contained"
              startIcon={<SendIcon />}
              onClick={() => void handleSend()}
              disabled={!canSend}
            >
              Senden
            </Button>
            <Divider orientation="vertical" flexItem />
            <TextField
              label="Als…"
              placeholder="Name"
              value={saveName}
              onChange={(e) => setSaveName(e.target.value)}
              size="small"
            />
            <Button
              startIcon={<SaveIcon />}
              onClick={handleSave}
              disabled={saveName.trim() === ''}
            >
              Speichern
            </Button>
          </Stack>
        </Stack>
      </Paper>

      {errorText && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {errorText}
        </Alert>
      )}

      {response && (
        <Paper sx={{ p: 2, mb: 3 }} aria-label="Response">
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
            <Chip
              label={`${response.status} ${response.statusText}`.trim()}
              color={statusColor(response.status)}
              size="small"
            />
            <Typography variant="caption" color="text.secondary">
              {response.durationMs} ms
            </Typography>
          </Stack>
          <Typography variant="subtitle2" gutterBottom>
            Body
          </Typography>
          <Box
            component="pre"
            sx={{
              bgcolor: 'action.hover',
              p: 1.5,
              borderRadius: 1,
              overflow: 'auto',
              maxHeight: 360,
              fontFamily: 'monospace',
              fontSize: 13,
              m: 0,
            }}
          >
            {response.body.trim() === '' ? '(leerer Body)' : prettyBody(response.body)}
          </Box>
        </Paper>
      )}

      {saved.length > 0 && (
        <Paper sx={{ p: 2 }}>
          <Typography variant="subtitle2" gutterBottom>
            Gespeicherte Requests
          </Typography>
          <Stack spacing={1}>
            {saved.map((req) => (
              <Stack direction="row" spacing={1} alignItems="center" key={req.id}>
                <Chip label={req.method} size="small" variant="outlined" />
                <Button
                  size="small"
                  onClick={() => handleLoad(req)}
                  sx={{ flex: 1, justifyContent: 'flex-start', textTransform: 'none' }}
                >
                  {req.name} — {req.path}
                </Button>
                <IconButton
                  size="small"
                  aria-label={`${req.name} löschen`}
                  onClick={() => handleDelete(req.id)}
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Stack>
            ))}
          </Stack>
        </Paper>
      )}
    </Box>
  );
}
