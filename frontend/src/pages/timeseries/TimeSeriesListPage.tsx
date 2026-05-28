import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  MenuItem,
  Paper,
  Skeleton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ShowChartIcon from '@mui/icons-material/ShowChart';

import {
  createTimeSeries,
  deleteTimeSeries,
  listTimeSeries,
  type TimeSeriesDataType,
  type TimeSeriesSummary,
} from '../../api/timeseries';
import { ApiError } from '../../api/client';
import { useNotify } from '../../notify/NotifyProvider';

type LoadState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; series: TimeSeriesSummary[] };

interface CreateDraft {
  name: string;
  description: string;
  unit: string;
  dataType: TimeSeriesDataType;
}

const EMPTY_DRAFT: CreateDraft = {
  name: '',
  description: '',
  unit: '',
  dataType: 'DECIMAL',
};

export default function TimeSeriesListPage(): JSX.Element {
  const navigate = useNavigate();
  const notify = useNotify();
  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [createOpen, setCreateOpen] = useState(false);
  const [draft, setDraft] = useState<CreateDraft>(EMPTY_DRAFT);
  const [createPending, setCreatePending] = useState(false);
  const [toDelete, setToDelete] = useState<TimeSeriesSummary | null>(null);

  async function reload(): Promise<void> {
    try {
      const list = await listTimeSeries();
      setState({ kind: 'ready', series: list });
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
    setDraft(EMPTY_DRAFT);
    setCreateOpen(true);
  }

  async function handleCreate(): Promise<void> {
    if (createPending) return;
    if (!draft.name.trim() || !draft.unit.trim()) return;
    setCreatePending(true);
    try {
      const created = await createTimeSeries({
        name: draft.name.trim(),
        description: draft.description.trim() || undefined,
        unit: draft.unit.trim(),
        dataType: draft.dataType,
      });
      setCreateOpen(false);
      navigate(`/timeseries/${created.id}`);
    } catch (e) {
      notify.error(e instanceof ApiError ? e.message : 'Anlegen fehlgeschlagen');
    } finally {
      setCreatePending(false);
    }
  }

  async function handleConfirmDelete(): Promise<void> {
    if (!toDelete) return;
    try {
      await deleteTimeSeries(toDelete.id);
      setToDelete(null);
      notify.success('Zeitreihe gelöscht.');
      await reload();
    } catch (e) {
      setToDelete(null);
      notify.error(e instanceof ApiError ? e.message : 'Löschen fehlgeschlagen');
    }
  }

  const createDisabled = !draft.name.trim() || !draft.unit.trim() || createPending;

  return (
    <Box>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 3 }}>
        <Typography variant="h4">Zeitreihen</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={openCreate}
          aria-label="Neue Zeitreihe anlegen"
        >
          Neu
        </Button>
      </Stack>

      {state.kind === 'loading' && (
        <Stack spacing={1.5} aria-busy="true" aria-label="Zeitreihen werden geladen">
          <Skeleton variant="rectangular" height={56} />
          <Skeleton variant="rectangular" height={56} />
          <Skeleton variant="rectangular" height={56} />
        </Stack>
      )}

      {state.kind === 'error' && <Alert severity="error">{state.message}</Alert>}

      {state.kind === 'ready' && state.series.length === 0 && (
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
          <ShowChartIcon sx={{ fontSize: 56, color: 'action.disabled' }} />
          <Typography variant="h6">Noch keine Zeitreihen</Typography>
          <Typography variant="body2">
            Lege deine erste Zeitreihe an — z. B. Gewicht, Temperatur oder Stromzähler.
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={openCreate}
            aria-label="Erste Zeitreihe anlegen"
          >
            Erste Zeitreihe anlegen
          </Button>
        </Paper>
      )}

      {state.kind === 'ready' && state.series.length > 0 && (
        <List>
          {state.series.map((ts) => (
            <ListItem
              key={ts.id}
              disablePadding
              secondaryAction={
                <Tooltip title="Löschen">
                  <IconButton
                    edge="end"
                    onClick={() => setToDelete(ts)}
                    aria-label={`Zeitreihe ${ts.name} löschen`}
                  >
                    <DeleteIcon />
                  </IconButton>
                </Tooltip>
              }
            >
              <ListItemButton onClick={() => navigate(`/timeseries/${ts.id}`)}>
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography>{ts.name}</Typography>
                      <Chip label={ts.unit} size="small" variant="outlined" />
                      <Chip
                        label={ts.dataType === 'INTEGER' ? 'Ganzzahl' : 'Dezimal'}
                        size="small"
                        color={ts.dataType === 'INTEGER' ? 'secondary' : 'default'}
                        variant="outlined"
                      />
                    </Stack>
                  }
                  secondary={
                    <Stack direction="row" spacing={1} sx={{ mt: 0.5 }}>
                      <Typography variant="caption" color="text.secondary">
                        {ts.entryCount} {ts.entryCount === 1 ? 'Eintrag' : 'Einträge'}
                      </Typography>
                      {ts.description && (
                        <Typography variant="caption" color="text.secondary">
                          · {ts.description}
                        </Typography>
                      )}
                    </Stack>
                  }
                />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      )}

      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        aria-labelledby="timeseries-create-title"
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle id="timeseries-create-title">Neue Zeitreihe</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Name"
              required
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              autoFocus
              inputProps={{ maxLength: 200 }}
            />
            <TextField
              label="Einheit"
              required
              value={draft.unit}
              onChange={(e) => setDraft({ ...draft, unit: e.target.value })}
              placeholder="z. B. kg, °C, kWh"
              inputProps={{ maxLength: 50 }}
            />
            <TextField
              label="Datentyp"
              select
              value={draft.dataType}
              onChange={(e) =>
                setDraft({ ...draft, dataType: e.target.value as TimeSeriesDataType })
              }
            >
              <MenuItem value="DECIMAL">Dezimal</MenuItem>
              <MenuItem value="INTEGER">Ganzzahl</MenuItem>
            </TextField>
            <TextField
              label="Beschreibung (optional)"
              value={draft.description}
              onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              multiline
              minRows={2}
              inputProps={{ maxLength: 500 }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Abbrechen</Button>
          <Button
            variant="contained"
            onClick={() => void handleCreate()}
            disabled={createDisabled}
          >
            Anlegen
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={toDelete !== null}
        onClose={() => setToDelete(null)}
        aria-labelledby="timeseries-delete-title"
      >
        <DialogTitle id="timeseries-delete-title">Zeitreihe löschen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Die Zeitreihe „{toDelete?.name}" und alle ihre Einträge werden unwiderruflich
            entfernt.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setToDelete(null)}>Abbrechen</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => void handleConfirmDelete()}
          >
            Löschen
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
