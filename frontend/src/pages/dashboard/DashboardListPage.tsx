import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
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
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import StarIcon from '@mui/icons-material/Star';
import StarBorderIcon from '@mui/icons-material/StarBorder';

import {
  createDashboard,
  deleteDashboard,
  listDashboards,
  setDefaultDashboard,
  type DashboardSummary,
} from '../../api/dashboard';
import { ApiError } from '../../api/client';

type LoadState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; dashboards: DashboardSummary[] };

export default function DashboardListPage(): JSX.Element {
  const navigate = useNavigate();
  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [createPending, setCreatePending] = useState(false);
  const [toDelete, setToDelete] = useState<DashboardSummary | null>(null);

  async function reload(): Promise<void> {
    try {
      const list = await listDashboards();
      setState({ kind: 'ready', dashboards: list });
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

  /**
   * Klick auf "+" legt direkt ein Dashboard mit dem Default-Namen "Neues Dashboard" an
   * und navigiert auf das frische Dashboard. Kein Modal, kein Inline-Input — Umbenennen
   * passiert im Dashboard selbst (Inline-Edit auf dem Namen, siehe #43).
   */
  async function handleCreate(): Promise<void> {
    if (createPending) return;
    setCreatePending(true);
    try {
      const created = await createDashboard('Neues Dashboard');
      navigate(`/dashboards/${created.id}`);
    } catch (e) {
      setState({
        kind: 'error',
        message: e instanceof ApiError ? e.message : 'Anlegen fehlgeschlagen',
      });
    } finally {
      setCreatePending(false);
    }
  }

  async function handleSetDefault(id: number): Promise<void> {
    await setDefaultDashboard(id);
    await reload();
  }

  async function handleConfirmDelete(): Promise<void> {
    if (!toDelete) return;
    await deleteDashboard(toDelete.id);
    setToDelete(null);
    await reload();
  }

  return (
    <Box>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 3 }}>
        <Typography variant="h4">Dashboards</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => void handleCreate()}
          disabled={createPending}
          aria-label="Neues Dashboard anlegen"
        >
          Neu
        </Button>
      </Stack>

      {state.kind === 'loading' && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }} aria-busy="true">
          <CircularProgress aria-label="Dashboards werden geladen" />
        </Box>
      )}

      {state.kind === 'error' && <Alert severity="error">{state.message}</Alert>}

      {state.kind === 'ready' && state.dashboards.length === 0 && (
        <Typography color="text.secondary">
          Noch keine Dashboards. Lege oben mit „Neu" eines an.
        </Typography>
      )}

      {state.kind === 'ready' && state.dashboards.length > 0 && (
        <List>
          {state.dashboards.map((d) => (
            <ListItem
              key={d.id}
              disablePadding
              secondaryAction={
                <Stack direction="row" spacing={0.5}>
                  <Tooltip title={d.isDefault ? 'Default-Dashboard' : 'Als Default markieren'}>
                    <span>
                      <IconButton
                        edge="end"
                        onClick={() => void handleSetDefault(d.id)}
                        disabled={d.isDefault}
                        aria-label={d.isDefault ? 'Bereits Default' : 'Als Default markieren'}
                      >
                        {d.isDefault ? <StarIcon color="primary" /> : <StarBorderIcon />}
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title="Löschen">
                    <IconButton
                      edge="end"
                      onClick={() => setToDelete(d)}
                      aria-label={`Dashboard ${d.name} löschen`}
                    >
                      <DeleteIcon />
                    </IconButton>
                  </Tooltip>
                </Stack>
              }
            >
              <ListItemButton onClick={() => navigate(`/dashboards/${d.id}`)}>
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography>{d.name}</Typography>
                      {d.isDefault && (
                        <Chip label="Default" size="small" color="primary" variant="outlined" />
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
        open={toDelete !== null}
        onClose={() => setToDelete(null)}
        aria-labelledby="dashboard-delete-title"
      >
        <DialogTitle id="dashboard-delete-title">Dashboard löschen?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Das Dashboard „{toDelete?.name}" und alle seine Widgets werden unwiderruflich
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
