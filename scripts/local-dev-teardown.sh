#!/usr/bin/env bash
# Cleanup zum Wechseln vom Dev-Build zurück auf einen "echten" Production-Build.
#
# Entfernt `frontend/.env.production.local` — danach greift wieder
# `frontend/.env.production` (Prod-Keycloak unter toolboxauth.mwolff.org).
#
# `.env` (root) bleibt erhalten — Passwörter manuell löschen wenn nötig.
#
# Nach diesem Script die Images neu bauen, damit das Frontend-Bundle die
# Prod-URLs einbäckt:
#
#   docker buildx prune -af
#   docker compose build --no-cache --pull api
#   docker compose stop api && docker compose rm -f api
#   docker compose up -d --no-deps api

set -euo pipefail

cd "$(dirname "$0")/.."

FE_ENV="frontend/.env.production.local"

if [[ -f "$FE_ENV" ]]; then
  rm "$FE_ENV"
  echo "✓ $FE_ENV entfernt."
else
  echo "✓ $FE_ENV war nicht da — nichts zu tun."
fi

cat <<'EOF'

Cleanup fertig. Für einen echten Production-mode-Build:

  docker buildx prune -af
  docker compose build --no-cache --pull api
  docker compose stop api && docker compose rm -f api
  docker compose up -d --no-deps api

Beim Aufruf von http://localhost:8080 redirected der Browser dann auf den
Production-Keycloak (toolboxauth.mwolff.org) — der kennt localhost:8080 nicht
als Redirect-URI, also kommt eine "Invalid redirect_uri"-Fehlermeldung.
Das ist erwartet — wer trotzdem Prod-Auth testen will, muss den Realm dort
ergänzen.
EOF
