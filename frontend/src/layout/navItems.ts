import type { ComponentType } from 'react';
import type { SvgIconProps } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import DashboardCustomizeIcon from '@mui/icons-material/DashboardCustomize';
import SettingsIcon from '@mui/icons-material/Settings';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import AspectRatioIcon from '@mui/icons-material/AspectRatio';
import CompressIcon from '@mui/icons-material/Compress';
import TransformIcon from '@mui/icons-material/Transform';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import ImageIcon from '@mui/icons-material/Image';
import BuildIcon from '@mui/icons-material/Build';
import ViewKanbanIcon from '@mui/icons-material/ViewKanban';

type NavIcon = ComponentType<SvgIconProps>;

export interface NavLink {
  kind: 'link';
  label: string;
  path: string;
  icon: NavIcon;
}

export interface NavGroup {
  kind: 'group';
  label: string;
  icon: NavIcon;
  children: NavLink[];
}

export type NavNode = NavLink | NavGroup;

export const navItems: NavNode[] = [
  // "Dashboard" (Singular) öffnet das Default-Dashboard direkt — der typische Einstieg
  // nach dem Login. "Dashboards" (Plural) liegt am Ende, oberhalb der Einstellungen,
  // und zeigt die Liste zum Verwalten/Anlegen/Löschen.
  { kind: 'link', label: 'Dashboard', path: '/dashboards/default', icon: DashboardIcon },
  { kind: 'link', label: 'Kanban', path: '/kanban', icon: ViewKanbanIcon },
  {
    kind: 'group',
    label: 'Bildverarbeitung',
    icon: ImageIcon,
    children: [
      { kind: 'link', label: 'Hintergrund entfernen', path: '/tools/remove-background', icon: AutoFixHighIcon },
      { kind: 'link', label: 'Beitragsbild', path: '/tools/og-image', icon: AspectRatioIcon },
      { kind: 'link', label: 'Bild verkleinern', path: '/tools/resize', icon: CompressIcon },
      { kind: 'link', label: 'SVG zu PNG', path: '/tools/svg-to-png', icon: TransformIcon },
    ],
  },
  {
    kind: 'group',
    label: 'Nützliche Tools',
    icon: BuildIcon,
    children: [
      // Die Seite hinter /tools/password wird in Issue #34 implementiert.
      // Der Menüpunkt wird hier bereits angelegt, damit die Reihenfolge
      // #33 -> #34 sauber funktioniert.
      { kind: 'link', label: 'Passwortgenerator', path: '/tools/password', icon: VpnKeyIcon },
    ],
  },
  { kind: 'link', label: 'Dashboards', path: '/dashboards', icon: DashboardCustomizeIcon },
  { kind: 'link', label: 'Einstellungen', path: '/settings', icon: SettingsIcon },
];
