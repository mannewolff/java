import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Collapse,
  CssBaseline,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LogoutIcon from '@mui/icons-material/Logout';
import { Outlet, useLocation, useMatch, useNavigate } from 'react-router-dom';
import { navItems } from './navItems';
import type { NavGroup, NavLink, NavNode } from './navItems';
import { useAuth } from '../auth/useAuth';
import { useEditMode } from '../pages/dashboard/EditModeContext';
import { useKioskMode } from '../pages/dashboard/KioskModeContext';
import WidgetPalette from '../pages/dashboard/WidgetPalette';
import type { WidgetType } from '../api/dashboard';
import { getAppVersion } from '../api/appVersion';

const DRAWER_WIDTH = 240;
const DRAWER_COLLAPSED_WIDTH = 56;
const STORAGE_KEY = 'sidebar-collapsed';

function readCollapsed(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

function writeCollapsed(value: boolean): void {
  try {
    localStorage.setItem(STORAGE_KEY, String(value));
  } catch {
    // localStorage nicht verfügbar — kein Hard-Fail
  }
}

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
  const { editMode, setEditMode, setDraggingType } = useEditMode();
  const { kioskMode } = useKioskMode();

  const [collapsed, setCollapsed] = useState<boolean>(readCollapsed);
  // Anwendungsversion (z. B. "0.1") aus dem Backend; bei Fehler bleibt sie leer (graceful).
  const [version, setVersion] = useState<string>('');

  useEffect(() => {
    let cancelled = false;
    getAppVersion()
      .then((v) => {
        if (!cancelled) setVersion(`${v.major}.${v.minor}`);
      })
      .catch(() => {
        // Graceful Degradation: ohne Version-Suffix weiteranzeigen.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const toggleCollapsed = (): void => {
    setCollapsed((prev) => {
      const next = !prev;
      writeCollapsed(next);
      return next;
    });
  };

  // Auf der Mobile-Seite (#195) wird die Sidebar automatisch eingeklappt und der vorherige
  // Zustand beim Verlassen wiederhergestellt. Das umgeht bewusst writeCollapsed(), damit die
  // gespeicherte Nutzer-Präferenz unangetastet bleibt.
  const onMobile = location.pathname.startsWith('/mobile');
  const restoreCollapsedRef = useRef<boolean | null>(null);
  useEffect(() => {
    if (onMobile) {
      if (restoreCollapsedRef.current === null) {
        restoreCollapsedRef.current = collapsed;
        setCollapsed(true);
      }
    } else if (restoreCollapsedRef.current !== null) {
      setCollapsed(restoreCollapsedRef.current);
      restoreCollapsedRef.current = null;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [onMobile]);

  // Auf einer Dashboard-Detail-Route? `useMatch` matched genau `/dashboards/:id`
  // (nicht `/dashboards` selbst, nicht `/dashboards/default`).
  const dashboardDetailMatch = useMatch('/dashboards/:id');
  const isOnDashboardDetail = Boolean(dashboardDetailMatch && dashboardDetailMatch.params.id !== 'default');
  const sidebarShowsPalette = isOnDashboardDetail && editMode;

  // Beim Verlassen der Dashboard-Detail-Route automatisch in den Read-Modus zurück.
  useMemo(() => {
    if (!isOnDashboardDetail && editMode) {
      setEditMode(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOnDashboardDetail]);

  const handlePaletteDragStart = (type: WidgetType): void => {
    setDraggingType(type);
  };

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

    if (collapsed) {
      return (
        <Tooltip key={link.path} title={link.label} placement="right">
          <ListItem disablePadding>
            <ListItemButton
              selected={selected}
              onClick={() => navigate(link.path)}
              aria-selected={selected}
              aria-label={link.label}
              sx={{ justifyContent: 'center', px: 1 }}
            >
              <ListItemIcon sx={{ minWidth: 0 }}>
                <Icon color={selected ? 'primary' : 'inherit'} />
              </ListItemIcon>
            </ListItemButton>
          </ListItem>
        </Tooltip>
      );
    }

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

    if (collapsed) {
      // Im collapsed Modus: nur Gruppen-Icon mit Tooltip, keine Kinder
      return (
        <Tooltip key={group.label} title={group.label} placement="right">
          <ListItem disablePadding>
            <ListItemButton
              aria-label={group.label}
              sx={{ justifyContent: 'center', px: 1 }}
            >
              <ListItemIcon sx={{ minWidth: 0 }}>
                <GroupIcon color={hasActiveChild ? 'primary' : 'inherit'} />
              </ListItemIcon>
            </ListItemButton>
          </ListItem>
        </Tooltip>
      );
    }

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

  const drawerWidth = collapsed ? DRAWER_COLLAPSED_WIDTH : DRAWER_WIDTH;

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
            sx={{ height: 32, mr: 1.5 }}
          />
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            {version ? `mannewolff-tools v${version}` : 'mannewolff-tools'}
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
          width: kioskMode ? 0 : drawerWidth,
          flexShrink: 0,
          display: kioskMode ? 'none' : 'block',
          transition: (t) =>
            t.transitions.create('width', {
              easing: t.transitions.easing.sharp,
              duration: t.transitions.duration.enteringScreen,
            }),
          [`& .MuiDrawer-paper`]: {
            width: drawerWidth,
            boxSizing: 'border-box',
            overflowX: 'hidden',
            transition: (t) =>
              t.transitions.create('width', {
                easing: t.transitions.easing.sharp,
                duration: t.transitions.duration.enteringScreen,
              }),
          },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: 'auto', display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Box sx={{ flexGrow: 1 }}>
            {sidebarShowsPalette ? (
              <WidgetPalette onDragStartWidget={handlePaletteDragStart} />
            ) : (
              <List>
                {navItems.map((node) =>
                  isGroup(node) ? renderGroup(node) : renderLink(node, false),
                )}
              </List>
            )}
          </Box>
          <Box>
            <Divider />
            <Box sx={{ display: 'flex', justifyContent: collapsed ? 'center' : 'flex-end', p: 0.5 }}>
              <Tooltip title={collapsed ? 'Menü ausklappen' : 'Menü einklappen'} placement="right">
                <IconButton
                  onClick={toggleCollapsed}
                  size="small"
                  aria-label={collapsed ? 'Menü ausklappen' : 'Menü einklappen'}
                >
                  {collapsed ? <ChevronRightIcon /> : <ChevronLeftIcon />}
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
        </Box>
      </Drawer>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          bgcolor: 'background.default',
          transition: (t) =>
            t.transitions.create('margin', {
              easing: t.transitions.easing.sharp,
              duration: t.transitions.duration.enteringScreen,
            }),
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
