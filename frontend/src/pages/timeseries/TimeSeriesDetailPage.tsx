import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Chip,
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
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import EditIcon from '@mui/icons-material/Edit';

import {
  addEntry,
  getTimeSeries,
  listEntries,
  updateTimeSeries,
  type TimeSeriesEntry,
  type TimeSeriesSummary,
} from '../../api/timeseries';
import { ApiError } from '../../api/client';
import { useNotify } from '../../notify/NotifyProvider';

type LoadState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; summary: TimeSeriesSummary; entries: TimeSeriesEntry[] };

/** Liefert "YYYY-MM-DDTHH:MM" in lokaler Zeit fuer das datetime-local-Input. */
function localNowForInput(): string {
  const now = new Date();
  const tzAdjusted = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return tzAdjusted.toISOString().slice(0, 16);
}

/** Wandelt einen ISO-Timestamp in lokales "YYYY-MM-DD HH:MM:SS" um. */
function formatLocal(ts: string): string {
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return ts;
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  const hh = String(d.getHours()).padStart(2, '0');
  const mi = String(d.getMinutes()).padStart(2, '0');
  const ss = String(d.getSeconds()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
}

export default function TimeSeriesDetailPage(): JSX.Element {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const navigate = useNavigate();
  const notify = useNotify();

  const [state, setState] = useState<LoadState>({ kind: 'loading' });

  const [editingName, setEditingName] = useState(false);
  const [nameDraft, setNameDraft] = useState('');
  const [namePending, setNamePending] = useState(false);

  const [valueInput, setValueInput] = useState('');
  const [tsInput, setTsInput] = useState(localNowForInput());
  const [valueError, setValueError] = useState<string | null>(null);
  const [addPending, setAddPending] = useState(false);

  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(50);

  async function reload(): Promise<void> {
    try {
      const [summary, entries] = await Promise.all([
        getTimeSeries(id),
        listEntries(id, { limit: 1000 }),
      ]);
      setState({ kind: 'ready', summary, entries });
      setNameDraft(summary.name);
    } catch (e) {
      setState({
        kind: 'error',
        message: e instanceof ApiError ? e.message : 'Unbekannter Fehler',
      });
    }
  }

  useEffect(() => {
    if (Number.isNaN(id)) {
      setState({ kind: 'error', message: 'Ungültige Zeitreihen-ID' });
      return;
    }
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function handleSaveName(): Promise<void> {
    if (state.kind !== 'ready') return;
    const trimmed = nameDraft.trim();
    if (!trimmed || trimmed === state.summary.name) {
      setEditingName(false);
      setNameDraft(state.summary.name);
      return;
    }
    setNamePending(true);
    try {
      await updateTimeSeries(state.summary.id, {
        name: trimmed,
        description: state.summary.description,
        unit: state.summary.unit,
        dataType: state.summary.dataType,
      });
      setEditingName(false);
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Umbenennen fehlgeschlagen');
    } finally {
      setNamePending(false);
    }
  }

  function validateValue(raw: string, dataType: 'DECIMAL' | 'INTEGER'): string | null {
    const trimmed = raw.trim().replace(',', '.');
    if (!trimmed) return 'Wert erforderlich';
    const num = Number(trimmed);
    if (!Number.isFinite(num)) return 'Keine gültige Zahl';
    if (dataType === 'INTEGER' && !Number.isInteger(num)) {
      return 'Bei Ganzzahl-Zeitreihen sind keine Nachkommastellen erlaubt';
    }
    return null;
  }

  async function handleAddEntry(): Promise<void> {
    if (state.kind !== 'ready') return;
    const err = validateValue(valueInput, state.summary.dataType);
    if (err) {
      setValueError(err);
      return;
    }
    setValueError(null);
    setAddPending(true);
    try {
      const num = Number(valueInput.trim().replace(',', '.'));
      const localDate = new Date(tsInput);
      if (Number.isNaN(localDate.getTime())) {
        setValueError('Ungültiger Zeitpunkt');
        return;
      }
      await addEntry(state.summary.id, localDate.toISOString(), num);
      setValueInput('');
      setTsInput(localNowForInput());
      await reload();
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Eintrag fehlgeschlagen');
    } finally {
      setAddPending(false);
    }
  }

  const visibleEntries = useMemo(() => {
    if (state.kind !== 'ready') return [];
    const start = page * rowsPerPage;
    return state.entries.slice(start, start + rowsPerPage);
  }, [state, page, rowsPerPage]);

  return (
    <Box>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component={RouterLink} to="/timeseries" underline="hover">
          Zeitreihen
        </Link>
        <Typography color="text.primary">
          {state.kind === 'ready' ? state.summary.name : 'Detail'}
        </Typography>
      </Breadcrumbs>

      {state.kind === 'loading' && (
        <Stack spacing={1.5} aria-busy="true" aria-label="Zeitreihe wird geladen">
          <Skeleton variant="rectangular" height={48} />
          <Skeleton variant="rectangular" height={120} />
          <Skeleton variant="rectangular" height={200} />
        </Stack>
      )}

      {state.kind === 'error' && (
        <Stack spacing={2}>
          <Alert severity="error">{state.message}</Alert>
          <Button onClick={() => navigate('/timeseries')}>Zurück zur Liste</Button>
        </Stack>
      )}

      {state.kind === 'ready' && (
        <Stack spacing={3}>
          <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
            {editingName ? (
              <>
                <TextField
                  value={nameDraft}
                  onChange={(e) => setNameDraft(e.target.value)}
                  size="small"
                  autoFocus
                  inputProps={{ maxLength: 200, 'aria-label': 'Zeitreihen-Name' }}
                />
                <Tooltip title="Speichern">
                  <span>
                    <IconButton
                      color="primary"
                      onClick={() => void handleSaveName()}
                      disabled={namePending}
                      aria-label="Namen speichern"
                    >
                      <CheckIcon />
                    </IconButton>
                  </span>
                </Tooltip>
                <Tooltip title="Abbrechen">
                  <IconButton
                    onClick={() => {
                      setEditingName(false);
                      setNameDraft(state.summary.name);
                    }}
                    aria-label="Bearbeitung abbrechen"
                  >
                    <CloseIcon />
                  </IconButton>
                </Tooltip>
              </>
            ) : (
              <>
                <Typography variant="h4">{state.summary.name}</Typography>
                <Tooltip title="Namen bearbeiten">
                  <IconButton
                    onClick={() => {
                      setEditingName(true);
                      setNameDraft(state.summary.name);
                    }}
                    aria-label="Namen bearbeiten"
                  >
                    <EditIcon />
                  </IconButton>
                </Tooltip>
              </>
            )}
            <Chip label={state.summary.unit} variant="outlined" />
            <Chip
              label={state.summary.dataType === 'INTEGER' ? 'Ganzzahl' : 'Dezimal'}
              color={state.summary.dataType === 'INTEGER' ? 'secondary' : 'default'}
              variant="outlined"
            />
          </Stack>

          {state.summary.description && (
            <Typography variant="body2" color="text.secondary">
              {state.summary.description}
            </Typography>
          )}

          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Neuen Wert eintragen
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start">
              <TextField
                label={`Wert (${state.summary.unit})`}
                value={valueInput}
                onChange={(e) => {
                  setValueInput(e.target.value);
                  setValueError(null);
                }}
                error={Boolean(valueError)}
                helperText={valueError ?? ' '}
                inputProps={{
                  inputMode: state.summary.dataType === 'INTEGER' ? 'numeric' : 'decimal',
                  'aria-label': 'Wert',
                }}
                sx={{ minWidth: 180 }}
              />
              <TextField
                label="Zeitpunkt"
                type="datetime-local"
                value={tsInput}
                onChange={(e) => setTsInput(e.target.value)}
                InputLabelProps={{ shrink: true }}
                helperText=" "
                sx={{ minWidth: 220 }}
                inputProps={{ 'aria-label': 'Zeitpunkt' }}
              />
              <Button
                variant="contained"
                onClick={() => void handleAddEntry()}
                disabled={addPending}
                sx={{ mt: { xs: 0, sm: '6px' } }}
              >
                Eintragen
              </Button>
            </Stack>
          </Paper>

          <Paper variant="outlined">
            <Typography variant="h6" sx={{ p: 2 }}>
              Einträge ({state.entries.length})
            </Typography>
            {state.entries.length === 0 ? (
              <Box sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
                <Typography>Noch keine Einträge — erfasse den ersten oben.</Typography>
              </Box>
            ) : (
              <>
                <TableContainer>
                  <Table size="small" aria-label="Einträge der Zeitreihe">
                    <TableHead>
                      <TableRow>
                        <TableCell>Zeitpunkt</TableCell>
                        <TableCell align="right">Wert ({state.summary.unit})</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {visibleEntries.map((entry) => (
                        <TableRow key={entry.id}>
                          <TableCell>{formatLocal(entry.timestamp)}</TableCell>
                          <TableCell align="right">{entry.value}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                <TablePagination
                  component="div"
                  count={state.entries.length}
                  page={page}
                  onPageChange={(_, newPage) => setPage(newPage)}
                  rowsPerPage={rowsPerPage}
                  onRowsPerPageChange={(e) => {
                    setRowsPerPage(parseInt(e.target.value, 10));
                    setPage(0);
                  }}
                  rowsPerPageOptions={[25, 50, 100]}
                  labelRowsPerPage="Einträge pro Seite:"
                />
              </>
            )}
          </Paper>
        </Stack>
      )}
    </Box>
  );
}
