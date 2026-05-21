import { useRef, useState } from 'react';
import type { PointerEvent, SyntheticEvent } from 'react';
import { Box, Typography } from '@mui/material';

const MAX_DISPLAY_WIDTH = 720;

export interface CropOffsets {
  xOffset: number;
  yOffset: number;
}

export interface InteractiveCropFrameProps {
  imageUrl: string;
  targetWidth: number;
  targetHeight: number;
  xOffset: number;
  yOffset: number;
  onChange: (next: CropOffsets) => void;
}

interface DragState {
  startClientX: number;
  startClientY: number;
  startXOffset: number;
  startYOffset: number;
}

function clamp01(value: number): number {
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

export default function InteractiveCropFrame({
  imageUrl,
  targetWidth,
  targetHeight,
  xOffset,
  yOffset,
  onChange,
}: InteractiveCropFrameProps) {
  const [natural, setNatural] = useState<{ w: number; h: number } | null>(null);
  const [grabbing, setGrabbing] = useState(false);
  const dragRef = useRef<DragState | null>(null);

  const handleImageLoad = (event: SyntheticEvent<HTMLImageElement>) => {
    const img = event.currentTarget;
    if (img.naturalWidth > 0 && img.naturalHeight > 0) {
      setNatural({ w: img.naturalWidth, h: img.naturalHeight });
    }
  };

  if (!natural) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center' }}>
        <Box
          component="img"
          src={imageUrl}
          alt="Original-Bild lädt"
          onLoad={handleImageLoad}
          sx={{ maxWidth: MAX_DISPLAY_WIDTH, display: 'block' }}
        />
      </Box>
    );
  }

  const displayWidth = Math.min(MAX_DISPLAY_WIDTH, natural.w);
  const scale = displayWidth / natural.w;
  const displayHeight = natural.h * scale;
  const srcRatio = natural.w / natural.h;
  const targetRatio = targetWidth / targetHeight;
  const horizontalOverhang = srcRatio > targetRatio;

  const frameNaturalW = horizontalOverhang ? natural.h * targetRatio : natural.w;
  const frameNaturalH = horizontalOverhang ? natural.h : natural.w / targetRatio;
  const frameDisplayW = frameNaturalW * scale;
  const frameDisplayH = frameNaturalH * scale;

  const frameDisplayX = horizontalOverhang
    ? (displayWidth - frameDisplayW) * xOffset
    : 0;
  const frameDisplayY = horizontalOverhang
    ? 0
    : (displayHeight - frameDisplayH) * yOffset;

  const handlePointerDown = (e: PointerEvent<HTMLDivElement>) => {
    e.preventDefault();
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {
      /* jsdom and older browsers may not support pointer capture */
    }
    dragRef.current = {
      startClientX: e.clientX,
      startClientY: e.clientY,
      startXOffset: xOffset,
      startYOffset: yOffset,
    };
    setGrabbing(true);
  };

  const handlePointerMove = (e: PointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current;
    if (!drag) return;
    if (horizontalOverhang) {
      const room = displayWidth - frameDisplayW;
      if (room <= 0) return;
      const delta = (e.clientX - drag.startClientX) / room;
      onChange({ xOffset: clamp01(drag.startXOffset + delta), yOffset });
    } else {
      const room = displayHeight - frameDisplayH;
      if (room <= 0) return;
      const delta = (e.clientY - drag.startClientY) / room;
      onChange({ xOffset, yOffset: clamp01(drag.startYOffset + delta) });
    }
  };

  const handlePointerUp = (e: PointerEvent<HTMLDivElement>) => {
    try {
      if (e.currentTarget.hasPointerCapture(e.pointerId)) {
        e.currentTarget.releasePointerCapture(e.pointerId);
      }
    } catch {
      /* see handlePointerDown */
    }
    dragRef.current = null;
    setGrabbing(false);
  };

  const activeOffset = horizontalOverhang ? xOffset : yOffset;

  return (
    <Box>
      <Box
        sx={{
          position: 'relative',
          width: displayWidth,
          height: displayHeight,
          mx: 'auto',
          overflow: 'hidden',
          userSelect: 'none',
        }}
      >
        <Box
          component="img"
          src={imageUrl}
          alt="Original"
          draggable={false}
          sx={{ width: displayWidth, height: displayHeight, display: 'block' }}
        />
        <Box
          role="slider"
          aria-label="Crop-Rahmen verschieben"
          aria-valuemin={0}
          aria-valuemax={1}
          aria-valuenow={Number(activeOffset.toFixed(2))}
          aria-orientation={horizontalOverhang ? 'horizontal' : 'vertical'}
          tabIndex={0}
          data-testid="crop-frame"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          sx={{
            position: 'absolute',
            left: frameDisplayX,
            top: frameDisplayY,
            width: frameDisplayW,
            height: frameDisplayH,
            border: '2px solid white',
            boxShadow: '0 0 0 9999px rgba(0,0,0,0.45)',
            cursor: grabbing ? 'grabbing' : 'grab',
            touchAction: 'none',
            boxSizing: 'border-box',
          }}
        />
      </Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', textAlign: 'center', mt: 1 }}>
        {horizontalOverhang ? 'Horizontal' : 'Vertikal'} verschieben:{' '}
        <strong>{Math.round(activeOffset * 100)}%</strong>
      </Typography>
    </Box>
  );
}
