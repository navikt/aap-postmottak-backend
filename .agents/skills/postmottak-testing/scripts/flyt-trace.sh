#!/usr/bin/env bash
# Extracts a compact flyt-engine trace (jobs, steps, avklaringsbehov, rewinds, errors) from test output.
# Flyt-test reports are large (Flyttest XML is ~1 MB); use this instead of reading them.
#
# Usage:
#   flyt-trace.sh <module | report.xml | gradle-log-file> [extra ERE filter]
#
# Examples:
#   flyt-trace.sh flyt                           # all JUnit XML reports from the last :flyt:test run
#   flyt-trace.sh flyt '\[b=2\]'                 # only lines for behandlingId 2
#   flyt-trace.sh flyt/build/test-results/test/TEST-no.nav.aap.postmottak.flyt.Flyttest.xml
#   flyt-trace.sh /tmp/postmottak-test-flyt-abc123   # log written by run-tests.sh (stdout is only there with -i)
#
# Output format: <time> [b=<behandlingId>|b=-] <Logger>: <message>   (INFO level omitted, WARN/ERROR kept)
# The JUnit XML holds output for a whole test class (not per test). Run a single test method for a clean trace.
#
# Environment: TRACE_WIDTH (default 220) truncates each line.
set -uo pipefail

if [[ $# -lt 1 ]]; then
  sed -n '2,17p' "$0"
  exit 2
fi

source_arg="$1"
extra_filter="${2:-}"
root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

files=()
if [[ -f "$source_arg" ]]; then
  files=("$source_arg")
elif [[ -d "$root/$source_arg/build/test-results/test" ]]; then
  for f in "$root/$source_arg"/build/test-results/test/TEST-*.xml; do
    [[ -e "$f" ]] && files+=("$f")
  done
fi

if [[ ${#files[@]} -eq 0 ]]; then
  echo "Found no log/report for '$source_arg'. Give a file, or a module that has build/test-results/test/TEST-*.xml." >&2
  exit 2
fi

events='Plukket jobb|Fullført jobb|Feil under prosessering av jobb|Planlagt kjøring av jobb|Behandler steg|Resultat fra steg|Neste utledet steg|Stopper opp|nådd slutten|Tilbakeført|Fant avklaringsbehov|Fant ventebehov|Fant relevante avklaringsbehov|Legger til avklaringsbehov|Løser avklaringsbehov|Forsøker løse avklaringsbehov|Avbryter ventebehov|Tar av vent|Fant relevante endringer|har passert det åpne| (WARN|ERROR) '
exception_line='^[[:space:]]*(Caused by: )?[a-zA-Z0-9_.$]+(Exception|Error)(: .*)?$'
noise='GatewayRegistry|ResourceReaper|testcontainers|org\.apache\.kafka|io\.confluent|Autoreload|DockerClientProviderStrategy|StreamsConfig: Processing exception handler'

esc=$'\033'

cat "${files[@]}" \
  | sed -e "s/${esc}\[[0-9;]*m//g" -e 's/?\[[0-9;]*m//g' \
  | grep -E "($events)|$exception_line" \
  | grep -Ev "$noise" \
  | sed -E \
      -e 's/^[[:space:]]+//' \
      -e 's/^([0-9:.]+) \[[^]]*\] /\1 /' \
      -e "s/\[jobb='[^']*', sakId='[^']*', behandlingId=''\] - /[b=-] /" \
      -e "s/\[jobb='[^']*', sakId='[^']*', behandlingId='([^']*)'\] - /[b=\1] /" \
      -e 's/ (INFO|DEBUG|TRACE) +([A-Za-z0-9_$]+\.)*([A-Za-z0-9_$]+) +(\[b=[^]]*\]) / \4 \3: /' \
      -e 's/ (WARN|ERROR) +([A-Za-z0-9_$]+\.)*([A-Za-z0-9_$]+) +(\[b=[^]]*\]) / \4 \1 \3: /' \
      -e 's/MotorImpl\$Forbrenningskammer/Motor/' \
  | { if [[ -n "$extra_filter" ]]; then grep -E -- "$extra_filter"; else cat; fi; } \
  | cut -c1-"${TRACE_WIDTH:-220}"
