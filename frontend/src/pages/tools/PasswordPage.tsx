import { useEffect, useMemo, useState } from 'react';
import type { ChangeEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Divider,
  Drawer,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Paper,
  Slider,
  Snackbar,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import SettingsIcon from '@mui/icons-material/Settings';
import {
  DEFAULT_SPECIALS,
  entropyBits,
  generate,
  hashBcrypt,
} from '../../lib/password';
import { loadPasswordSettings, savePasswordSettings } from '../../lib/passwordSettings';

const MIN_LENGTH = 8;
const MAX_LENGTH = 64;
const DEFAULT_LENGTH = 20;
const MIN_COST = 8;
const MAX_COST = 12;
const DEFAULT_COST = 10;

interface SpecialsSelection {
  active: boolean;
  picked: ReadonlySet<string>;
}

const DEFAULT_SPECIALS_SET: ReadonlySet<string> = new Set(DEFAULT_SPECIALS);

function activeSpecials(sel: SpecialsSelection): string[] {
  if (!sel.active) return [];
  return DEFAULT_SPECIALS.filter((c) => sel.picked.has(c));
}

function alphabetSize(
  useUpper: boolean,
  useLower: boolean,
  useDigits: boolean,
  specials: string[],
): number {
  let size = 0;
  if (useUpper) size += 26;
  if (useLower) size += 26;
  if (useDigits) size += 10;
  size += specials.length;
  return size;
}

export default function PasswordPage() {
  // Beim ersten Render einmalig die persistierten Einstellungen laden (#178).
  const initial = useMemo(() => loadPasswordSettings(), []);

  // Applied state — used by the Generate button
  const [length, setLength] = useState(initial.length);
  const [useUpper, setUseUpper] = useState(initial.useUpper);
  const [useLower, setUseLower] = useState(initial.useLower);
  const [useDigits, setUseDigits] = useState(initial.useDigits);
  const [specials, setSpecials] = useState<SpecialsSelection>({
    active: initial.specialsActive,
    picked: new Set(initial.specialsPicked),
  });
  const [costFactor, setCostFactor] = useState(initial.costFactor);

  // Outputs
  const [password, setPassword] = useState<string>('');
  const [hash, setHash] = useState<string>('');
  const [isHashing, setIsHashing] = useState(false);
  const [generateError, setGenerateError] = useState<string | null>(null);
  const [snackbar, setSnackbar] = useState<string | null>(null);

  // Drawer + draft state
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [draftLength, setDraftLength] = useState(DEFAULT_LENGTH);
  const [draftUpper, setDraftUpper] = useState(true);
  const [draftLower, setDraftLower] = useState(true);
  const [draftDigits, setDraftDigits] = useState(true);
  const [draftSpecials, setDraftSpecials] = useState<SpecialsSelection>({
    active: true,
    picked: DEFAULT_SPECIALS_SET,
  });
  const [draftCost, setDraftCost] = useState(DEFAULT_COST);

  const activeSpecialChars = activeSpecials(specials);
  const noClassActive =
    !useUpper && !useLower && !useDigits && activeSpecialChars.length === 0;
  const draftActiveSpecials = activeSpecials(draftSpecials);
  const draftNoClassActive =
    !draftUpper && !draftLower && !draftDigits && draftActiveSpecials.length === 0;

  const handleGenerate = () => {
    setGenerateError(null);
    try {
      const pw = generate({
        length,
        useUpper,
        useLower,
        useDigits,
        specials: activeSpecialChars,
      });
      // setPassword fires the debounced hash effect below.
      setPassword(pw);
    } catch (err) {
      setGenerateError(err instanceof Error ? err.message : 'Unbekannter Fehler');
    }
  };

  const handlePasswordChange = (event: ChangeEvent<HTMLInputElement>) => {
    setGenerateError(null);
    setPassword(event.target.value);
  };

  // Debounced re-hash on every password change, no matter whether it comes
  // from clicking Generate or from the user typing into the input.
  useEffect(() => {
    if (!password) {
      setHash('');
      setIsHashing(false);
      return;
    }
    setIsHashing(true);
    let cancelled = false;
    const handle = window.setTimeout(async () => {
      try {
        const h = await hashBcrypt(password, costFactor);
        if (!cancelled) {
          setHash(h);
        }
      } finally {
        if (!cancelled) {
          setIsHashing(false);
        }
      }
    }, 400);
    return () => {
      cancelled = true;
      window.clearTimeout(handle);
    };
  }, [password, costFactor]);

  const copyToClipboard = async (value: string, label: string) => {
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      setSnackbar(label + ' kopiert');
    } catch {
      setSnackbar('Konnte nicht in Zwischenablage kopieren');
    }
  };

  const openSettings = () => {
    setDraftLength(length);
    setDraftUpper(useUpper);
    setDraftLower(useLower);
    setDraftDigits(useDigits);
    setDraftSpecials(specials);
    setDraftCost(costFactor);
    setDrawerOpen(true);
  };

  const applySettings = () => {
    setLength(draftLength);
    setUseUpper(draftUpper);
    setUseLower(draftLower);
    setUseDigits(draftDigits);
    setSpecials(draftSpecials);
    setCostFactor(draftCost);
    // #178: Übernommene Einstellungen in localStorage persistieren. specialsPicked in
    // kanonischer Reihenfolge (DEFAULT_SPECIALS) speichern — auch deaktivierte Auswahl bleibt
    // erhalten, sodass erneutes Aktivieren von Sonderzeichen die Auswahl wiederherstellt.
    savePasswordSettings({
      length: draftLength,
      useUpper: draftUpper,
      useLower: draftLower,
      useDigits: draftDigits,
      specialsActive: draftSpecials.active,
      specialsPicked: DEFAULT_SPECIALS.filter((c) => draftSpecials.picked.has(c)),
      costFactor: draftCost,
    });
    setDrawerOpen(false);
  };

  const toggleSpecial = (ch: string) => {
    setDraftSpecials((prev) => {
      const next = new Set(prev.picked);
      if (next.has(ch)) {
        next.delete(ch);
      } else {
        next.add(ch);
      }
      return { ...prev, picked: next };
    });
  };

  const entropy = entropyBits(
    length,
    alphabetSize(useUpper, useLower, useDigits, activeSpecialChars),
  );

  return (
    <>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="h4">Passwortgenerator</Typography>
        <Tooltip title="Einstellungen öffnen">
          <IconButton onClick={openSettings} aria-label="Einstellungen öffnen">
            <SettingsIcon />
          </IconButton>
        </Tooltip>
      </Stack>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Erzeugt im Browser ein zufälliges Passwort und einen passenden bcrypt-Hash. Nichts verlässt das Gerät.
      </Typography>

      {generateError && (
        <Alert severity="error" sx={{ mb: 2 }} role="alert">
          {generateError}
        </Alert>
      )}

      {noClassActive && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Keine Zeichenklasse aktiv. Öffne die Einstellungen und aktiviere mindestens eine.
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 3 }}>
        <Stack spacing={2}>
          <Button
            variant="contained"
            size="large"
            onClick={handleGenerate}
            disabled={noClassActive || isHashing}
          >
            {isHashing ? 'Hash wird berechnet …' : 'Passwort generieren'}
          </Button>
          <TextField
            label="Passwort"
            value={password}
            onChange={handlePasswordChange}
            placeholder="Generieren oder eigenes Passwort eintippen"
            helperText="Tippen aktualisiert den bcrypt-Hash automatisch nach kurzer Pause."
            slotProps={{
              input: {
                endAdornment: (
                  <InputAdornment position="end">
                    <Tooltip title="Passwort kopieren">
                      <IconButton
                        edge="end"
                        onClick={() => copyToClipboard(password, 'Passwort')}
                        aria-label="Passwort kopieren"
                      >
                        <ContentCopyIcon />
                      </IconButton>
                    </Tooltip>
                  </InputAdornment>
                ),
              },
            }}
            fullWidth
          />
          <TextField
            label="bcrypt-Hash"
            value={hash}
            slotProps={{
              input: {
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <Tooltip title="Hash kopieren">
                      <span style={{ display: 'inline-flex' }}>
                        <IconButton
                          edge="end"
                          onClick={() => copyToClipboard(hash, 'Hash')}
                          aria-label="Hash kopieren"
                          disabled={!hash}
                        >
                          <ContentCopyIcon />
                        </IconButton>
                      </span>
                    </Tooltip>
                  </InputAdornment>
                ),
              },
            }}
            fullWidth
          />
          <Typography variant="caption" color="text.secondary">
            Länge {length}, geschätzte Entropie ≈ {entropy.toFixed(0)} Bit
          </Typography>
        </Stack>
      </Paper>

      <Drawer
        anchor="right"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 360 } } }}
      >
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Einstellungen
          </Typography>
          <Stack spacing={3}>
            <Box>
              <Typography gutterBottom>Länge: {draftLength}</Typography>
              <Slider
                value={draftLength}
                onChange={(_, v) => setDraftLength(v as number)}
                min={MIN_LENGTH}
                max={MAX_LENGTH}
                step={1}
                marks={[
                  { value: MIN_LENGTH, label: MIN_LENGTH.toString() },
                  { value: MAX_LENGTH, label: MAX_LENGTH.toString() },
                ]}
                aria-label="Passwortlänge"
              />
            </Box>

            <Stack>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={draftUpper}
                    onChange={(e) => setDraftUpper(e.target.checked)}
                  />
                }
                label="Großbuchstaben (A bis Z)"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={draftLower}
                    onChange={(e) => setDraftLower(e.target.checked)}
                  />
                }
                label="Kleinbuchstaben (a bis z)"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={draftDigits}
                    onChange={(e) => setDraftDigits(e.target.checked)}
                  />
                }
                label="Ziffern (0 bis 9)"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={draftSpecials.active}
                    onChange={(e) =>
                      setDraftSpecials((prev) => ({ ...prev, active: e.target.checked }))
                    }
                  />
                }
                label="Sonderzeichen"
              />
            </Stack>

            {draftSpecials.active && (
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                  Einzeln an- oder abwählen — nützlich für Seiten mit Sonderzeichen-Restriktionen.
                </Typography>
                <Stack direction="row" spacing={0.5} useFlexGap flexWrap="wrap">
                  {DEFAULT_SPECIALS.map((ch) => {
                    const picked = draftSpecials.picked.has(ch);
                    return (
                      <Chip
                        key={ch}
                        label={ch}
                        onClick={() => toggleSpecial(ch)}
                        color={picked ? 'primary' : 'default'}
                        variant={picked ? 'filled' : 'outlined'}
                        size="small"
                        aria-pressed={picked}
                        aria-label={'Sonderzeichen ' + ch}
                      />
                    );
                  })}
                </Stack>
              </Box>
            )}

            <Box>
              <Typography gutterBottom>Bcrypt-Cost-Faktor: {draftCost}</Typography>
              <Slider
                value={draftCost}
                onChange={(_, v) => setDraftCost(v as number)}
                min={MIN_COST}
                max={MAX_COST}
                step={1}
                marks={[
                  { value: MIN_COST, label: MIN_COST.toString() },
                  { value: MAX_COST, label: MAX_COST.toString() },
                ]}
                aria-label="Bcrypt-Cost-Faktor"
              />
              <Typography variant="caption" color="text.secondary">
                Höhere Werte sind spürbar langsamer, aber besser gegen Brute-Force.
              </Typography>
            </Box>

            <Divider />
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button onClick={() => setDrawerOpen(false)}>Abbrechen</Button>
              <Button variant="contained" onClick={applySettings} disabled={draftNoClassActive}>
                Übernehmen
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Drawer>

      <Snackbar
        open={Boolean(snackbar)}
        autoHideDuration={2000}
        onClose={() => setSnackbar(null)}
        message={snackbar ?? ''}
      />
    </>
  );
}
