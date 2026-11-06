#!/usr/bin/env bash
# preToolUse-hook: ber om bekreftelse før agenten endrer filer som brekker historikk eller publiserer kontrakt,
# og nekter push til main, force-push og lesing av .env-filer. Leser hook-input som JSON på stdin. Krever jq.
#
# Feltnavnene i input er ikke fullt dokumentert og varierer mellom Copilot CLI, VS Code og IntelliJ: verktøynavnet
# står i toolName eller tool_name, argumentene i toolArgs eller tool_input (objekt eller JSON-streng), filstien i
# path, file_path eller filePath, og apply_patch har stiene i patchteksten. Hooken leter derfor etter alle.
# Svaret står både på toppnivå (Copilot) og i hookSpecificOutput (VS Code-format). Hooken avslutter alltid med 0:
# en hook som krasjer, blir tolket som avslag for alle verktøy.

if ! command -v jq >/dev/null 2>&1; then
  echo "beskytt-sensitive-filer: jq mangler, så hooken sjekker ingenting" >&2
  exit 0
fi

svar() { # $1 = ask eller deny, $2 = begrunnelse
  jq -n --arg d "$1" --arg r "$2" '{permissionDecision: $d, permissionDecisionReason: $r,
    hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: $d, permissionDecisionReason: $r}}'
  exit 0
}

input=$(cat)
tool=$(jq -r '.toolName // .tool_name // empty' <<<"$input" 2>/dev/null)
args=$(jq -c '(.toolArgs // .tool_input // {}) | if type == "string" then (try fromjson catch {input: .}) else . end' \
  <<<"$input" 2>/dev/null)
[[ -z "$args" ]] && args='{}'
cmd=$(jq -r '[.. | objects | (.command?, .commandLine?, .cmd?, .script?) | strings] | first // empty' <<<"$args" 2>/dev/null)
stier=$(jq -r '.. | objects | (.path?, .file_path?, .filePath?) | strings' <<<"$args" 2>/dev/null)
patchstier=$(jq -r '.. | objects | (.input?, .patch?) | strings' <<<"$args" 2>/dev/null \
  | sed -nE 's/^\*\*\* (Add|Update|Delete) File: (.*)$/\2/p')

# Skallverktøy og verktøy som endrer filer. str_replace_editor har også et command-felt (view, create, str_replace),
# så navnet avgjør. Uten verktøynavn gjettes det ut fra argumentene, så et ukjent format heller spør for mye.
shopt -s nocasematch
skall=0; endrer=0
[[ "$tool" =~ ^(bash|powershell|shell|execute|terminal|run_in_terminal|runInTerminal)$ ]] && skall=1
[[ -z "$tool" && -n "$cmd" && -z "$stier$patchstier" ]] && skall=1
[[ "$tool" =~ (edit|create|write|replace|patch|insert) ]] && endrer=1
[[ -z "$tool" && -n "$stier$patchstier" ]] && endrer=1
[[ "$cmd" == "view" ]] && endrer=0
shopt -u nocasematch

# .env-filer har hemmeligheter (.env.dev-gcp brukes av docker-compose og kjøreoppsettet i IntelliJ). Gjelder også lesing.
while IFS= read -r sti; do
  case "${sti##*/}" in
    .env*) svar deny "$sti kan inneholde hemmeligheter og skal ikke leses eller endres av agenten." ;;
  esac
done <<<"$stier
$patchstier"

if [[ "$skall" == 1 ]]; then
  if grep -Eq "(^|[[:space:]/=<\"'])\.env" <<<"$cmd"; then
    svar deny "Kommandoen rører en .env-fil, som kan inneholde hemmeligheter."
  fi
  if grep -Eq 'git +push[^|;&]*(--force|(^|[[:space:]])-f([[:space:]]|$)|[[:space:]]\+[^[:space:]]|(^|[[:space:]:+/])main([[:space:]:]|$))' <<<"$cmd"; then
    svar deny "git push til main eller force-push er ikke tillatt fra agent. Lag en PR."
  fi
  if grep -Eq '(^|[;&| ])(sed +-i|perl +-pi|tee)\b.*(kontrakt/|flyway/)' <<<"$cmd"; then
    svar ask "Kommandoen skriver i kontrakt/ eller flyway/ via shell. Bekreft at dette er ønsket (publisert kontrakt / migrasjon)."
  fi
  exit 0
fi
[[ "$endrer" == 1 ]] || exit 0

while IFS= read -r sti; do
  [[ -z "$sti" ]] && continue
  case "/${sti#/}" in
    */kontrakt/src/main/*/Definisjon.kt|*/kontrakt/src/main/*/StegType.kt|*/kontrakt/src/main/*/AvklaringsbehovKode.kt|*/kontrakt/src/main/*/TypeBehandling.kt|*/kontrakt/src/main/*/Status.kt)
      svar ask "Endring i $sti: verdier her er lagret i DB og publisert kontrakt. Bare legg til nye verdier; eksisterende må ikke endres/fjernes. Bekreft." ;;
    */kontrakt/src/main/*)
      svar ask "Endring i kontrakt/ publiseres automatisk som ny bibliotekversjon og krever bump i aap-oppgave/aap-statistikk/aap-tilgang. Bekreft." ;;
    */resources/flyway/*)
      svar ask "Ny/endret migrasjon. Sjekk: neste ledige V1.<n>, aldri rediger en merget fil, rullende-trygg (nullable/DEFAULT, ingen DROP/RENAME sammen med kode). Bekreft." ;;
    */forretningsflyt/behandlingstyper/*)
      svar ask "Endring i flytdefinisjon ($sti). Stegrekkefølge og StegType-navn ligger i STEG_HISTORIKK for åpne behandlinger. Bekreft." ;;
    */src/main/kotlin/no/nav/aap/fordeler/regler/*|*/fordeler/FordelerRegelService.kt)
      svar ask "Endring i fordelingen Kelvin/Arena ($sti). Regelnavn lagres i REGEL_EVALUERING, og en ny aktiv regel blir et krav for Kelvin. Bekreft." ;;
    */unleash/FeatureToggle.kt)
      svar ask "Toggle-navnet er Unleash-nøkkelen og må finnes i Unleash; oppdater også FakeUnleash.kt. Bekreft." ;;
    */.nais/*.yaml)
      svar ask "Endring i NAIS-manifest ($sti): gjelder prod/dev ved neste deploy (outbound, miljøvariabler, ressurser). Bekreft." ;;
    */app/src/main/resources/logback.xml)
      svar ask "logback.xml maskerer fødselsnummer i prod-logg og styrer auditloggen. Bekreft." ;;
  esac
done <<<"$stier
$patchstier"
exit 0
