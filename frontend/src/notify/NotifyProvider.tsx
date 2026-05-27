import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
} from 'react';
import type { FC, ReactNode, SyntheticEvent } from 'react';
import { Alert, Snackbar } from '@mui/material';

export type Severity = 'success' | 'error' | 'info' | 'warning';

export interface NotifyApi {
  success(message: string): void;
  error(message: string): void;
  info(message: string): void;
  warning(message: string): void;
  show(message: string, severity: Severity): void;
}

interface ToastItem {
  key: number;
  message: string;
  severity: Severity;
}

const AUTO_HIDE_MS: Record<Severity, number> = {
  success: 3000,
  info: 3000,
  error: 6000,
  warning: 6000,
};

const NotifyContext = createContext<NotifyApi | null>(null);

export const NotifyProvider: FC<{ children: ReactNode }> = ({ children }) => {
  const [current, setCurrent] = useState<ToastItem | null>(null);
  const [open, setOpen] = useState(false);
  const queueRef = useRef<ToastItem[]>([]);
  const counterRef = useRef(0);
  // currentRef spiegelt `current` synchron — bei mehreren `show()`-Calls im
  // selben Tick würde die useCallback-Closure sonst noch das alte (null) sehen
  // und beide Toasts würden sich überschreiben statt zu queuen.
  const currentRef = useRef<ToastItem | null>(null);

  const pumpNext = useCallback(() => {
    const next = queueRef.current.shift();
    if (next) {
      currentRef.current = next;
      setCurrent(next);
      setOpen(true);
    } else {
      currentRef.current = null;
      setCurrent(null);
    }
  }, []);

  const show = useCallback((message: string, severity: Severity) => {
    counterRef.current += 1;
    const item: ToastItem = { key: counterRef.current, message, severity };
    if (currentRef.current == null) {
      currentRef.current = item;
      setCurrent(item);
      setOpen(true);
    } else {
      // Silently queue — der nächste wird gepumpt, sobald der aktuelle
      // ausgeblendet ist (Auto-Hide oder Close-Button → onExited → pumpNext).
      queueRef.current.push(item);
    }
  }, []);

  const api = useMemo<NotifyApi>(
    () => ({
      show,
      success: (m) => show(m, 'success'),
      error: (m) => show(m, 'error'),
      info: (m) => show(m, 'info'),
      warning: (m) => show(m, 'warning'),
    }),
    [show],
  );

  const handleClose = (_event: Event | SyntheticEvent, reason?: string) => {
    if (reason === 'clickaway') return;
    setOpen(false);
  };

  const handleAlertClose = () => {
    setOpen(false);
  };

  return (
    <NotifyContext.Provider value={api}>
      {children}
      <Snackbar
        key={current?.key ?? 'empty'}
        open={open}
        autoHideDuration={current ? AUTO_HIDE_MS[current.severity] : null}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
        TransitionProps={{ onExited: pumpNext }}
      >
        {current ? (
          <Alert
            severity={current.severity}
            variant="filled"
            onClose={handleAlertClose}
            sx={{ width: '100%' }}
          >
            {current.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </NotifyContext.Provider>
  );
};

export function useNotify(): NotifyApi {
  const ctx = useContext(NotifyContext);
  if (!ctx) {
    throw new Error('useNotify must be used inside <NotifyProvider>');
  }
  return ctx;
}
