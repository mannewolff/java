#!/usr/bin/env bash
# Wendet die Realm-Aenderungen aus #284/#287/#210 auf einen LAUFENDEN Keycloak
# an, ohne die Realm-JSONs neu zu importieren (und damit ohne die MariaDB zu
# wipen). Gedacht fuer Umgebungen mit bestehenden echten Nutzerdaten (Prod),
# in denen `docker compose down -v` keine Option ist — `--import-realm`
# ueberspringt einen bereits existierenden Realm ohnehin komplett, die drei
# Aenderungen muessen daher separat ueber die Admin-REST-API nachgezogen
# werden.
#
# Angewendete Aenderungen (idempotent — mehrfaches Ausfuehren aendert nichts,
# wenn schon alles vorhanden ist):
#   1. Client "toolbox-cli" anlegen (Device Authorization Grant, RFC 8628,
#      public, kein Secret) inkl. toolbox-api-audience-Mapper — #284.
#   2. defaultClientScopes von "toolbox-web" und "toolbox-ios" um "basic"
#      ergaenzen (fehlender sub-Claim ohne diesen Scope) — #287.
#   3. Realm-Rolle "USER" als Composite um "offline_access" erweitern,
#      damit das Device-Flow-Login ueberhaupt ein Offline-Token bekommt — #210.
#
# Aufruf:
#   KEYCLOAK_URL=https://toolboxauth.mwolff.org \
#   KEYCLOAK_ADMIN=admin \
#   KEYCLOAK_ADMIN_PASSWORD=*** \
#   REALM=toolbox \
#   ./scripts/keycloak-apply-cli-changes.sh --dry-run   # zeigt nur, was fehlt
#   ./scripts/keycloak-apply-cli-changes.sh --apply     # wendet es an
#
# Voraussetzung: curl, jq. Keine Schreiboperation ausser den drei oben
# genannten — es wird nichts geloescht, keine bestehenden User/Items
# angefasst.

set -euo pipefail

MODE="${1:-}"
if [[ "$MODE" != "--dry-run" && "$MODE" != "--apply" ]]; then
  echo "Aufruf: $0 --dry-run|--apply" >&2
  exit 1
fi

: "${KEYCLOAK_URL:?KEYCLOAK_URL muss gesetzt sein (z. B. https://toolboxauth.mwolff.org oder http://localhost:8081)}"
: "${KEYCLOAK_ADMIN:?KEYCLOAK_ADMIN muss gesetzt sein}"
: "${KEYCLOAK_ADMIN_PASSWORD:?KEYCLOAK_ADMIN_PASSWORD muss gesetzt sein}"
: "${REALM:?REALM muss gesetzt sein (z. B. toolbox oder toolbox-dev)}"

for bin in curl jq; do
  command -v "$bin" >/dev/null 2>&1 || { echo "FEHLER: '$bin' wird benoetigt, ist aber nicht installiert." >&2; exit 1; }
done

DRY_RUN=1
[[ "$MODE" == "--apply" ]] && DRY_RUN=0

log() { echo "[$([[ $DRY_RUN -eq 1 ]] && echo DRY-RUN || echo APPLY)] $*"; }

# Ohne dies stirbt das Script bei einem fehlschlagenden `curl -sf` (z. B. HTTP 401, wenn das
# Admin-Token nach 60 s zwischen zwei Schritten ablaeuft) wegen `set -e` WORTLOS — die eigenen
# FEHLER-Meldungen weiter unten werden nie erreicht (#314). Der ERR-Trap gibt stattdessen einen
# verstaendlichen Hinweis aus, bevor `set -e` das Script beendet.
trap 'rc=$?; echo "FEHLER: Ein Keycloak-API-Aufruf ist fehlgeschlagen (Exit $rc, Zeile $LINENO)." >&2;
      echo "       Haeufigste Ursache: das Admin-Token ist abgelaufen (Lebensdauer 60 s) oder die" >&2;
      echo "       URL/Zugangsdaten stimmen nicht. Das Script ist idempotent — einfach erneut" >&2;
      echo "       ausfuehren." >&2' ERR

# ---------- Admin-Token -------------------------------------------------------

ADMIN_TOKEN=$(curl -sf -d "client_id=admin-cli" -d "username=$KEYCLOAK_ADMIN" \
  -d "password=$KEYCLOAK_ADMIN_PASSWORD" -d "grant_type=password" \
  "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" | jq -r '.access_token')

if [[ -z "$ADMIN_TOKEN" || "$ADMIN_TOKEN" == "null" ]]; then
  echo "FEHLER: Konnte kein Admin-Token holen — Zugangsdaten/URL pruefen." >&2
  exit 1
fi

auth=(-H "Authorization: Bearer $ADMIN_TOKEN")
api="$KEYCLOAK_URL/admin/realms/$REALM"

# ---------- 1. Client toolbox-cli ---------------------------------------------

existing_cli_client=$(curl -sf "${auth[@]}" "$api/clients?clientId=toolbox-cli")
if [[ "$(echo "$existing_cli_client" | jq 'length')" -gt 0 ]]; then
  log "Client 'toolbox-cli' existiert bereits — ueberspringe Anlage."
  cli_client_id=$(echo "$existing_cli_client" | jq -r '.[0].id')
else
  log "Client 'toolbox-cli' fehlt — wird angelegt."
  if [[ $DRY_RUN -eq 0 ]]; then
    curl -sf "${auth[@]}" -H "Content-Type: application/json" -X POST "$api/clients" -d '{
      "clientId": "toolbox-cli",
      "name": "Toolbox CLI (tbx)",
      "description": "Kommandozeilen-Client fuer den Board-Adapter (Device Authorization Grant, RFC 8628) — kein Redirect, kein Secret.",
      "enabled": true,
      "publicClient": true,
      "standardFlowEnabled": false,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "serviceAccountsEnabled": false,
      "redirectUris": [],
      "attributes": {
        "oauth2.device.authorization.grant.enabled": "true",
        "oauth2.device.polling.interval": "5"
      },
      "defaultClientScopes": ["web-origins", "acr", "profile", "roles", "basic", "email"],
      "optionalClientScopes": ["offline_access"]
    }'
    cli_client_id=$(curl -sf "${auth[@]}" "$api/clients?clientId=toolbox-cli" | jq -r '.[0].id')
  else
    cli_client_id=""
  fi
fi

if [[ -n "${cli_client_id:-}" ]]; then
  existing_mapper=$(curl -sf "${auth[@]}" "$api/clients/$cli_client_id/protocol-mappers/models" \
    | jq '[.[] | select(.name=="toolbox-api-audience")] | length')
  if [[ "$existing_mapper" -gt 0 ]]; then
    log "Protocol-Mapper 'toolbox-api-audience' auf toolbox-cli existiert bereits."
  else
    log "Protocol-Mapper 'toolbox-api-audience' fehlt auf toolbox-cli — wird angelegt."
    if [[ $DRY_RUN -eq 0 ]]; then
      curl -sf "${auth[@]}" -H "Content-Type: application/json" \
        -X POST "$api/clients/$cli_client_id/protocol-mappers/models" -d '{
        "name": "toolbox-api-audience",
        "protocol": "openid-connect",
        "protocolMapper": "oidc-audience-mapper",
        "consentRequired": false,
        "config": {
          "included.client.audience": "toolbox-api",
          "id.token.claim": "false",
          "access.token.claim": "true"
        }
      }'
    fi
  fi
fi

# ---------- 2. basic-Scope fuer toolbox-web / toolbox-ios ---------------------

basic_scope_id=$(curl -sf "${auth[@]}" "$api/client-scopes" | jq -r '.[] | select(.name=="basic") | .id')
if [[ -z "$basic_scope_id" ]]; then
  echo "FEHLER: Client-Scope 'basic' nicht im Realm gefunden — Realm-Setup pruefen." >&2
  exit 1
fi

for client_id_name in toolbox-web toolbox-ios; do
  target_client=$(curl -sf "${auth[@]}" "$api/clients?clientId=$client_id_name")
  if [[ "$(echo "$target_client" | jq 'length')" -eq 0 ]]; then
    log "Client '$client_id_name' existiert nicht im Realm '$REALM' — ueberspringe (Realm ohne diesen Client?)."
    continue
  fi
  internal_id=$(echo "$target_client" | jq -r '.[0].id')

  has_basic=$(curl -sf "${auth[@]}" "$api/clients/$internal_id/default-client-scopes" \
    | jq '[.[] | select(.name=="basic")] | length')
  if [[ "$has_basic" -gt 0 ]]; then
    log "'$client_id_name' hat 'basic' bereits als Default-Scope."
  else
    log "'$client_id_name' fehlt 'basic' als Default-Scope — wird ergaenzt."
    if [[ $DRY_RUN -eq 0 ]]; then
      curl -sf "${auth[@]}" -X PUT "$api/clients/$internal_id/default-client-scopes/$basic_scope_id"
    fi
  fi
done

# ---------- 3. USER-Rolle als Composite um offline_access erweitern -----------

user_role=$(curl -sf "${auth[@]}" "$api/roles/USER")
if [[ -z "$(echo "$user_role" | jq -r '.name // empty')" ]]; then
  echo "FEHLER: Realm-Rolle 'USER' nicht gefunden — Realm-Setup pruefen." >&2
  exit 1
fi

has_offline_composite=$(curl -sf "${auth[@]}" "$api/roles/USER/composites" \
  | jq '[.[] | select(.name=="offline_access")] | length')
if [[ "$has_offline_composite" -gt 0 ]]; then
  log "Rolle 'USER' ist bereits Composite mit 'offline_access'."
else
  log "Rolle 'USER' hat 'offline_access' noch nicht als Composite — wird ergaenzt."
  offline_role=$(curl -sf "${auth[@]}" "$api/roles/offline_access")
  if [[ $DRY_RUN -eq 0 ]]; then
    curl -sf "${auth[@]}" -H "Content-Type: application/json" \
      -X POST "$api/roles/USER/composites" -d "[$offline_role]"
  fi
fi

echo
if [[ $DRY_RUN -eq 1 ]]; then
  echo "Dry-Run beendet — keine Aenderung wurde geschrieben. Mit --apply erneut aufrufen, um anzuwenden."
else
  echo "Alle Aenderungen angewendet (bereits vorhandene wurden uebersprungen)."
fi
