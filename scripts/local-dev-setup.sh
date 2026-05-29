#!/usr/bin/env bash
# Setup für lokales Docker-Deployment.
#
# Was es tut:
#  1. Legt `.env` aus `.env.example` an, wenn noch nicht da.
#  2. Schreibt `frontend/.env.production.local` mit Dev-Keycloak-Werten.
#     `.env.production.local` ist von Vite-Konvention in der höchsten
#     Prioritätsstufe für `npm run build` — überschreibt damit die
#     versionierten Prod-Werte aus `.env.production`, ohne sie zu editieren.
#     Die Datei ist in `.gitignore` und kommt nie ins Repo.
#
# Idempotent: zweimaliges Ausführen ändert nichts (außer `.env` neu erzeugt
# wird, wenn jemand sie zwischendurch gelöscht hat).
#
# Aufruf:  ./scripts/local-dev-setup.sh
# Cleanup: ./scripts/local-dev-teardown.sh

set -euo pipefail

cd "$(dirname "$0")/.."

# ---------- 1. .env -----------------------------------------------------------

if [[ ! -f .env ]]; then
  if [[ ! -f .env.example ]]; then
    echo "FEHLER: .env.example fehlt — kein Template zum Kopieren da." >&2
    exit 1
  fi
  cp .env.example .env
  echo "✓ .env aus .env.example erzeugt — Passwörter in .env anpassen!"
else
  echo "✓ .env existiert bereits — nicht angefasst."
fi

# ---------- 2. frontend/.env.production.local --------------------------------

FE_ENV="frontend/.env.production.local"

cat > "$FE_ENV" <<'EOF'
# Lokales Override für den Docker-Build (Vite priorisiert .env.production.local
# höher als .env.production). Erzeugt von scripts/local-dev-setup.sh.
# In .gitignore — nicht committen.
VITE_KEYCLOAK_URL=http://localhost:8081
VITE_KEYCLOAK_REALM=toolbox-dev
VITE_KEYCLOAK_CLIENT_ID=toolbox-web
EOF

echo "✓ $FE_ENV geschrieben (Dev-Keycloak-Werte)."

# ---------- 3. Hinweis --------------------------------------------------------

cat <<EOF

Setup fertig. Nächste Schritte:

  1. Passwörter in .env anpassen (mindestens DB_PASSWORD, KEYCLOAK_ADMIN_PASSWORD,
     KEYCLOAK_DB_PASSWORD, DB_ROOT_PASSWORD).

  2. Stack starten:
       docker compose up --build -d

  3. Im Browser: http://localhost:8080 — landet auf Keycloak-Login unter
     http://localhost:8081/realms/toolbox-dev/...

  4. Self-Register, dann als Admin (siehe .env KEYCLOAK_ADMIN) den neuen User
     von Rolle PENDING auf USER promoten:
       http://localhost:8081/admin/  →  Realm "toolbox-dev"  →  Users  →
       <neuer User>  →  Role mapping  →  Realm roles  →  Assign role  →  USER

  Cleanup (z. B. um den Production-Build lokal zu testen):
       ./scripts/local-dev-teardown.sh
EOF
