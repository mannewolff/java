import { Link as RouterLink } from 'react-router-dom';
import { List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography } from '@mui/material';
import VpnKeyIcon from '@mui/icons-material/VpnKey';

export default function SettingsPage() {
  return (
    <>
      <Typography variant="h4" gutterBottom>
        Einstellungen
      </Typography>
      <List>
        <ListItem disablePadding>
          <ListItemButton component={RouterLink} to="/settings/tokens">
            <ListItemIcon>
              <VpnKeyIcon />
            </ListItemIcon>
            <ListItemText
              primary="Ingest-Tokens"
              secondary="Tokens für externen Schreibzugriff auf Zeitreihen verwalten"
            />
          </ListItemButton>
        </ListItem>
      </List>
    </>
  );
}
