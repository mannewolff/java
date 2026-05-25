import { useState } from 'react';
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
  // Applied state — used by the Generate button
  const [length, setLength] = useState(DEFAULT_LENGTH);
  const [useUpper, setUseUpper] = useState(true);
  const [useLower, setUseLower] = useState(true);
  const [useDigits, setUseDigits] = useState(true);
  const [specials, setSpecials] = useState<SpecialsSelection>({
    active: true,
    picked: DEFAULT_SPECIALS_SET,
  });
  const [costFactor, setCostFactor] = useState(DEFAULT_COST);

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

  const handleGenerate = async () => {
    setGenerateError(null);
    try {
      const pw = generate({
        length,
        useUpper,
        useLower,
        useDigits,
        specials: activeSpecialChars,
      });
      setPassword(pw);
      setIsHashing(true);
      const h = await hashBcrypt(pw, costFactor);
      setHash(h);
    } catch (err) {
      setGenerateError(err instanceof Error ? err.message : 'Unbekannter Fehler');
    } finally {
      setIsHashing(false);
    }
  };

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
            slotProps={{
              input: {
                readOnly: true,
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
                      <IconButton
                        edge="end"
                        onClick={() => copyToClipboard(hash, 'Hash')}
                        aria-label="Hash kopieren"
                        disabled={!hash}
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
