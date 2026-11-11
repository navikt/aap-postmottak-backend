#!/usr/bin/env bash
# Runs a narrow set of tests and prints a compact summary, so agents never need to read the full Gradle log.
#
# Usage:
#   run-tests.sh <module> [test-filter ...]
#
# Examples:
#   run-tests.sh flyt 'no.nav.aap.postmottak.forretningsflyt.steg.journalføring.AvklarSakStegTest'
#   run-tests.sh flyt 'no.nav.aap.postmottak.flyt.Flyttest.kjører en manuell søknad igjennom hele flyten'
#   run-tests.sh repository '*JournalpostRepositoryImplTest'
#   run-tests.sh flyt '*AvklarSakStegTest' '*AvklarTemaStegTest'
#
# Environment:
#   MAX_WORKERS  Gradle --max-workers (default 2). Also caps parallel test JVMs, and therefore containers.
#   RERUN        1 (default) passes --rerun so Gradle cannot skip the test task as UP-TO-DATE/FROM-CACHE.
#                Set RERUN=0 to allow Gradle's up-to-date check and build cache.
#
# The full Gradle log is written to a temp file (path is printed). JUnit XML is in <module>/build/test-results/test/
# and only contains the classes from the LAST run of that module's test task.
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
  echo "No test sources for module '$module' (expected $module/src/test)." >&2
  exit 2
fi

log="$(mktemp "${TMPDIR:-/tmp}/postmottak-test-${module}-XXXXXX")"
args=(":${module}:test")
if [[ "${RERUN:-1}" == "1" ]]; then
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

# Test event lines look like "ClassName > testName() PASSED". Task lines ("> Task :x:test FAILED") must not count.
test_lines() { grep -E " > .* $1\$" "$log" | grep -v '^> Task'; }
passed=$(test_lines PASSED | grep -c .)
failed=$(test_lines FAILED | grep -c .)
skipped=$(test_lines SKIPPED | grep -c .)

echo "Module: $module | exit=$status | time=$((end - start))s | passed=$passed failed=$failed skipped=$skipped"
echo "Full log: $log"

if [[ $failed -gt 0 ]]; then
  echo
  echo "Failed tests:"
  # The FAILED line plus Gradle's indented exception summary lines that follow it.
  awk '
    / > .* FAILED$/ && !/^> Task/ { print; grab = 1; n = 0; next }
    grab && /^    [^ ]/ && n < 5 { print; n++; next }
    { grab = 0 }
  ' "$log" | head -60

  report_dir="$module/build/test-results/test"
  if [[ -d "$report_dir" ]]; then
    echo
    echo "Failure messages from JUnit XML (truncated):"
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
  echo "Build failed without test failures (compilation, no matching tests, Docker, ...). Excerpt:"
  grep -E '^e: |error:|No tests found|What went wrong|Could not|Exception' "$log" | head -30
fi

if [[ $passed -eq 0 && $failed -eq 0 && $status -eq 0 ]]; then
  echo
  echo "Warning: no tests reported. Check that the filter matches (fully qualified class name, or a '*' prefix)."
fi

exit $status
