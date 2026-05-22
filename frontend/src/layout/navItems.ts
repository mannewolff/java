import type { ComponentType } from 'react';
import type { SvgIconProps } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import SettingsIcon from '@mui/icons-material/Settings';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import AspectRatioIcon from '@mui/icons-material/AspectRatio';
import CompressIcon from '@mui/icons-material/Compress';

export interface NavItem {
  label: string;
  path: string;
  icon: ComponentType<SvgIconProps>;
}

export const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: DashboardIcon },
  { label: 'Hintergrund entfernen', path: '/tools/remove-background', icon: AutoFixHighIcon },
  { label: 'Beitragsbild', path: '/tools/og-image', icon: AspectRatioIcon },
  { label: 'Bild verkleinern', path: '/tools/resize', icon: CompressIcon },
  { label: 'Einstellungen', path: '/settings', icon: SettingsIcon },
];
