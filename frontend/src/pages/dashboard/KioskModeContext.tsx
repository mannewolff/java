import { createContext, useContext, useState } from 'react';
import type { ReactNode } from 'react';

interface KioskModeContextValue {
  kioskMode: boolean;
  setKioskMode: (active: boolean) => void;
}

const KioskModeContext = createContext<KioskModeContextValue | null>(null);

export function KioskModeProvider({ children }: { children: ReactNode }): JSX.Element {
  const [kioskMode, setKioskMode] = useState(false);
  return (
    <KioskModeContext.Provider value={{ kioskMode, setKioskMode }}>
      {children}
    </KioskModeContext.Provider>
  );
}

export function useKioskMode(): KioskModeContextValue {
  const ctx = useContext(KioskModeContext);
  if (!ctx) throw new Error('useKioskMode must be used within KioskModeProvider');
  return ctx;
}
