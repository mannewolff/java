import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, DragEvent } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadIcon from '@mui/icons-material/Download';
import ImageIcon from '@mui/icons-material/Image';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import TransformIcon from '@mui/icons-material/Transform';

import { ApiError } from '../../api/client';
import { convertRasterToPng } from '../../api/rasterToPng';
import { useNotify } from '../../notify/NotifyProvider';

const ACCEPTED_TYPES = ['image/png', 'image/jpeg'];
const MAX_BYTES = 10 * 1024 * 1024;

function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Unbekannter Fehler';
}

function buildOutputFilename(input: File): string {
  const lastDot = input.name.lastIndexOf('.');
  const base = lastDot > 0 ? input.name.slice(0, lastDot) : input.name;
  return `${base || 'image'}.png`;
}

export default function RasterToPngPage(): JSX.Element {
  const [file, setFile] = useState<File | null>(null);
  const [sourceUrl, setSourceUrl] = useState<string | null>(null);
  const [resultUrl, setResultUrl] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const notify = useNotify();
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    return () => {
      if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    };
  }, [sourceUrl]);

  useEffect(() => {
    return () => {
      if (resultUrl) URL.revokeObjectURL(resultUrl);
    };
  }, [resultUrl]);

  const acceptFile = (incoming: File): void => {
    if (!ACCEPTED_TYPES.includes(incoming.type)) {
      notify.error(
        `Format nicht unterstützt: ${incoming.type || 'unbekannt'} (erwartet: PNG oder JPEG)`,
      );
      return;
    }
    if (incoming.size > MAX_BYTES) {
      notify.error(`Datei zu groß (${(incoming.size / 1024 / 1024).toFixed(1)} MB, max 10 MB)`);
      return;
    }
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    if (resultUrl) URL.revokeObjectURL(resultUrl);
    setFile(incoming);
    setResultUrl(null);
    setSourceUrl(URL.createObjectURL(incoming));
  };

  const reset = (): void => {
    if (sourceUrl) URL.revokeObjectURL(sourceUrl);
    if (resultUrl) URL.revokeObjectURL(resultUrl);
    setFile(null);
    setSourceUrl(null);
    setResultUrl(null);
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

  const handleSubmit = async (): Promise<void> => {
    if (!file) return;
    setIsProcessing(true);
    try {
      const { blob } = await convertRasterToPng(file);
      if (resultUrl) URL.revokeObjectURL(resultUrl);
      setResultUrl(URL.createObjectURL(blob));
    } catch (err) {
      notify.error(errorMessage(err));
    } finally {
      setIsProcessing(false);
    }
  };

  const downloadFilename = file ? buildOutputFilename(file) : 'image.png';

  return (
    <>
      <Stack direction="row" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="h4">Raster zu PNG</Typography>
      </Stack>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        JPEG oder PNG hochladen und 1:1 als PNG exportieren.
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
        <ImageIcon
          sx={{ fontSize: 32, color: 'text.secondary', mr: 1, verticalAlign: 'middle' }}
        />
        <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary' }} />
        <Typography>JPEG oder PNG hier ablegen oder klicken zum Auswählen</Typography>
        <Typography variant="caption" color="text.secondary">
          image/png, image/jpeg, max 10 MB
        </Typography>
        <input
          ref={inputRef}
          type="file"
          accept="image/png,image/jpeg,.png,.jpg,.jpeg"
          onChange={handleInputChange}
          hidden
          aria-label="Bild auswählen"
        />
      </Paper>

      {file && (
        <>
          <Paper sx={{ p: 2, mb: 3 }}>
            <Typography gutterBottom>
              Quelle: <strong>{file.name}</strong>{' '}
              <Typography component="span" variant="caption" color="text.secondary">
                ({(file.size / 1024).toFixed(1)} KB)
              </Typography>
            </Typography>
            {sourceUrl && (
              <Box
                component="img"
                src={sourceUrl}
                alt="Quelldatei"
                sx={{
                  width: '100%',
                  maxWidth: 800,
                  display: 'block',
                  mx: 'auto',
                  mb: 2,
                  borderRadius: 1,
                  border: 1,
                  borderColor: 'divider',
                }}
              />
            )}
            <Stack direction="row" spacing={2}>
              <Button
                variant="contained"
                onClick={handleSubmit}
                disabled={isProcessing}
                startIcon={
                  isProcessing ? <CircularProgress size={16} color="inherit" /> : <TransformIcon />
                }
              >
                Konvertieren
              </Button>
              <Button
                variant="outlined"
                startIcon={<RestartAltIcon />}
                onClick={reset}
                disabled={isProcessing}
              >
                Zurücksetzen
              </Button>
              {resultUrl && (
                <Button
                  variant="outlined"
                  startIcon={<DownloadIcon />}
                  component="a"
                  href={resultUrl}
                  download={downloadFilename}
                >
                  Herunterladen
                </Button>
              )}
            </Stack>
          </Paper>

          {resultUrl && (
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                Ergebnis
              </Typography>
              <Box
                component="img"
                src={resultUrl}
                alt="Konvertiertes PNG"
                sx={{
                  width: '100%',
                  maxWidth: 800,
                  display: 'block',
                  mx: 'auto',
                  borderRadius: 1,
                  border: 1,
                  borderColor: 'divider',
                  imageRendering: 'auto',
                }}
              />
            </Paper>
          )}
        </>
      )}
    </>
  );
}
