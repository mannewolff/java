import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, DragEvent, MouseEvent, SyntheticEvent } from 'react';
import {
  Box,
  IconButton,
  Paper,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import ColorizeIcon from '@mui/icons-material/Colorize';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckIcon from '@mui/icons-material/Check';

import { useNotify } from '../../notify/NotifyProvider';

const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];
const MAX_BYTES = 10 * 1024 * 1024;

type Field = 'hex' | 'rgb';

function toHex(r: number, g: number, b: number): string {
  const channel = (v: number): string => v.toString(16).padStart(2, '0');
  return `#${channel(r)}${channel(g)}${channel(b)}`;
}

export default function ColorPickerPage(): JSX.Element {
  const [sourceUrl, setSourceUrl] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [hex, setHex] = useState<string | null>(null);
  const [rgb, setRgb] = useState<string | null>(null);
  const [copied, setCopied] = useState<Field | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const notify = useNotify();

  useEffect(() => {
    return () => {
      if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    };
  }, [sourceUrl]);

  const acceptFile = (incoming: File): void => {
    if (!ACCEPTED_TYPES.includes(incoming.type)) {
      notify.error(`Format nicht unterstützt: ${incoming.type || 'unbekannt'} (erwartet: PNG, JPEG, WebP, GIF)`);
      return;
    }
    if (incoming.size > MAX_BYTES) {
      notify.error(`Datei zu groß (${(incoming.size / 1024 / 1024).toFixed(1)} MB, max 10 MB)`);
      return;
    }
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    setHex(null);
    setRgb(null);
    setCopied(null);
    setSourceUrl(URL.createObjectURL(incoming));
  };

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const selected = event.target.files?.[0];
    if (selected) acceptFile(selected);
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>): void => {
    event.preventDefault();
    setIsDragging(false);
    const dropped = event.dataTransfer.files?.[0];
    if (dropped) acceptFile(dropped);
  };

  const handleImageLoad = (event: SyntheticEvent<HTMLImageElement>): void => {
    const img = event.currentTarget;
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.width = img.naturalWidth;
    canvas.height = img.naturalHeight;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    ctx?.drawImage(img, 0, 0);
  };

  const handlePick = (event: MouseEvent<HTMLImageElement>): void => {
    const img = event.currentTarget;
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d', { willReadFrequently: true });
    if (!canvas || !ctx) return;
    const rect = img.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) return;
    const px = Math.floor((event.clientX - rect.left) * (canvas.width / rect.width));
    const py = Math.floor((event.clientY - rect.top) * (canvas.height / rect.height));
    const x = Math.min(Math.max(px, 0), Math.max(canvas.width - 1, 0));
    const y = Math.min(Math.max(py, 0), Math.max(canvas.height - 1, 0));
    const [r, g, b] = ctx.getImageData(x, y, 1, 1).data;
    setHex(toHex(r, g, b));
    setRgb(`rgb(${r}, ${g}, ${b})`);
    setCopied(null);
  };

  const copyValue = async (value: string, field: Field): Promise<void> => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(field);
      notify.success('Farbwert kopiert');
      window.setTimeout(() => setCopied(null), 1500);
    } catch {
      notify.error('Kopieren fehlgeschlagen');
    }
  };

  const renderColorField = (label: string, value: string | null, field: Field): JSX.Element => (
    <Stack direction="row" spacing={1} alignItems="center">
      <TextField
        label={label}
        value={value ?? ''}
        InputProps={{ readOnly: true }}
        placeholder="—"
        inputProps={{ 'aria-label': label }}
        sx={{ flex: 1, fontFamily: 'monospace' }}
      />
      <Tooltip title={copied === field ? 'Kopiert' : `${label} kopieren`}>
        <span>
          <IconButton
            aria-label={`${label} kopieren`}
            disabled={!value}
            onClick={() => value && void copyValue(value, field)}
            color={copied === field ? 'success' : 'default'}
          >
            {copied === field ? <CheckIcon /> : <ContentCopyIcon />}
          </IconButton>
        </span>
      </Tooltip>
    </Stack>
  );

  return (
    <>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
        <ColorizeIcon />
        <Typography variant="h4">Farbpipette</Typography>
      </Stack>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Bild hochladen, dann ein Pixel anklicken. Der Farbwert wird als HEX und RGB angezeigt und
        lässt sich kopieren.
      </Typography>

      <Paper
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        sx={{
          p: 4,
          mb: 3,
          textAlign: 'center',
          cursor: 'pointer',
          borderStyle: 'dashed',
          borderWidth: 2,
          borderColor: isDragging ? 'primary.main' : 'divider',
          bgcolor: isDragging ? 'action.hover' : 'background.paper',
        }}
        data-testid="drop-zone"
      >
        <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
        <Typography>Bild hier ablegen oder klicken zum Auswählen</Typography>
        <Typography variant="caption" color="text.secondary">
          PNG, JPEG, WebP, GIF — max 10 MB
        </Typography>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES.join(',')}
          onChange={handleInputChange}
          hidden
          aria-label="Bild auswählen"
        />
      </Paper>

      {sourceUrl && (
        <>
          <Paper sx={{ p: 2, mb: 3 }}>
            <Box
              component="img"
              src={sourceUrl}
              alt="Hochgeladenes Bild"
              aria-label="Bild — Pixel anklicken, um die Farbe auszulesen"
              onLoad={handleImageLoad}
              onClick={handlePick}
              sx={{
                maxWidth: '100%',
                display: 'block',
                mx: 'auto',
                cursor: 'crosshair',
                borderRadius: 1,
                border: 1,
                borderColor: 'divider',
              }}
            />
            <canvas ref={canvasRef} aria-hidden="true" style={{ display: 'none' }} />
          </Paper>

          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Farbwert
            </Typography>
            {!hex && (
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Klicke ein Pixel im Bild an, um den Farbwert auszulesen.
              </Typography>
            )}
            <Stack direction="row" spacing={2} alignItems="center">
              <Box
                aria-label="Farbvorschau"
                sx={{
                  width: 48,
                  height: 48,
                  flexShrink: 0,
                  borderRadius: 1,
                  border: 1,
                  borderColor: 'divider',
                  bgcolor: hex ?? 'transparent',
                }}
              />
              <Stack spacing={2} sx={{ flex: 1, maxWidth: 360 }}>
                {renderColorField('HEX', hex, 'hex')}
                {renderColorField('RGB', rgb, 'rgb')}
              </Stack>
            </Stack>
          </Paper>
        </>
      )}
    </>
  );
}
