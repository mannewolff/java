import { Children, isValidElement, useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ReactMarkdown from 'react-markdown';
import type { Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';

import {
  addKanbanComment,
  deleteKanbanComment,
  getKanbanEpics,
  listKanbanComments,
  updateKanbanComment,
} from '../../api/kanban';
import type { KanbanComment, KanbanEpic, KanbanItem } from '../../api/kanban';
import { useAuth } from '../../auth/useAuth';
import { cleanupCountdownLabel, cleanupDaysRemaining } from './cleanupCountdown';
import { COLUMN_LABELS } from './columnMeta';
import { epicShortcode } from './epicMeta';
import {
  MODAL_BORDER,
  MODAL_HEADER_BG,
  MODAL_TEXT_PRIMARY,
  MODAL_TEXT_SECONDARY,
  STATUS_COLORS,
} from './statusColors';
import KanbanAttachmentList from './KanbanAttachmentList';
import KanbanCommentForm from './KanbanCommentForm';
import KanbanCommentList from './KanbanCommentList';

interface KanbanDetailModalProps {
  open: boolean;
  item: KanbanItem;
  retentionDays: number;
  onClose: () => void;
  onSubmit: (
    title: string,
    body: string,
    parentId: number | null,
    dependencies: number[],
  ) => Promise<void> | void;
  /** Stellt ein archiviertes Item wieder her (#341). Nur relevant, wenn {@code item.archived}. */
  onRestore?: () => Promise<void> | void;
  /** Löscht ein archiviertes Item endgültig (#341), nach Bestätigung im Modal. */
  onForceDelete?: () => Promise<void> | void;
  /**
   * Persistiert den geänderten Body nach einem Task-Checkbox-Klick, ohne das Modal zu schließen
   * (#359). Wird nur im Lesemodus aufgerufen. Lehnt der Aufruf ab, nimmt das Modal die optimistische
   * Umschaltung zurück. Ohne diesen Callback sind die Checkboxen deaktiviert.
   */
  onToggleTask?: (body: string) => Promise<void> | void;
}

/**
 * Flippt in genau einer Body-Zeile die GitHub-Task-Markierung (`[ ]`↔`[x]`) und gibt den neuen Body
 * zurück (#359). {@code line} ist 1-basiert (mdast-Position). Zeilenenden bleiben erhalten; trägt die
 * Zeile keine Task-Markierung, bleibt der Body unverändert.
 */
export function toggleTaskLine(body: string, line: number): string {
  const newline = body.includes('\r\n') ? '\r\n' : '\n';
  const lines = body.split(/\r?\n/);
  const index = line - 1;
  if (index < 0 || index >= lines.length) return body;
  lines[index] = lines[index].replace(
    /^(\s*[-*+]\s+\[)([ xX])(\])/,
    (_match, prefix: string, mark: string, suffix: string) =>
      prefix + (mark === ' ' ? 'x' : ' ') + suffix,
  );
  return lines.join(newline);
}

/**
 * Parst die kommagetrennte Abhängigkeits-Eingabe in Nummern. Nur positive Ganzzahlen sind gültig;
 * Leerzeichen/Leer-Tokens werden ignoriert, Duplikate entfernt. {@code valid=false} bei jedem
 * nicht-numerischen oder nicht-positiven Token.
 */
export function parseDependencyInput(input: string): { deps: number[]; valid: boolean } {
  const tokens = input
    .split(',')
    .map((t) => t.trim())
    .filter((t) => t.length > 0);
  const deps: number[] = [];
  for (const token of tokens) {
    if (!/^\d+$/.test(token)) return { deps: [], valid: false };
    const n = Number(token);
    if (n <= 0) return { deps: [], valid: false };
    if (!deps.includes(n)) deps.push(n);
  }
  return { deps, valid: true };
}

/**
 * GitHub-artige Typografie für die gerenderte Beschreibung (Issue #358): dezenter Rahmen,
 * Abschnitts-Überschriften (h1/h2) mit Trennlinie darunter, ab h3 ohne Linie. Farben aus der
 * zentralen Kit-Palette. Nur für die Beschreibung, nicht für die Kommentare.
 */
const descriptionSx = {
  border: `1px solid ${MODAL_BORDER}`,
  borderRadius: 1,
  p: 2,
  '& :first-of-type': { mt: 0 },
  '& h1, & h2': {
    fontWeight: 600,
    fontSize: '1.15rem',
    mt: 2,
    pb: 0.5,
    borderBottom: `1px solid ${MODAL_BORDER}`,
  },
  '& h3, & h4': { fontWeight: 600, fontSize: '1rem', mt: 1.5, mb: 0.5 },
  '& p, & li': { lineHeight: 1.6, color: MODAL_TEXT_PRIMARY },
  '& ul, & ol': { pl: 3, my: 1 },
  '& code': {
    backgroundColor: '#f4f5f7',
    px: 0.5,
    borderRadius: '3px',
    fontFamily: 'monospace',
    fontSize: '0.85em',
  },
  '& pre': { backgroundColor: '#f4f5f7', p: 1.5, borderRadius: 1, overflowX: 'auto' },
  '& pre code': { backgroundColor: 'transparent', px: 0 },
} as const;

/**
 * Detail-Modal eines Kanban-Items. Es oeffnet im **Lesemodus**: der Markdown-Body wird gerendert
 * angezeigt, darunter die Kommentare (Vorlage board-ui). Ueber "Bearbeiten" wechselt es in den
 * **Edit-Modus** mit Titel-Feld und Roh-Markdown-Textarea; Speichern rendert wieder, Abbrechen
 * verwirft den Draft. Standalone und controlled — Open/Item-State liegt beim Aufrufer, damit das
 * Modal auch ausserhalb des Boards (Dashboard-Widget) wiederverwendbar ist.
 *
 * {@code DialogContent} scrollt vertikal, falls der Inhalt hoeher als der Viewport ist; der
 * Dialog waechst dank {@code scroll="paper"} nicht ueber das Browserfenster hinaus.
 */
export default function KanbanDetailModal({
  open,
  item,
  retentionDays,
  onClose,
  onSubmit,
  onRestore,
  onForceDelete,
  onToggleTask,
}: KanbanDetailModalProps): JSX.Element {
  const { username } = useAuth();
  // Defensive: Altdaten/Fixtures ohne dependencies-Feld dürfen das Modal nicht crashen.
  const itemDependencies = item.dependencies ?? [];
  const [editing, setEditing] = useState(false);
  // Lesemodus rendert aus renderedBody (nicht item.body), damit ein Task-Checkbox-Klick sofort
  // optimistisch umschalten kann (#359); bei PUT-Fehler wird zurückgesetzt.
  const [renderedBody, setRenderedBody] = useState(item.body);
  const [title, setTitle] = useState(item.title);
  const [body, setBody] = useState(item.body);
  const [parentId, setParentId] = useState<number | null>(item.parentId);
  const [dependencies, setDependencies] = useState<string>(itemDependencies.join(', '));
  const [depsError, setDepsError] = useState<string | null>(null);
  const [epics, setEpics] = useState<KanbanEpic[]>([]);
  const [saving, setSaving] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  // Nur normale Items lassen sich einem Epic zuordnen; ein Epic selbst bekommt keinen Parent (#339).
  const canAssignEpic = item.type !== 'EPIC';

  const [comments, setComments] = useState<KanbanComment[]>([]);
  const [loadingComments, setLoadingComments] = useState(false);
  const [commentBusy, setCommentBusy] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);

  // Beim Oeffnen in den Lesemodus zuruecksetzen und den Draft aus dem Item uebernehmen — verhindert,
  // dass ein verworfener Draft oder Edit-Zustand beim naechsten Oeffnen nachklingt.
  useEffect(() => {
    if (open) {
      setEditing(false);
      setTitle(item.title);
      setBody(item.body);
      setParentId(item.parentId);
      setDependencies((item.dependencies ?? []).join(', '));
      setDepsError(null);
      setConfirmingDelete(false);
    }
  }, [open, item.title, item.body, item.parentId, item.dependencies]);

  // Lesemodus-Body an das (ggf. neu geladene) Item angleichen — nach erfolgreicher Persistenz
  // eines Task-Toggles trägt item.body schon den neuen Stand, nach Fehler den alten (#359).
  useEffect(() => {
    setRenderedBody(item.body);
  }, [item.body]);

  // Epic-Liste fuer die Zuordnungs-Auswahl laden (nur relevant fuer normale Items).
  useEffect(() => {
    if (!open || !canAssignEpic) return;
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
  }, [open, canAssignEpic]);

  const refreshComments = useCallback(async (): Promise<void> => {
    setComments(await listKanbanComments(item.id));
  }, [item.id]);

  // Kommentare beim Oeffnen laden. Ein cancelled-Flag verhindert State-Updates, falls das Modal
  // vor Abschluss des Requests wieder geschlossen (bzw. die Komponente neu gerendert) wird.
  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setLoadingComments(true);
    setCommentError(null);
    listKanbanComments(item.id)
      .then((loaded) => {
        if (!cancelled) setComments(loaded);
      })
      .catch(() => {
        if (!cancelled) setCommentError('Kommentare konnten nicht geladen werden.');
      })
      .finally(() => {
        if (!cancelled) setLoadingComments(false);
      });
    return () => {
      cancelled = true;
    };
  }, [open, item.id]);

  const handleAddComment = async (text: string): Promise<boolean> => {
    setCommentBusy(true);
    setCommentError(null);
    try {
      await addKanbanComment(item.id, text);
      await refreshComments();
      return true;
    } catch {
      setCommentError('Kommentar konnte nicht gespeichert werden.');
      return false;
    } finally {
      setCommentBusy(false);
    }
  };

  const handleUpdateComment = async (id: number, text: string): Promise<boolean> => {
    setCommentError(null);
    try {
      await updateKanbanComment(item.id, id, text);
      await refreshComments();
      return true;
    } catch {
      setCommentError('Kommentar konnte nicht aktualisiert werden.');
      return false;
    }
  };

  const handleDeleteComment = async (id: number): Promise<void> => {
    setCommentError(null);
    try {
      await deleteKanbanComment(item.id, id);
      await refreshComments();
    } catch {
      setCommentError('Kommentar konnte nicht gelöscht werden.');
    }
  };

  const canSubmit = title.trim().length > 0;
  const { movedToDoneAt } = item;
  const showDoneHint = item.column === 'DONE' && movedToDoneAt != null;
  const daysRemaining =
    showDoneHint && movedToDoneAt != null
      ? cleanupDaysRemaining(movedToDoneAt, retentionDays)
      : 0;

  const startEditing = (): void => {
    setTitle(item.title);
    setBody(item.body);
    setParentId(item.parentId);
    setDependencies(itemDependencies.join(', '));
    setDepsError(null);
    setEditing(true);
  };

  const cancelEditing = (): void => {
    setEditing(false);
  };

  // ESC und Backdrop-Klick sollen sich wie „Abbrechen" verhalten: im Edit-Modus zurück in den
  // Lesemodus (Draft verwerfen), nur im Lesemodus das Modal schließen. MUIs onClose feuert bei
  // beidem — ohne diese Weiche würde ESC/Backdrop im Edit-Modus das Modal zumachen (#357).
  const handleDialogClose = (): void => {
    if (editing) {
      cancelEditing();
      return;
    }
    onClose();
  };

  const handleSave = async (): Promise<void> => {
    if (!canSubmit || saving) return;
    const { deps, valid } = parseDependencyInput(dependencies);
    if (!valid) {
      setDepsError('Nur positive Nummern, kommagetrennt (z. B. 12, 34).');
      return;
    }
    setDepsError(null);
    setSaving(true);
    try {
      // Ein Epic bekommt keinen Parent; sonst die (evtl. entfernte) Zuordnung uebergeben.
      await onSubmit(title.trim(), body, canAssignEpic ? parentId : null, deps);
      setEditing(false);
    } finally {
      setSaving(false);
    }
  };

  // Checkboxen sind nur klickbar, wenn ein Persistenz-Callback existiert und das Item nicht
  // archiviert ist (#359).
  const canToggleTasks = !item.archived && typeof onToggleTask === 'function';

  // Optimistischer Task-Toggle: sofort lokal umschalten, dann persistieren; bei Fehler zurück.
  const handleToggleTask = (line: number): void => {
    const next = toggleTaskLine(renderedBody, line);
    if (next === renderedBody) return;
    const previous = renderedBody;
    setRenderedBody(next);
    void Promise.resolve(onToggleTask?.(next)).catch(() => {
      setRenderedBody(previous);
    });
  };

  // Task-List-Checkboxen im gerenderten Markdown durch klickbare MUI-Checkboxen ersetzen (#359).
  // Die Quellzeile kommt aus der mdast-Position des <li>; der disabled Default-Input wird entfernt.
  const markdownComponents: Components = {
    li({ node, children, className, ...props }) {
      const isTask =
        typeof className === 'string' && className.split(/\s+/).includes('task-list-item');
      const line = node?.position?.start.line;
      if (!isTask || line == null) {
        return (
          <li className={className} {...props}>
            {children}
          </li>
        );
      }
      const inputChild = node?.children.find(
        (child) => child.type === 'element' && child.tagName === 'input',
      );
      const checked =
        inputChild != null && 'properties' in inputChild
          ? Boolean(inputChild.properties?.checked)
          : false;
      const label = Children.toArray(children).filter(
        (child) => !(isValidElement(child) && child.type === 'input'),
      );
      return (
        <Box
          component="li"
          sx={{ listStyleType: 'none', display: 'flex', alignItems: 'flex-start' }}
        >
          <Checkbox
            size="small"
            checked={checked}
            disabled={!canToggleTasks}
            onChange={() => handleToggleTask(line)}
            sx={{ p: 0, mr: 1, mt: '1px' }}
            inputProps={{ 'aria-label': 'Aufgabe umschalten' }}
          />
          <Box component="span" sx={{ flex: 1 }}>
            {label}
          </Box>
        </Box>
      );
    },
  };

  return (
    <Dialog
      open={open}
      onClose={handleDialogClose}
      scroll="paper"
      maxWidth="lg"
      fullWidth
      aria-labelledby="kanban-detail-title"
      PaperProps={{
        sx: {
          borderRadius: '8px',
          boxShadow: '0 8px 32px rgba(0,0,0,.24)',
          // Desktop-Arbeitsfläche: breiter (lg ≈ 1200px) und hoch genug, damit man wie im
          // GitHub-Issue-Panel viel auf einen Blick sieht, statt in einem schmalen Kasten zu scrollen.
          minHeight: '70vh',
        },
      }}
    >
      <DialogTitle
        id="kanban-detail-title"
        data-testid="kanban-detail-header"
        style={{ borderBottom: `1px solid ${MODAL_BORDER}`, backgroundColor: MODAL_HEADER_BG }}
      >
        <Stack direction="row" alignItems="center" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Chip
            label={COLUMN_LABELS[item.column]}
            size="small"
            sx={{
              bgcolor: STATUS_COLORS[item.column].bg,
              color: STATUS_COLORS[item.column].text,
              fontWeight: 600,
              borderRadius: '12px',
            }}
          />
          {item.number > 0 && (
            <Typography component="span" variant="body2" style={{ color: MODAL_TEXT_SECONDARY }}>
              #{item.number}
            </Typography>
          )}
          <Typography component="span" style={{ fontWeight: 600, color: MODAL_TEXT_PRIMARY }}>
            {item.title}
          </Typography>
          <Box sx={{ flexGrow: 1 }} />
          {!editing && (
            <Button size="small" variant="outlined" onClick={startEditing}>
              Bearbeiten
            </Button>
          )}
        </Stack>
      </DialogTitle>
      <DialogContent dividers sx={{ overflowY: 'auto' }}>
        <Stack spacing={2} sx={{ mt: 0.5 }}>
          {editing ? (
            <>
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
                label="Markdown-Beschreibung"
                multiline
                rows={8}
                value={body}
                onChange={(e) => setBody(e.target.value)}
                inputProps={{
                  maxLength: 10_000,
                  'aria-label': 'Markdown-Beschreibung',
                  style: { fontFamily: 'monospace' },
                }}
                sx={{ '& textarea': { resize: 'vertical' } }}
                fullWidth
              />
              {canAssignEpic && (
                <TextField
                  select
                  SelectProps={{ native: true }}
                  label="Epic"
                  value={parentId ?? ''}
                  onChange={(e) =>
                    setParentId(e.target.value === '' ? null : Number(e.target.value))
                  }
                  inputProps={{ 'aria-label': 'Epic' }}
                  InputLabelProps={{ shrink: true }}
                  fullWidth
                >
                  <option value="">(kein Epic)</option>
                  {epics.map((epic) => (
                    <option key={epic.id} value={epic.id}>
                      {epicShortcode(epic.title, epic.shortcode)} – {epic.title}
                    </option>
                  ))}
                </TextField>
              )}
              <TextField
                label="Abhängig von (Nummern, kommagetrennt)"
                value={dependencies}
                onChange={(e) => {
                  setDependencies(e.target.value);
                  if (depsError) setDepsError(null);
                }}
                error={depsError != null}
                helperText={depsError ?? 'z. B. 12, 34'}
                inputProps={{ 'aria-label': 'Abhängig von' }}
                fullWidth
              />
            </>
          ) : (
            <>
              <Box aria-label="Beschreibung" sx={descriptionSx}>
                <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                  {renderedBody}
                </ReactMarkdown>
              </Box>
              {itemDependencies.length > 0 && (
                <Typography variant="body2" color="text.secondary" aria-label="Abhängigkeiten">
                  Abhängig von: {itemDependencies.map((n) => `#${n}`).join(', ')}
                </Typography>
              )}
            </>
          )}
          {showDoneHint && (
            <Typography variant="caption" color="text.secondary">
              {cleanupCountdownLabel(daysRemaining)}
            </Typography>
          )}

          {!editing && (
            <>
              <Divider />
              <KanbanAttachmentList itemId={item.id} />
              <Divider />
              <Stack spacing={1.5}>
                <Typography variant="subtitle1">Kommentare</Typography>
                {commentError && <Alert severity="error">{commentError}</Alert>}
                {loadingComments ? (
                  <Stack alignItems="center" sx={{ py: 2 }}>
                    <CircularProgress size={24} aria-label="Kommentare werden geladen" />
                  </Stack>
                ) : (
                  <KanbanCommentList
                    comments={comments}
                    currentUsername={username}
                    onUpdate={handleUpdateComment}
                    onDelete={handleDeleteComment}
                  />
                )}
                <KanbanCommentForm onAdd={handleAddComment} busy={commentBusy} />
              </Stack>
            </>
          )}
        </Stack>
      </DialogContent>
      <Divider />
      <DialogActions>
        {editing ? (
          <>
            <Button onClick={cancelEditing}>Abbrechen</Button>
            <Button
              variant="contained"
              onClick={() => void handleSave()}
              disabled={!canSubmit || saving}
            >
              Speichern
            </Button>
          </>
        ) : (
          <>
            {item.archived && onRestore && (
              <Button onClick={() => void onRestore()}>Wiederherstellen</Button>
            )}
            {item.archived && onForceDelete && (
              <Button color="error" onClick={() => setConfirmingDelete(true)}>
                Endgültig löschen
              </Button>
            )}
            <Box sx={{ flexGrow: 1 }} />
            <Button onClick={onClose}>Schließen</Button>
          </>
        )}
      </DialogActions>

      <Dialog
        open={confirmingDelete}
        onClose={() => setConfirmingDelete(false)}
        aria-labelledby="kanban-force-delete-title"
      >
        <DialogTitle id="kanban-force-delete-title">Endgültig löschen?</DialogTitle>
        <DialogContent>
          <Typography>„{item.title}” wird unwiderruflich entfernt.</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmingDelete(false)}>Abbrechen</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => {
              setConfirmingDelete(false);
              void onForceDelete?.();
            }}
          >
            Endgültig löschen
          </Button>
        </DialogActions>
      </Dialog>
    </Dialog>
  );
}
