#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=generated/handoff/catalog
mkdir -p "$out"
cli=build/install/mtg-assistant/bin/mtg-assistant
for who in judith krenko; do
 commander='Judith, the Scourge Diva'; [ "$who" != krenko ] || commander='Krenko, Mob Boss'
 for letter in a b c; do
  case "$letter" in
   a) objective=own-nontoken-creature-deaths; review=src/test/resources/regression/deaths-expanded.json ;;
   b) objective=reanimate-own-creatures; review=src/test/resources/regression/reanimation.json ;;
   c) objective=create-creature-tokens; review=src/test/resources/regression/tokens.json ;;
  esac
  "$cli" discover --commander "$commander" --objective "$objective" --json --output "$out/$who-$letter.json"
  "$cli" audit benchmark --snapshot "$out/$who-$letter.json" --benchmark "$review" --json --output "$out/$who-$letter-benchmark.json"
 done
done
"$cli" audit coverage --snapshot "$out/judith-c.json" --json --output "$out/coverage.json"
"$cli" audit inconsistencies --snapshot "$out/judith-c.json" --benchmark src/test/resources/regression/tokens.json --json --output "$out/examination.json"
"$cli" audit diff --before "$out/judith-a.json" --after "$out/judith-a.json" --json --output "$out/diff-identical.json"
"${PYTHON:-python3}" scripts/compare-baseline.py "$out" src/test/resources/regression/catalog-baseline.json
