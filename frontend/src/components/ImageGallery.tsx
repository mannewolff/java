import { useEffect, useState } from 'react';
import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material';

import { fetchThumbnailObjectUrl, listImages, type ImageMetadata } from '../api/images';
import { ApiError } from '../api/client';

const THUMB_SIZE = 80;
const PAGE_SIZE = 24;

export interface ImageGalleryProps {
  /** Wird mit der gewählten Bild-ID aufgerufen. */
  onSelect: (id: number) => void;
  /** Aktuell ausgewählte Bild-ID (für Hervorhebung). */
  selectedId?: number | null;
}

/**
 * Wiederverwendbare Galerie gespeicherter Bilder als 80×80-Thumbnails (#198). Die Thumbnails werden
 * über den server-seitig skalierten Thumbnail-Endpoint geladen (#200, bearer-only, als Object-URL).
 * Genutzt von ResizePage (#198) und dem WidgetImage-Galerie-Modal (#199).
 */
export default function ImageGallery({ onSelect, selectedId }: ImageGalleryProps): JSX.Element {
  const [items, setItems] = useState<ImageMetadata[]>([]);
  const [total, setTotal] = useState(0);
  const [offset, setOffset] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    listImages(PAGE_SIZE, offset)
      .then((res) => {
        if (cancelled) return;
        setItems(res.images);
        setTotal(res.total);
      })
      .catch((e) => {
        if (cancelled) return;
        setError(e instanceof ApiError ? e.message : 'Galerie konnte nicht geladen werden');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [offset]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress aria-label="Galerie lädt" />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  if (items.length === 0) {
    return (
      <Typography color="text.secondary" sx={{ p: 2, textAlign: 'center' }}>
        Noch keine Bilder gespeichert.
      </Typography>
    );
  }

  const hasPrev = offset > 0;
  const hasNext = offset + items.length < total;

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 1,
          justifyContent: 'flex-start',
        }}
      >
        {items.map((img) => (
          <GalleryThumb
            key={img.id}
            image={img}
            selected={img.id === selectedId}
            onSelect={onSelect}
          />
        ))}
      </Box>
      {(hasPrev || hasNext) && (
        <Stack direction="row" spacing={1} justifyContent="center" sx={{ mt: 2 }}>
          <Button
            size="small"
            disabled={!hasPrev}
            onClick={() => setOffset((o) => Math.max(0, o - PAGE_SIZE))}
          >
            Zurück
          </Button>
          <Button size="small" disabled={!hasNext} onClick={() => setOffset((o) => o + PAGE_SIZE)}>
            Weiter
          </Button>
        </Stack>
      )}
    </Box>
  );
}

interface GalleryThumbProps {
  image: ImageMetadata;
  selected: boolean;
  onSelect: (id: number) => void;
}

function GalleryThumb({ image, selected, onSelect }: GalleryThumbProps): JSX.Element {
  const [url, setUrl] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    let created: string | null = null;
    fetchThumbnailObjectUrl(image.id)
      .then((u) => {
        if (active) {
          created = u;
          setUrl(u);
        } else {
          URL.revokeObjectURL(u);
        }
      })
      .catch(() => {
        /* fehlerhafte Einzelbilder werden still ausgelassen */
      });
    return () => {
      active = false;
      if (created) URL.revokeObjectURL(created);
    };
  }, [image.id]);

  return (
    <Box
      component="button"
      type="button"
      onClick={() => onSelect(image.id)}
      aria-label={`Bild ${image.id} auswählen`}
      sx={{
        width: THUMB_SIZE,
        height: THUMB_SIZE,
        p: 0,
        cursor: 'pointer',
        borderRadius: 1,
        overflow: 'hidden',
        border: 2,
        borderColor: selected ? 'primary.main' : 'divider',
        bgcolor: 'action.hover',
        transition: 'transform 0.1s, border-color 0.1s',
        '&:hover': { transform: 'scale(1.05)', borderColor: 'primary.light' },
      }}
    >
      {url && (
        <Box
          component="img"
          src={url}
          alt=""
          sx={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
        />
      )}
    </Box>
  );
}
