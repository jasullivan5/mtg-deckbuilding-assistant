#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
case "${1:-fast}" in
 fast) ./dev test --tests dev.mtg.assistant.ArgentumIntegrationTest --tests '*FactsTest' --tests dev.mtg.assistant.CommanderIntegrationTest --tests dev.mtg.assistant.ExtensionAdmissionTest --tests dev.mtg.assistant.ExtensionStructureTest --tests dev.mtg.assistant.OviyaPashiriSageLifecrafterScenarioTest --tests dev.mtg.assistant.JudithTheScourgeDivaScenarioTest --tests '*OracleEnrichment*Test' --tests '*EligibilityCorrections*Test' --tests dev.mtg.assistant.CatalogCoverageTest --tests dev.mtg.assistant.Phase27TranslationIntegrationTest --tests dev.mtg.assistant.Phase27TranslationScenarioTest; ./dev installDist; cli/build/install/mtg-assistant/bin/mtg-assistant card 'Judith, the Scourge Diva' ;;
 full) ./dev test --rerun; ./dev installDist; bash scripts/check-catalog.sh ;;
 catalog) bash scripts/check-catalog.sh ;;
 *) echo 'Usage: scripts/verify.sh {fast|full|catalog}' >&2; exit 2 ;;
esac
