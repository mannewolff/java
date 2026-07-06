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

# #229: Shared-Secret fuer den X-Version-Token-Header. Aus der Umgebung oder aus .env
# (gleiche Quelle wie docker compose). Fehlt es, lehnen die increment-Endpunkte mit 401 ab.
if [ -z "${APP_VERSION_INCREMENT_SECRET:-}" ] && [ -f .env ]; then
  raw="$(grep -E '^APP_VERSION_INCREMENT_SECRET=' .env | tail -n1 | cut -d= -f2- || true)"
  # .env-Wert robust lesen (#314): trailing CR (Windows-Zeilenende) und ein Paar umschliessende
  # Anfuehrungszeichen entfernen — sonst landen sie im Header und der Server antwortet mit 401.
  raw="${raw%$'\r'}"
  case "$raw" in
    \"*\") raw="${raw#\"}"; raw="${raw%\"}" ;;
    \'*\') raw="${raw#\'}"; raw="${raw%\'}" ;;
  esac
  APP_VERSION_INCREMENT_SECRET="$raw"
fi
if [ -z "${APP_VERSION_INCREMENT_SECRET:-}" ]; then
  echo "::error::APP_VERSION_INCREMENT_SECRET nicht gesetzt (Env oder .env) — Increment wird 401 (#229)." >&2
  exit 1
fi

API_SERVICE="${API_SERVICE:-api}"
CURL_IMAGE="${CURL_IMAGE:-curlimages/curl:8.10.1}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-60}"
SLEEP_SECONDS=3

INTERNAL="http://localhost:8080"
# Health ueber den oeffentlichen Actuator-Endpoint pruefen — GET /api/app/version ist seit
# #229 ROLE_USER-pflichtig und ohne JWT nicht abrufbar.
HEALTH_PATH="/actuator/health"
INCREMENT_PATH="/api/app/version/increment-minor"
TOKEN_HEADER="X-Version-Token: ${APP_VERSION_INCREMENT_SECRET}"

if [ -n "${APP_BASE_URL:-}" ]; then
  # Direkt-HTTP-Modus: Override-Basis-URL (dev mit Port-Mapping / Jar ohne Docker).
  base="${APP_BASE_URL%/}"
  target="${base}"
  health()       { curl --fail --silent --show-error --max-time 5  "${base}${HEALTH_PATH}"; }
  # Secret NICHT als -H-Argument (waere in `ps aux` sichtbar, #314), sondern ueber eine
  # curl-Konfiguration von stdin (`-K -`) — der Header-Wert verlaesst nie die Prozess-Args.
  do_increment() {
    printf 'header = "%s"\n' "${TOKEN_HEADER}" \
      | curl --fail --silent --show-error --max-time 30 -X POST -K - "${base}${INCREMENT_PATH}"
  }
else
  # Docker-Modus (Default, prod-tauglich): curl im Netz des api-Containers.
  cid="$(docker compose ps -q "${API_SERVICE}")"
  if [ -z "${cid}" ]; then
    echo "::error::api-Container ('${API_SERVICE}') nicht gefunden — läuft 'docker compose up'? (#225)" >&2
    exit 1
  fi
  target="container:${cid:0:12} → ${INTERNAL}"
  health()       { docker run --rm --network "container:${cid}" "${CURL_IMAGE}" --fail --silent --show-error --max-time 5  "${INTERNAL}${HEALTH_PATH}"; }
  # Secret weder als -H-Argument noch als `docker run`-Arg (waere in `ps aux`/`docker inspect`
  # sichtbar, #314): per stdin an einen mit -i gestarteten curl-Container, curl liest die
  # Konfiguration mit `-K -`.
  do_increment() {
    printf 'header = "%s"\n' "${TOKEN_HEADER}" \
      | docker run --rm -i --network "container:${cid}" "${CURL_IMAGE}" \
          --fail --silent --show-error --max-time 30 -X POST -K - "${INTERNAL}${INCREMENT_PATH}"
  }
fi

echo "Warte auf gesunde App (${target}, max ${MAX_WAIT_SECONDS}s) ..."
waited=0
until health >/dev/null 2>&1; do
  if [ "${waited}" -ge "${MAX_WAIT_SECONDS}" ]; then
    echo "::error::App wurde nicht rechtzeitig gesund (${MAX_WAIT_SECONDS}s) — Increment abgebrochen (#225)." >&2
    exit 1
  fi
  sleep "${SLEEP_SECONDS}"
  waited=$((waited + SLEEP_SECONDS))
done

echo "Inkrementiere Minor-Version (POST ${INCREMENT_PATH}) ..."
if ! after="$(do_increment)"; then
  echo "::error::increment-minor fehlgeschlagen — Minor-Version NICHT erhöht (401? Secret falsch?, #229) (#225)." >&2
  exit 1
fi

echo "Neue Version: ${after}"
echo "Minor-Version erhöht."
