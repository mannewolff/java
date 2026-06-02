import type { ComponentType } from 'react';
import type { SvgIconProps } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import DashboardCustomizeIcon from '@mui/icons-material/DashboardCustomize';
import SettingsIcon from '@mui/icons-material/Settings';
import TransformIcon from '@mui/icons-material/Transform';
import ColorizeIcon from '@mui/icons-material/Colorize';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import ImageIcon from '@mui/icons-material/Image';
import BuildIcon from '@mui/icons-material/Build';
import ApiIcon from '@mui/icons-material/Api';
import ViewKanbanIcon from '@mui/icons-material/ViewKanban';
import ShowChartIcon from '@mui/icons-material/ShowChart';
import PhoneIphoneIcon from '@mui/icons-material/PhoneIphone';
import PhotoLibraryIcon from '@mui/icons-material/PhotoLibrary';

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
  // "Home" öffnet das Default-Dashboard direkt — der typische Einstieg nach dem Login.
  // "Dashboards" (Plural) liegt am Ende, oberhalb der Einstellungen, und zeigt die Liste
  // zum Verwalten/Anlegen/Löschen.
  { kind: 'link', label: 'Home', path: '/dashboards/default', icon: HomeIcon },
  { kind: 'link', label: 'Zeitreihen', path: '/timeseries', icon: ShowChartIcon },
  { kind: 'link', label: 'Kanban', path: '/kanban', icon: ViewKanbanIcon },
  { kind: 'link', label: 'Mobile', path: '/mobile', icon: PhoneIphoneIcon },
  {
    kind: 'group',
    label: 'Bildverarbeitung',
    icon: ImageIcon,
    children: [
      // Die übrigen Bildtools (Hintergrund entfernen, Beitragsbild, Bild verkleinern)
      // sind aus dem Menü in die Einstellungen umgezogen (#131) — Routen bleiben aktiv.
      { kind: 'link', label: 'SVG zu PNG', path: '/tools/svg-to-png', icon: TransformIcon },
      { kind: 'link', label: 'Farbpipette', path: '/tools/color-picker', icon: ColorizeIcon },
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
      { kind: 'link', label: 'Bilder verwalten', path: '/tools/images', icon: PhotoLibraryIcon },
      { kind: 'link', label: 'API-Konsole', path: '/tools/api-console', icon: ApiIcon },
    ],
  },
  { kind: 'link', label: 'Dashboards', path: '/dashboards', icon: DashboardCustomizeIcon },
  { kind: 'link', label: 'Einstellungen', path: '/settings', icon: SettingsIcon },
];
