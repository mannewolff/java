import { useMemo, useState } from 'react';
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Collapse,
  CssBaseline,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { navItems } from './navItems';
import type { NavGroup, NavLink, NavNode } from './navItems';
import { useAuth } from '../auth/useAuth';

const DRAWER_WIDTH = 240;

function isGroup(node: NavNode): node is NavGroup {
  return node.kind === 'group';
}

function groupContainsPath(group: NavGroup, pathname: string): boolean {
  return group.children.some((child) => pathname.startsWith(child.path));
}

export default function AppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const { username, initial, signOut } = useAuth();

  // Default-open: jede Gruppe, in der die aktuelle Route liegt.
  const initiallyOpenGroups = useMemo(() => {
    const open = new Set<string>();
    for (const node of navItems) {
      if (isGroup(node) && groupContainsPath(node, location.pathname)) {
        open.add(node.label);
      }
    }
    return open;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const [openGroups, setOpenGroups] = useState<Set<string>>(initiallyOpenGroups);

  const toggleGroup = (label: string) => {
    setOpenGroups((prev) => {
      const next = new Set(prev);
      if (next.has(label)) {
        next.delete(label);
      } else {
        next.add(label);
      }
      return next;
    });
  };

  const renderLink = (link: NavLink, indented: boolean) => {
    const Icon = link.icon;
    const selected = location.pathname.startsWith(link.path);
    return (
      <ListItem key={link.path} disablePadding>
        <ListItemButton
          selected={selected}
          onClick={() => navigate(link.path)}
          sx={indented ? { pl: 4 } : undefined}
          aria-selected={selected}
        >
          <ListItemIcon>
            <Icon color={selected ? 'primary' : 'inherit'} />
          </ListItemIcon>
          <ListItemText primary={link.label} />
        </ListItemButton>
      </ListItem>
    );
  };

  const renderGroup = (group: NavGroup) => {
    const GroupIcon = group.icon;
    const expanded = openGroups.has(group.label);
    const hasActiveChild = groupContainsPath(group, location.pathname);
    return (
      <Box key={group.label}>
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => toggleGroup(group.label)}
            aria-expanded={expanded}
          >
            <ListItemIcon>
              <GroupIcon color={hasActiveChild ? 'primary' : 'inherit'} />
            </ListItemIcon>
            <ListItemText
              primary={group.label}
              primaryTypographyProps={
                hasActiveChild ? { color: 'primary', fontWeight: 600 } : undefined
              }
            />
            {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          </ListItemButton>
        </ListItem>
        <Collapse in={expanded} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {group.children.map((child) => renderLink(child, true))}
          </List>
        </Collapse>
      </Box>
    );
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <CssBaseline />
      <AppBar
        position="fixed"
        sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}
      >
        <Toolbar>
          <Box
            component="img"
            src="/logo.png"
            alt=""
            sx={{
              height: 32,
              mr: 1.5,
              // Falls die Logo-Datei (noch) nicht im public-Ordner liegt,
              // wird das Bild als Broken-Image gerendert — aber das stoert nur
              // optisch; alt="" haelt Screenreader still.
            }}
          />
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            mannewolff-tools
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Avatar
              sx={{
                bgcolor: 'primary.dark',
                width: 32,
                height: 32,
                fontSize: '0.875rem',
              }}
              aria-label={username ? `Eingeloggt als ${username}` : 'Eingeloggt'}
            >
              {initial}
            </Avatar>
            {username && (
              <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'inline' } }}>
                {username}
              </Typography>
            )}
            <Button
              color="inherit"
              startIcon={<LogoutIcon />}
              onClick={signOut}
              aria-label="Abmelden"
            >
              Logout
            </Button>
          </Box>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
          },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: 'auto' }}>
          <List>
            {navItems.map((node) =>
              isGroup(node) ? renderGroup(node) : renderLink(node, false),
            )}
          </List>
        </Box>
      </Drawer>
      <Box
        component="main"
        sx={{ flexGrow: 1, p: 3, bgcolor: 'background.default' }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
