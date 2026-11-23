#!/usr/bin/env bash
# Kjører et smalt sett med tester og skriver ut en kompakt oppsummering, slik at agenter slipper å lese hele Gradle-loggen.
#
# Bruk:
#   run-tests.sh <modul> [testfilter ...]
#
# Eksempler:
#   run-tests.sh flyt 'no.nav.aap.postmottak.forretningsflyt.steg.journalføring.AvklarSakStegTest'
#   run-tests.sh flyt 'no.nav.aap.postmottak.flyt.Flyttest.kjører en manuell søknad igjennom hele flyten'
#   run-tests.sh repository '*JournalpostRepositoryImplTest'
#   run-tests.sh flyt '*AvklarSakStegTest' '*AvklarTemaStegTest'
#
# Miljø:
#   MAX_WORKERS  Gradle --max-workers (standard 2). Begrenser også parallelle test-JVM-er og dermed containere.
#   RERUN        0 (standard) bruker Gradles up-to-date-sjekk og build-cache.
#                Sett RERUN=1 for å sende --rerun og tvinge testtasken til å kjøre.
#
# Hele Gradle-loggen skrives til en midlertidig fil (stien skrives ut). JUnit-XML ligger i <modul>/build/test-results/test/
# og inneholder bare klassene fra SISTE kjøring av modulens testtask.
set -uo pipefail

if [[ $# -lt 1 ]]; then
  sed -n '2,19p' "$0"
  exit 2
fi

module="$1"
shift

root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$root" || exit 2

if [[ ! -d "$module/src/test" ]]; then
  echo "Ingen testkilder for modulen '$module' (forventet $module/src/test)." >&2
  exit 2
fi

log="$(mktemp "${TMPDIR:-/tmp}/postmottak-test-${module}-XXXXXX")"
args=(":${module}:test")
if [[ "${RERUN:-0}" == "1" ]]; then
  args+=(--rerun)
fi
args+=(--console=plain "--max-workers=${MAX_WORKERS:-2}")
for filter in "$@"; do
  args+=(--tests "$filter")
done

start=$(date +%s)
./gradlew "${args[@]}" >"$log" 2>&1
status=$?
end=$(date +%s)

# Testhendelseslinjer ser slik ut: "ClassName > testName() PASSED". Task-linjer ("> Task :x:test FAILED") skal ikke telles.
test_lines() { grep -E " > .* $1\$" "$log" | grep -v '^> Task'; }
passed=$(test_lines PASSED | grep -c .)
failed=$(test_lines FAILED | grep -c .)
skipped=$(test_lines SKIPPED | grep -c .)

echo "Modul: $module | exit=$status | time=$((end - start))s | passed=$passed failed=$failed skipped=$skipped"
echo "Full logg: $log"

if [[ $failed -gt 0 ]]; then
  echo
  echo "Feilede tester:"
  # FAILED-linjen pluss Gradles innrykkede oppsummeringslinjer for exception som følger etter den.
  awk '
    / > .* FAILED$/ && !/^> Task/ { print; grab = 1; n = 0; next }
    grab && /^    [^ ]/ && n < 5 { print; n++; next }
    { grab = 0 }
  ' "$log" | head -60

  report_dir="$module/build/test-results/test"
  if [[ -d "$report_dir" ]]; then
    echo
    echo "Feilmeldinger fra JUnit-XML (avkortet):"
    for file in "$report_dir"/TEST-*.xml; do
      [[ -e "$file" ]] || continue
      awk '
        /<testcase / {
          if (match($0, /name="[^"]*"/)) { testname = substr($0, RSTART + 6, RLENGTH - 7) }
        }
        /<failure / {
          msg = ""
          if (match($0, /message="[^"]*"/)) { msg = substr($0, RSTART + 9, RLENGTH - 10) }
          gsub(/&#10;/, " ", msg); gsub(/&quot;/, "\"", msg); gsub(/&lt;/, "<", msg)
          gsub(/&gt;/, ">", msg); gsub(/&amp;/, "\\&", msg); gsub(/&apos;/, "\x27", msg)
          print "- " testname ": " substr(msg, 1, 400)
        }
      ' "$file"
    done
  fi
fi

if [[ $status -ne 0 && $failed -eq 0 ]]; then
  echo
  echo "Bygget feilet uten testfeil (kompilering, ingen treff på tester, Docker, ...). Utdrag:"
  grep -E '^e: |error:|No tests found|What went wrong|Could not|Exception' "$log" | head -30
fi

if [[ $passed -eq 0 && $failed -eq 0 && $status -eq 0 ]]; then
  echo
  if grep -qE "^> Task :${module}:test (UP-TO-DATE|FROM-CACHE)" "$log"; then
    grep -E "^> Task :${module}:test (UP-TO-DATE|FROM-CACHE)" "$log"
  else
    echo "Advarsel: ingen tester rapportert. Sjekk at filteret treffer (fullt kvalifisert klassenavn, eller prefiks med '*')."
  fi
fi

exit $status
