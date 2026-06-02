#!/usr/bin/env bash
#
# increment-version.sh — erhöht die Minor-Version der laufenden Anwendung um eins.
#
# #225: Läuft als letzter Schritt des Deploys AUF dem Server und ruft die App über
# localhost (api-Port-Mapping 8080:8080) — bewusst NICHT über den öffentlichen
# Hostnamen, damit weder Reverse-Proxy noch Firewall/Geo-Filter dazwischenfunken.
# Ersetzt den früheren GitHub-Actions-Job, der den Prod-Server von außen nicht
# erreichen konnte (TCP-Timeout, siehe #223).
#
# Aufruf (nach `docker compose up -d --no-deps api`):
#   ./scripts/increment-version.sh
# Optional andere Basis-URL:
#   APP_BASE_URL=http://localhost:8080 ./scripts/increment-version.sh
#
set -euo pipefail

BASE_URL="${APP_BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
VERSION_URL="${BASE_URL}/api/app/version"
INCREMENT_URL="${BASE_URL}/api/app/version/increment-minor"

MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-60}"
SLEEP_SECONDS=3

echo "Warte auf erreichbare App unter ${VERSION_URL} (max ${MAX_WAIT_SECONDS}s) ..."
waited=0
until curl --fail --silent --show-error --max-time 5 "${VERSION_URL}" >/dev/null 2>&1; do
  if [ "${waited}" -ge "${MAX_WAIT_SECONDS}" ]; then
    echo "::error::App unter ${VERSION_URL} wurde nicht rechtzeitig gesund (${MAX_WAIT_SECONDS}s) — Increment abgebrochen (#225)." >&2
    exit 1
  fi
  sleep "${SLEEP_SECONDS}"
  waited=$((waited + SLEEP_SECONDS))
done

before="$(curl --fail --silent --max-time 5 "${VERSION_URL}")"
echo "Aktuelle Version: ${before}"

echo "Inkrementiere Minor-Version (POST ${INCREMENT_URL}) ..."
if ! after="$(curl --fail --silent --show-error --max-time 30 -X POST "${INCREMENT_URL}")"; then
  echo "::error::POST an ${INCREMENT_URL} ist fehlgeschlagen — Minor-Version NICHT erhöht (#225)." >&2
  exit 1
fi

echo "Neue Version: ${after}"
echo "Minor-Version erhöht."
