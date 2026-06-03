#!/usr/bin/env bash
#
# increment-version.sh — erhöht die Minor-Version der laufenden Anwendung um eins.
#
# #225: Läuft als letzter Schritt des Deploys AUF dem Server. Der api-Container ist
# in Produktion NICHT auf einen Host-Port gemappt (nur im Compose-Netz erreichbar,
# nginx spricht ihn intern an). Deshalb wird der HTTP-Aufruf per Wegwerf-curl-Container
# IM Netzwerk-Namespace des api-Containers gemacht — dort ist `localhost:8080` wieder
# der api. Kein Host-Port, kein Reverse-Proxy, keine Firewall/Geo-Filter (die den
# früheren GitHub-Actions-Job ausgesperrt hatten, siehe #223).
#
# Aufruf (nach `docker compose up -d --no-deps api`), egal aus welchem Verzeichnis:
#   ./scripts/increment-version.sh
#
# Override für den Direkt-HTTP-Modus (z. B. dev mit Host-Port-Mapping oder Jar ohne Docker):
#   APP_BASE_URL=http://localhost:8080 ./scripts/increment-version.sh
#
set -euo pipefail

# Immer aus dem Repo-Root arbeiten (docker compose braucht die compose-Datei im CWD).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/.."

API_SERVICE="${API_SERVICE:-api}"
CURL_IMAGE="${CURL_IMAGE:-curlimages/curl:8.10.1}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-60}"
SLEEP_SECONDS=3

INTERNAL="http://localhost:8080"
VERSION_PATH="/api/app/version"
INCREMENT_PATH="/api/app/version/increment-minor"

if [ -n "${APP_BASE_URL:-}" ]; then
  # Direkt-HTTP-Modus: Override-Basis-URL (dev mit Port-Mapping / Jar ohne Docker).
  base="${APP_BASE_URL%/}"
  target="${base}"
  get_version()  { curl --fail --silent --show-error --max-time 5  "${base}${VERSION_PATH}"; }
  do_increment() { curl --fail --silent --show-error --max-time 30 -X POST "${base}${INCREMENT_PATH}"; }
else
  # Docker-Modus (Default, prod-tauglich): curl im Netz des api-Containers.
  cid="$(docker compose ps -q "${API_SERVICE}")"
  if [ -z "${cid}" ]; then
    echo "::error::api-Container ('${API_SERVICE}') nicht gefunden — läuft 'docker compose up'? (#225)" >&2
    exit 1
  fi
  target="container:${cid:0:12} → ${INTERNAL}"
  get_version()  { docker run --rm --network "container:${cid}" "${CURL_IMAGE}" --fail --silent --show-error --max-time 5  "${INTERNAL}${VERSION_PATH}"; }
  do_increment() { docker run --rm --network "container:${cid}" "${CURL_IMAGE}" --fail --silent --show-error --max-time 30 -X POST "${INTERNAL}${INCREMENT_PATH}"; }
fi

echo "Warte auf erreichbare App (${target}, max ${MAX_WAIT_SECONDS}s) ..."
waited=0
until get_version >/dev/null 2>&1; do
  if [ "${waited}" -ge "${MAX_WAIT_SECONDS}" ]; then
    echo "::error::App wurde nicht rechtzeitig gesund (${MAX_WAIT_SECONDS}s) — Increment abgebrochen (#225)." >&2
    exit 1
  fi
  sleep "${SLEEP_SECONDS}"
  waited=$((waited + SLEEP_SECONDS))
done

before="$(get_version)"
echo "Aktuelle Version: ${before}"

echo "Inkrementiere Minor-Version (POST ${INCREMENT_PATH}) ..."
if ! after="$(do_increment)"; then
  echo "::error::increment-minor fehlgeschlagen — Minor-Version NICHT erhöht (#225)." >&2
  exit 1
fi

echo "Neue Version: ${after}"
echo "Minor-Version erhöht."
