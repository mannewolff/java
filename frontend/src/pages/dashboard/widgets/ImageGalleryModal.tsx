import { Button, Dialog, DialogActions, DialogContent, DialogTitle } from '@mui/material';

import ImageGallery from '../../../components/ImageGallery';

export interface ImageGalleryModalProps {
  open: boolean;
  onClose: () => void;
  /** Wird mit der gewählten Bild-ID aufgerufen; das Modal schließt danach. */
  onSelect: (id: number) => void;
}

/**
 * Modal mit der wiederverwendbaren {@link ImageGallery} zur Auswahl eines bereits gespeicherten
 * Bildes für das Bild-Widget (#199). Auswahl schließt das Modal.
 */
export default function ImageGalleryModal({
  open,
  onClose,
  onSelect,
}: ImageGalleryModalProps): JSX.Element {
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm" aria-labelledby="gallery-title">
      <DialogTitle id="gallery-title">Bild aus der Datenbank wählen</DialogTitle>
      <DialogContent dividers>
        <ImageGallery
          onSelect={(id) => {
            onSelect(id);
            onClose();
          }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Abbrechen</Button>
      </DialogActions>
    </Dialog>
  );
}
