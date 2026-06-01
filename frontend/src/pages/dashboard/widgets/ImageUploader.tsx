import { useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material';
import UploadIcon from '@mui/icons-material/Upload';

import {
  ACCEPTED_IMAGE_TYPES,
  MAX_IMAGE_BYTES,
  uploadImage,
  type UploadedImageInfo,
} from '../../../api/images';

interface Props {
  /** Label des Buttons — z. B. "Bild hochladen" oder "Bild ersetzen". */
  label: string;
  onUploaded: (info: UploadedImageInfo) => void;
}

/**
 * Upload-Komponente für das Bild-Widget (#184): Datei wählen, Format-/Größen-Validierung,
 * Upload an den Image-Store, Progress + lokale Thumbnail-Vorschau, Fehleranzeige.
 */
export default function ImageUploader({ label, onUploaded }: Props): JSX.Element {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  async function handleFile(event: ChangeEvent<HTMLInputElement>): Promise<void> {
    const file = event.target.files?.[0];
    // Input zurücksetzen, damit dieselbe Datei erneut auswählbar bleibt.
    event.target.value = '';
    if (!file) return;
    setError(null);

    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      setError(`Format nicht unterstützt: ${file.type || 'unbekannt'}`);
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setError(`Datei zu groß (${(file.size / 1024 / 1024).toFixed(1)} MB, max 5 MB)`);
      return;
    }

    setBusy(true);
    try {
      const info = await uploadImage(file);
      // Lokale Vorschau aus der Datei (kein erneuter authentifizierter Fetch nötig).
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      setPreviewUrl(URL.createObjectURL(file));
      onUploaded(info);
    } catch {
      setError('Upload fehlgeschlagen. Bitte erneut versuchen.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Stack spacing={1}>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_IMAGE_TYPES.join(',')}
        onChange={handleFile}
        style={{ display: 'none' }}
        aria-label="Bilddatei auswählen"
        data-testid="image-file-input"
      />
      <Button
        variant="outlined"
        startIcon={busy ? <CircularProgress size={16} /> : <UploadIcon />}
        onClick={() => inputRef.current?.click()}
        disabled={busy}
        onMouseDown={(e) => e.stopPropagation()}
      >
        {busy ? 'Wird hochgeladen …' : label}
      </Button>
      <Typography variant="caption" color="text.secondary">
        JPG, PNG, WebP oder GIF · max 5 MB
      </Typography>
      {error && (
        <Alert severity="error" role="alert">
          {error}
        </Alert>
      )}
      {previewUrl && (
        <Box
          component="img"
          src={previewUrl}
          alt="Vorschau des hochgeladenen Bildes"
          sx={{ maxWidth: '100%', maxHeight: 120, objectFit: 'contain', borderRadius: 1 }}
        />
      )}
    </Stack>
  );
}
