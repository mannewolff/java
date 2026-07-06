import { useEffect, useState } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';

import { getKanbanEpics } from '../../api/kanban';
import type { KanbanEpic, KanbanItemType } from '../../api/kanban';
import { epicShortcode } from './epicMeta';
import { CREATE_BUTTON_BG, CREATE_BUTTON_HOVER } from './statusColors';

const BODY_TEMPLATE =
  '## Kontext\n\n## Aufgabe\n\n## Akzeptanzkriterium\n\n## Abhängigkeiten\n';

interface KanbanNewItemModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (
    title: string,
    body: string,
    type: KanbanItemType,
    parentId: number | null,
  ) => Promise<void> | void;
  /** Vorbelegtes Epic (z. B. „+ Neue Story" aus der Epic-Detailansicht, #326). */
  defaultParentId?: number | null;
}

/**
 * Zentriertes Anlage-Modal fuer neue Kanban-Eintraege (Issue #303/#324), analog der Kit-Referenz
 * `kit/board-ui.mjs`: Typ (Item/Epic) + optionale Epic-Zuordnung, Titel und Beschreibung
 * vorbefuellt mit einer vierteiligen Vorlage, keine Live-Vorschau. Bei Typ=Epic entfaellt die
 * Epic-Auswahl (ein Epic hat keinen Parent).
 */
export default function KanbanNewItemModal({
  open,
  onClose,
  onSubmit,
  defaultParentId = null,
}: KanbanNewItemModalProps): JSX.Element {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState(BODY_TEMPLATE);
  const [type, setType] = useState<KanbanItemType>('ITEM');
  const [parentId, setParentId] = useState<number | null>(defaultParentId);
  const [epics, setEpics] = useState<KanbanEpic[]>([]);
  const [saving, setSaving] = useState(false);

  // Bei jedem Oeffnen frisch starten und die Epic-Liste laden.
  useEffect(() => {
    if (!open) return;
    setTitle('');
    setBody(BODY_TEMPLATE);
    setType('ITEM');
    setParentId(defaultParentId);
    let cancelled = false;
    getKanbanEpics()
      .then((loaded) => {
        if (!cancelled) setEpics(loaded);
      })
      .catch(() => {
        if (!cancelled) setEpics([]);
      });
    return () => {
      cancelled = true;
    };
  }, [open, defaultParentId]);

  const canSubmit = title.trim().length > 0;

  const handleCreate = async (): Promise<void> => {
    if (!canSubmit || saving) return;
    setSaving(true);
    // Ein Epic hat keinen Parent; sonst die gewaehlte Zuordnung (oder keine).
    const effectiveParent = type === 'EPIC' ? null : parentId;
    try {
      await onSubmit(title.trim(), body, type, effectiveParent);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      aria-labelledby="kanban-new-item-title"
    >
      <DialogTitle id="kanban-new-item-title">Neues Item</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 0.5 }}>
          <TextField
            select
            SelectProps={{ native: true }}
            label="Typ"
            value={type}
            onChange={(e) => setType(e.target.value as KanbanItemType)}
            inputProps={{ 'aria-label': 'Typ' }}
            fullWidth
          >
            <option value="ITEM">Item</option>
            <option value="EPIC">Epic</option>
          </TextField>
          {type === 'ITEM' && (
            <TextField
              select
              SelectProps={{ native: true }}
              label="Epic"
              value={parentId ?? ''}
              onChange={(e) => setParentId(e.target.value === '' ? null : Number(e.target.value))}
              inputProps={{ 'aria-label': 'Epic' }}
              InputLabelProps={{ shrink: true }}
              fullWidth
            >
              <option value="">(kein Epic)</option>
              {epics.map((epic) => (
                <option key={epic.id} value={epic.id}>
                  {epicShortcode(epic.title)} – {epic.title}
                </option>
              ))}
            </TextField>
          )}
          <TextField
            label="Titel"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            inputProps={{ maxLength: 200, 'aria-label': 'Titel' }}
            fullWidth
            autoFocus
          />
          <TextField
            label="Beschreibung"
            multiline
            minRows={8}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            inputProps={{ maxLength: 10_000, 'aria-label': 'Beschreibung' }}
            sx={{ '& textarea': { fontFamily: 'monospace' } }}
            fullWidth
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Abbrechen</Button>
        <Button
          variant="contained"
          onClick={() => void handleCreate()}
          disabled={!canSubmit || saving}
          sx={{
            backgroundColor: CREATE_BUTTON_BG,
            '&:hover': { backgroundColor: CREATE_BUTTON_HOVER },
          }}
        >
          Anlegen
        </Button>
      </DialogActions>
    </Dialog>
  );
}
