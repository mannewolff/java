import { createContext, useContext, useState, type ReactNode } from 'react';

import type { WidgetType } from '../../api/dashboard';

/**
 * Edit-Modus eines Dashboards. Wird in App.tsx oberhalb von AppShell provided,
 * sodass sowohl die Sidebar (AppShell) als auch die DashboardPage (Outlet-Child)
 * denselben State sehen. Im Read-Modus zeigt die Sidebar die normale Navigation,
 * im Edit-Modus die Widget-Palette.
 *
 * Zusätzlich wird der aktuell gedraggte Widget-Typ hier gehalten, weil die
 * Drag-Source (WidgetPalette in der Sidebar) und das Drop-Ziel (Grid in
 * DashboardPage) in unterschiedlichen Outlet-Geschwistern leben.
 *
 * Bewusst keine URL-Persistenz: ein Reload stellt den sicheren Read-Modus
 * wieder her.
 */
interface EditModeValue {
  editMode: boolean;
  setEditMode: (v: boolean) => void;
  /** Aktuell gedraggter Widget-Typ aus der Palette; `null` wenn kein Drag aktiv. */
  draggingType: WidgetType | null;
  setDraggingType: (t: WidgetType | null) => void;
}

const EditModeContext = createContext<EditModeValue | null>(null);

export function EditModeProvider({ children }: { children: ReactNode }): JSX.Element {
  const [editMode, setEditMode] = useState(false);
  const [draggingType, setDraggingType] = useState<WidgetType | null>(null);
  return (
    <EditModeContext.Provider value={{ editMode, setEditMode, draggingType, setDraggingType }}>
      {children}
    </EditModeContext.Provider>
  );
}

export function useEditMode(): EditModeValue {
  const ctx = useContext(EditModeContext);
  if (!ctx) {
    throw new Error('useEditMode must be used inside EditModeProvider');
  }
  return ctx;
}
